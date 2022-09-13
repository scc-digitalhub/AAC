package it.smartcommunitylab.aac.core;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class IdentityProviderManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private RealmService realmService;

    /*
     * Identity Providers
     */

    public Collection<ConfigurableIdentityProvider> listProviders(String realm) throws NoSuchRealmException {
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            return identityProviderService.listProviders(realm);
        }

        Realm re = realmService.getRealm(realm);
        return identityProviderService.listProviders(re.getSlug());
    }

    public ConfigurableIdentityProvider findProvider(String realm, String providerId) {
        logger.debug("find provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider ip = identityProviderService.findProvider(providerId);

        if (ip != null && !realm.equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return ip;
    }

    public ConfigurableIdentityProvider getProvider(String realm, String providerId)
            throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug("get provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        Realm re = realmService.getRealm(realm);
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);
        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // deprecated, let controllers/managers ask for status where needed
        // this does not pertain to configuration
        boolean isActive = identityProviderAuthorityService.getAuthority(ip.getAuthority())
                .hasProvider(ip.getProvider());
        ip.setRegistered(isActive);

        return ip;
    }

    public ConfigurableIdentityProvider addProvider(String realm, ConfigurableIdentityProvider provider)
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
        return identityProviderService.addProvider(re.getSlug(), provider);
    }

    public ConfigurableIdentityProvider updateProvider(String realm, String providerId,
            ConfigurableIdentityProvider provider)
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
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return identityProviderService.updateProvider(providerId, provider);
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
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support delete for active providers
        boolean isActive = identityProviderAuthorityService.getAuthority(ip.getAuthority())
                .hasProvider(ip.getProvider());

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be deleted");
        }

        identityProviderService.deleteProvider(providerId);
    }

    public ConfigurableIdentityProvider registerProvider(String realm, String providerId)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("register provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active
        boolean isActive = identityProviderAuthorityService.getAuthority(ip.getAuthority())
                .hasProvider(ip.getProvider());
        if (isActive) {
            // make a quick unload
            identityProviderAuthorityService.getAuthority(ip.getAuthority()).unregisterProvider(ip.getProvider());
            isActive = identityProviderAuthorityService.getAuthority(ip.getAuthority())
                    .hasProvider(ip.getProvider());
        }

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be registered again");
        }

        // check if already enabled in config, or update
        if (!ip.isEnabled()) {
            ip.setEnabled(true);
            ip = identityProviderService.updateProvider(providerId, ip);
        }

        IdentityProvider<?, ?, ?> idp = identityProviderAuthorityService.getAuthority(ip.getAuthority())
                .registerProvider(ip);
        isActive = idp != null;
        ip.setRegistered(isActive);

        return ip;
    }

    public ConfigurableIdentityProvider unregisterProvider(String realm, String providerId)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("unregister provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if already disabled in config, or update
        if (ip.isEnabled()) {
            ip.setEnabled(false);
            ip = identityProviderService.updateProvider(providerId, ip);
        }

        // check if active
        boolean isActive = identityProviderAuthorityService.getAuthority(ip.getAuthority())
                .hasProvider(ip.getProvider());

        if (isActive) {
            identityProviderAuthorityService.getAuthority(ip.getAuthority()).unregisterProvider(ip.getProvider());
            isActive = false;
            ip.setRegistered(isActive);
        }

        return ip;
    }

    /*
     * Compatibility
     * 
     * Support checking registration status
     */
    public boolean isProviderRegistered(String realm, ConfigurableIdentityProvider provider) throws SystemException {
        try {
            return identityProviderAuthorityService.getAuthority(provider.getAuthority())
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
        return identityProviderService.getConfigurableProperties(authority);
    }

    public JsonSchema getConfigurationSchema(String realm, String authority)
            throws NoSuchAuthorityException {
        return identityProviderService.getConfigurationSchema(authority);
    }

}
