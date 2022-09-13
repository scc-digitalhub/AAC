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
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.service.AttributeProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class AttributeProviderManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private AttributeProviderAuthorityService attributeProviderAuthorityService;

    @Autowired
    private RealmService realmService;

    /*
     * Attribute Providers
     */

    public Collection<ConfigurableAttributeProvider> listProviders(String realm) throws NoSuchRealmException {
        logger.debug("list providers for realm {}", StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            return attributeProviderService.listProviders(realm);
        }

        Realm re = realmService.getRealm(realm);
        return attributeProviderService.listProviders(re.getSlug());
    }

    public ConfigurableAttributeProvider findProvider(String realm, String providerId) {
        logger.debug("find provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        ConfigurableAttributeProvider ap = attributeProviderService.findProvider(providerId);

        if (ap != null && !realm.equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return ap;
    }

    public ConfigurableAttributeProvider getProvider(String realm, String providerId)
            throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug("get provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        Realm re = realmService.getRealm(realm);
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);
        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // deprecated, let controllers/managers ask for status where needed
        // this does not pertain to configuration
        boolean isActive = attributeProviderAuthorityService.getAuthority(ap.getAuthority())
                .hasProvider(ap.getProvider());
        ap.setRegistered(isActive);

        return ap;
    }

    public ConfigurableAttributeProvider addProvider(String realm, ConfigurableAttributeProvider provider)
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
        return attributeProviderService.addProvider(re.getSlug(), provider);
    }

    public ConfigurableAttributeProvider updateProvider(String realm, String providerId,
            ConfigurableAttributeProvider provider)
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
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return attributeProviderService.updateProvider(providerId, provider);
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
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support delete for active providers
        boolean isActive = attributeProviderAuthorityService.getAuthority(ap.getAuthority())
                .hasProvider(ap.getProvider());

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be deleted");
        }

        attributeProviderService.deleteProvider(providerId);
    }

    public ConfigurableAttributeProvider registerProvider(String realm, String providerId)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("register provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support update for active providers
        boolean isActive = attributeProviderAuthorityService.getAuthority(ap.getAuthority())
                .hasProvider(ap.getProvider());
        if (isActive) {
            // make a quick unload
            attributeProviderAuthorityService.getAuthority(ap.getAuthority()).unregisterProvider(ap.getProvider());
            isActive = attributeProviderAuthorityService.getAuthority(ap.getAuthority())
                    .hasProvider(ap.getProvider());
        }

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be registered again");
        }

        // check if already enabled in config, or update
        if (!ap.isEnabled()) {
            ap.setEnabled(true);
            ap = attributeProviderService.updateProvider(providerId, ap);
        }

        AttributeProvider<?, ?> idp = attributeProviderAuthorityService.getAuthority(ap.getAuthority())
                .registerProvider(ap);
        isActive = idp != null;
        ap.setRegistered(isActive);

        return ap;
    }

    public ConfigurableAttributeProvider unregisterProvider(String realm, String providerId)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("unregister provider {} for realm {}", StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if already disabled in config, or update
        if (ap.isEnabled()) {
            ap.setEnabled(false);
            ap = attributeProviderService.updateProvider(providerId, ap);
        }

        // check if active
        boolean isActive = attributeProviderAuthorityService.getAuthority(ap.getAuthority())
                .hasProvider(ap.getProvider());

        if (isActive) {
            attributeProviderAuthorityService.getAuthority(ap.getAuthority()).unregisterProvider(ap.getProvider());
            isActive = false;
            ap.setRegistered(isActive);
        }

        return ap;
    }

    /*
     * Compatibility
     * 
     * Support checking registration status
     */
    public boolean isProviderRegistered(String realm, ConfigurableAttributeProvider provider) throws SystemException {
        try {
            return attributeProviderAuthorityService.getAuthority(provider.getAuthority())
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
        return attributeProviderService.getConfigurableProperties(authority);
    }

    public JsonSchema getConfigurationSchema(String realm, String authority)
            throws NoSuchAuthorityException {
        return attributeProviderService.getConfigurationSchema(authority);
    }

}
