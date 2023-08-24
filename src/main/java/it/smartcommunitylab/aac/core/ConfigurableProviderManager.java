/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.service.AbstractConfigurableProviderService;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.service.RealmService;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public abstract class ConfigurableProviderManager<
    C extends ConfigurableProvider<? extends ConfigMap>,
    A extends ConfigurableProviderAuthority<?, ?, ?, ?, ? extends ConfigMap>
>
    implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AbstractConfigurableProviderService<C, ? extends ConfigMap> providerService;

    private RealmService realmService;

    protected ConfigurableProviderManager(AbstractConfigurableProviderService<C, ? extends ConfigMap> providerService) {
        Assert.notNull(providerService, "provider service is required");
        this.providerService = providerService;
    }

    @Autowired
    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(realmService, "realm service can not be null");
    }

    /*
     * Configurable Providers
     */
    public Collection<C> listProviders(String realm) throws NoSuchRealmException {
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            return providerService.listProviders(realm);
        }

        Realm re = realmService.getRealm(realm);
        return providerService.listProviders(re.getSlug());
    }

    public C findProvider(String realm, String providerId) {
        logger.debug(
            "find provider {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        C cp = providerService.findProvider(providerId);

        if (cp != null && !realm.equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return cp;
    }

    public C getProvider(String realm, String providerId)
        throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug(
            "get provider {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        Realm re = realmService.getRealm(realm);
        C cp = providerService.getProvider(providerId);
        if (!re.getSlug().equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // deprecated, let controllers/managers ask for status where needed
        // this does not pertain to configuration
        boolean isRegistered = providerService.isProviderRegistered(providerId);
        cp.setRegistered(isRegistered);

        return cp;
    }

    public C addProvider(String realm, C provider)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("add provider to realm {}", StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: {}", StringUtils.trimAllWhitespace(provider.toString()));
        }

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        return providerService.addProvider(re.getSlug(), provider);
    }

    public C updateProvider(String realm, String providerId, C provider)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "update provider {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: {}", StringUtils.trimAllWhitespace(provider.toString()));
        }

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        C cp = providerService.getProvider(providerId);

        if (!re.getSlug().equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return providerService.updateProvider(providerId, provider);
    }

    public void deleteProvider(String realm, String providerId)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "delete provider {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        C cp = providerService.getProvider(providerId);

        if (!re.getSlug().equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if registered, we don't support delete for active providers
        boolean isRegistered = providerService.isProviderRegistered(providerId);
        if (isRegistered) {
            throw new IllegalArgumentException("registered providers can not be deleted");
        }

        providerService.deleteProvider(providerId);
    }

    public C registerProvider(String realm, String providerId)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "register provider {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        C ip = providerService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if registered
        boolean isRegistered = providerService.isProviderRegistered(ip.getProvider());
        if (isRegistered) {
            // make a quick unload
            providerService.unregisterProvider(ip.getProvider());
            isRegistered = providerService.isProviderRegistered(ip.getProvider());
        }

        if (isRegistered) {
            throw new IllegalArgumentException("registered providers can not be registered again");
        }

        // check if already enabled in config, or update
        if (!ip.isEnabled()) {
            ip.setEnabled(true);
            ip = providerService.updateProvider(providerId, ip);
        }

        // register
        providerService.registerProvider(providerId);
        isRegistered = providerService.isProviderRegistered(ip.getProvider());
        ip.setRegistered(isRegistered);

        return ip;
    }

    public C unregisterProvider(String realm, String providerId)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "unregister provider {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        C cp = providerService.getProvider(providerId);

        if (!re.getSlug().equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if already disabled in config, or update
        if (cp.isEnabled()) {
            cp.setEnabled(false);
            cp = providerService.updateProvider(providerId, cp);
        }

        // check if registered
        boolean isRegistered = providerService.isProviderRegistered(providerId);

        if (isRegistered) {
            providerService.unregisterProvider(cp.getProvider());
            isRegistered = false;
            cp.setRegistered(isRegistered);
        }

        return cp;
    }

    /*
     * Compatibility
     *
     * Support checking registration status
     */
    public boolean isProviderRegistered(String realm, C provider) throws SystemException {
        if (provider == null) {
            return false;
        }

        try {
            return providerService.isProviderRegistered(provider.getProvider());
        } catch (NoSuchAuthorityException | NoSuchProviderException e) {
            return false;
        }
    }

    /*
     * Configuration schemas
     */

    // public ConfigurableProperties getConfigurableProperties(String realm, String authority)
    //     throws NoSuchAuthorityException {
    //     return providerService.getConfigurableProperties(authority);
    // }

    public JsonSchema getConfigurationSchema(String realm, String authority) throws NoSuchAuthorityException {
        return providerService.getConfigurationSchema(authority);
    }
}
