package it.smartcommunitylab.aac.core;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.AuthorityService;
import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;

public abstract class ConfigurableProviderManager<C extends ConfigurableProvider, A extends ProviderAuthority<?, ?, C, ?, ?>> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigurableProviderService<C, ? extends ProviderEntity> providerService;

    private final AuthorityService<A> providerAuthorityService;

    private RealmService realmService;

    public ConfigurableProviderManager(ConfigurableProviderService<C, ? extends ProviderEntity> providerService,
            AuthorityService<A> providerAuthorityService) {
        Assert.notNull(providerService, "provider service is required");
        Assert.notNull(providerAuthorityService, "authority service is required");

        this.providerService = providerService;
        this.providerAuthorityService = providerAuthorityService;
    }

    @Autowired
    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
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
        logger.debug("find provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        C cp = providerService.findProvider(providerId);

        if (cp != null && !realm.equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return cp;
    }

    public C getProvider(String realm, String providerId)
            throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug("get provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        Realm re = realmService.getRealm(realm);
        C cp = providerService.getProvider(providerId);
        if (!re.getSlug().equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // deprecated, let controllers/managers ask for status where needed
        // this does not pertain to configuration
        boolean isActive = providerAuthorityService.getAuthority(cp.getAuthority())
                .hasProvider(cp.getProvider());
        cp.setRegistered(isActive);

        return cp;
    }

    public C addProvider(String realm, C provider)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("add provider to realm {}", StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: " + StringUtils.trimAllWhitespace(provider.toString()));
        }

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        return providerService.addProvider(re.getSlug(), provider);
    }

    public C updateProvider(String realm, String providerId,
            C provider)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("update provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: " + StringUtils.trimAllWhitespace(provider.toString()));
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
        logger.debug("delete provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        C cp = providerService.getProvider(providerId);

        if (!re.getSlug().equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support delete for active providers
        boolean isActive = providerAuthorityService.getAuthority(cp.getAuthority())
                .hasProvider(cp.getProvider());

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be deleted");
        }

        providerService.deleteProvider(providerId);
    }

    public C registerProvider(String realm, String providerId)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("register provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

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

        // check if active
        boolean isActive = providerAuthorityService.getAuthority(ip.getAuthority())
                .hasProvider(ip.getProvider());
        if (isActive) {
            // make a quick unload
            providerAuthorityService.getAuthority(ip.getAuthority()).unregisterProvider(ip.getProvider());
            isActive = providerAuthorityService.getAuthority(ip.getAuthority())
                    .hasProvider(ip.getProvider());
        }

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be registered again");
        }

        // check if already enabled in config, or update
        if (!ip.isEnabled()) {
            ip.setEnabled(true);
            ip = providerService.updateProvider(providerId, ip);
        }

        ConfigurableResourceProvider<?, ?, ?, ?> idp = providerAuthorityService.getAuthority(ip.getAuthority())
                .registerProvider(ip);
        isActive = idp != null;
        ip.setRegistered(isActive);

        return ip;
    }

    public C unregisterProvider(String realm, String providerId)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("unregister provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

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

        // check if active
        boolean isActive = providerAuthorityService.getAuthority(cp.getAuthority())
                .hasProvider(cp.getProvider());

        if (isActive) {
            providerAuthorityService.getAuthority(cp.getAuthority()).unregisterProvider(cp.getProvider());
            isActive = false;
            cp.setRegistered(isActive);
        }

        return cp;
    }

    /*
     * Compatibility
     * 
     * Support checking registration status
     */
    public boolean isProviderRegistered(String realm, C provider) throws SystemException {
        try {
            return providerAuthorityService.getAuthority(provider.getAuthority())
                    .hasProvider(provider.getProvider());
        } catch (NoSuchAuthorityException e) {
            return false;
        }
    }

    /*
     * Configuration schemas
     */

    public ConfigurableProperties getConfigurableProperties(String realm, String authority)
            throws NoSuchAuthorityException {
        return providerService.getConfigurableProperties(authority);
    }

    public JsonSchema getConfigurationSchema(String realm, String authority)
            throws NoSuchAuthorityException {
        return providerService.getConfigurationSchema(authority);
    }

}
