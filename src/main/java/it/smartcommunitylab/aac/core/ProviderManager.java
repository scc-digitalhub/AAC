package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class ProviderManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private RealmService realmService;

    // TODO cleanup usage of authorityManager here
    // either move to authorityService or link controllers directly
    @Autowired
    private AuthorityManager authorityManager;

    /*
     * Public API: realm providers only.
     * 
     * For global providers we enable only a subset of features (RO)
     * 
     * TODO add permissions
     */
    public Collection<ConfigurableProvider> listProviders(String realm) throws NoSuchRealmException {
        List<ConfigurableProvider> providers = new ArrayList<>();

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // idps only
            Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(realm);
            providers.addAll(idps);
            return providers;
        }

        Realm re = realmService.getRealm(realm);

        // identity providers
        Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(re.getSlug());
        providers.addAll(idps);
        // attribute providers
        Collection<ConfigurableAttributeProvider> aps = attributeProviderService.listProviders(re.getSlug());
        providers.addAll(aps);

        return providers;
    }

    public Collection<? extends ConfigurableProvider> listProviders(String realm, String type)
            throws NoSuchRealmException {
        if (TYPE_IDENTITY.equals(type)) {
            return listIdentityProviders(realm);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return listAttributeProviders(realm);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider findProvider(String realm, String type, String providerId) {
        if (TYPE_IDENTITY.equals(type)) {
            return findIdentityProvider(realm, providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return findAttributeProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider getProvider(String realm, String type, String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        if (TYPE_IDENTITY.equals(type)) {
            return getIdentityProvider(realm, providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return getAttributeProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider addProvider(String realm,
            ConfigurableProvider provider)
            throws RegistrationException, SystemException, NoSuchRealmException, NoSuchProviderException {

        if (provider instanceof ConfigurableIdentityProvider) {
            return addIdentityProvider(realm, (ConfigurableIdentityProvider) provider);
        } else if (provider instanceof ConfigurableAttributeProvider) {
            return addAttributeProvider(realm, (ConfigurableAttributeProvider) provider);
        }

        throw new IllegalArgumentException("invalid provider");
    }

    public ConfigurableProvider updateProvider(String realm,
            String providerId, ConfigurableProvider provider)
            throws NoSuchProviderException, NoSuchRealmException {
        if (provider instanceof ConfigurableIdentityProvider) {
            return updateIdentityProvider(realm, providerId, (ConfigurableIdentityProvider) provider);
        } else if (provider instanceof ConfigurableAttributeProvider) {
            return updateAttributeProvider(realm, providerId, (ConfigurableAttributeProvider) provider);
        }

        throw new IllegalArgumentException("invalid provider");
    }

    public void deleteProvider(String realm, String type, String providerId)
            throws SystemException, NoSuchProviderException, NoSuchRealmException {
        if (TYPE_IDENTITY.equals(type)) {
            deleteIdentityProvider(realm, providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            deleteAttributeProvider(realm, providerId);
        } else {
            throw new IllegalArgumentException("invalid type");
        }
    }

    public ConfigurableProvider registerProvider(
            String realm, String type,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {
        if (TYPE_IDENTITY.equals(type)) {
            return registerIdentityProvider(realm, providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return registerAttributeProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider unregisterProvider(
            String realm, String type,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {
        if (TYPE_IDENTITY.equals(type)) {
            return unregisterIdentityProvider(realm, providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return unregisterAttributeProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    /*
     * Identity Providers
     */

    public Collection<ConfigurableIdentityProvider> listIdentityProviders(String realm) throws NoSuchRealmException {
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            return identityProviderService.listProviders(realm);
        }

        Realm re = realmService.getRealm(realm);
        return identityProviderService.listProviders(re.getSlug());
    }

    public ConfigurableIdentityProvider findIdentityProvider(String realm, String providerId) {
        ConfigurableIdentityProvider ip = identityProviderService.findProvider(providerId);

        if (ip != null && !realm.equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return ip;
    }

    public ConfigurableIdentityProvider getIdentityProvider(String realm, String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        Realm re = realmService.getRealm(realm);
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);
        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // deprecated, let controllers/managers ask for status where needed
        // this does not pertain to configuration
//        boolean isActive = isProviderRegistered(pe.getType(), pe.getAuthority(), pe.getProviderId());

        return ip;
    }

    public ConfigurableIdentityProvider addIdentityProvider(String realm,
            ConfigurableIdentityProvider provider)
            throws RegistrationException, SystemException, NoSuchRealmException, NoSuchProviderException {

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        return identityProviderService.addProvider(re.getSlug(), provider);

    }

    public ConfigurableIdentityProvider updateIdentityProvider(String realm,
            String providerId, ConfigurableIdentityProvider provider)
            throws NoSuchProviderException, NoSuchRealmException {
        Realm re = realmService.getRealm(realm);
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return identityProviderService.updateProvider(providerId, provider);

    }

    public void deleteIdentityProvider(String realm, String providerId)
            throws SystemException, NoSuchProviderException, NoSuchRealmException {
        Realm re = realmService.getRealm(realm);
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support update for active providers
        boolean isActive = authorityManager.isIdentityProviderRegistered(ip.getAuthority(), ip.getProvider());

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be deleted");
        }

        identityProviderService.deleteProvider(providerId);
    }

    //
    public ConfigurableIdentityProvider registerIdentityProvider(
            String realm,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        ConfigurableIdentityProvider ip = identityProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ip.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support update for active providers
        boolean isActive = authorityManager.isIdentityProviderRegistered(ip.getAuthority(), ip.getProvider());
        if (isActive) {
            // make a quick unload
            authorityManager.unregisterIdentityProvider(ip);
            isActive = authorityManager.isIdentityProviderRegistered(ip.getAuthority(), ip.getProvider());
        }

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be registered again");
        }

        // check if already enabled in config, or update
        if (!ip.isEnabled()) {
            ip.setEnabled(true);
            ip = identityProviderService.updateProvider(providerId,
                    ip);
        }

        IdentityProvider<? extends UserIdentity> idp = authorityManager
                .registerIdentityProvider(ip);
        isActive = idp != null;

        // TODO fetch registered status?
        ip.setRegistered(isActive);

        return ip;

    }

    public ConfigurableIdentityProvider unregisterIdentityProvider(
            String realm,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {

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
        boolean isActive = authorityManager.isIdentityProviderRegistered(ip.getAuthority(), ip.getProvider());

        if (isActive) {

            authorityManager.unregisterIdentityProvider(ip);
            isActive = false;

            // TODO fetch registered status?
            ip.setRegistered(isActive);
        }

        return ip;
    }

    /*
     * Attribute Providers
     */

    public Collection<ConfigurableAttributeProvider> listAttributeProviders(String realm) throws NoSuchRealmException {
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            return attributeProviderService.listProviders(realm);
        }

        Realm re = realmService.getRealm(realm);
        return attributeProviderService.listProviders(re.getSlug());
    }

    public ConfigurableAttributeProvider findAttributeProvider(String realm, String providerId) {
        ConfigurableAttributeProvider ap = attributeProviderService.findProvider(providerId);

        if (ap != null && !realm.equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return ap;
    }

    public ConfigurableAttributeProvider getAttributeProvider(String realm, String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        Realm re = realmService.getRealm(realm);
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);
        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // deprecated, let controllers/managers ask for status where needed
        // this does not pertain to configuration
//        boolean isActive = isProviderRegistered(pe.getType(), pe.getAuthority(), pe.getProviderId());

        return ap;
    }

    public ConfigurableAttributeProvider addAttributeProvider(String realm,
            ConfigurableAttributeProvider provider)
            throws RegistrationException, SystemException, NoSuchRealmException {

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        return attributeProviderService.addProvider(re.getSlug(), provider);

    }

    public ConfigurableAttributeProvider updateAttributeProvider(String realm,
            String providerId, ConfigurableAttributeProvider provider)
            throws NoSuchProviderException, NoSuchRealmException {
        Realm re = realmService.getRealm(realm);
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        return attributeProviderService.updateProvider(providerId, provider);

    }

    public void deleteAttributeProvider(String realm, String providerId)
            throws SystemException, NoSuchProviderException, NoSuchRealmException {
        Realm re = realmService.getRealm(realm);
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support update for active providers
        boolean isActive = authorityManager.isAttributeProviderRegistered(ap.getAuthority(), ap.getProvider());

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be deleted");
        }

        attributeProviderService.deleteProvider(providerId);
    }

    //
    public ConfigurableAttributeProvider registerAttributeProvider(
            String realm,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {

        Realm re = realmService.getRealm(realm);
        // fetch, only persisted configurations can be registered
        ConfigurableAttributeProvider ap = attributeProviderService.getProvider(providerId);

        if (!re.getSlug().equals(ap.getRealm())) {
            throw new IllegalArgumentException("realm does not match provider");
        }

        // check if active, we don't support update for active providers
        boolean isActive = authorityManager.isAttributeProviderRegistered(ap.getAuthority(), ap.getProvider());

        if (isActive) {
            throw new IllegalArgumentException("active providers can not be registered again");
        }

        // check if already enabled in config, or update
        if (!ap.isEnabled()) {
            ap.setEnabled(true);
            ap = attributeProviderService.updateProvider(providerId, ap);
        }

        AttributeProvider idp = authorityManager.registerAttributeProvider(ap);
        isActive = idp != null;

        // TODO fetch registered status?
        ap.setRegistered(isActive);

        return ap;

    }

    public ConfigurableAttributeProvider unregisterAttributeProvider(
            String realm,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {

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
        boolean isActive = authorityManager.isAttributeProviderRegistered(ap.getAuthority(), ap.getProvider());

        if (isActive) {

            authorityManager.unregisterAttributeProvider(ap);
            isActive = false;

            // TODO fetch registered status?
            ap.setRegistered(isActive);
        }

        return ap;
    }

    /*
     * Compatibility
     * 
     * Support checking registration status
     */
    public boolean isProviderRegistered(String realm, ConfigurableProvider provider) throws SystemException {
        if (TYPE_IDENTITY.equals(provider.getType())) {
            return authorityManager.isIdentityProviderRegistered(provider.getAuthority(), provider.getProvider());
        }
        if (TYPE_ATTRIBUTES.equals(provider.getType())) {
            return authorityManager.isAttributeProviderRegistered(provider.getAuthority(), provider.getProvider());
        }
        throw new IllegalArgumentException("invalid type");
    }

    /*
     * Internal API
     * 
     * methods for handling providers either private or exposed for internal (core)
     * usage
     */

    /*
     * Persist configuration
     * 
     * only for realm providers, global are configured only via app.properties to
     * avoid mangling with administrative sessions
     */

//    private List<ConfigurableProvider> listProvidersByAuthority(String authority) {
//        List<ProviderEntity> providers = providerService.listProvidersByAuthority(authority);
//        return providers.stream().map(p -> fromEntity(p)).collect(Collectors.toList());
//    }
//
//    private ConfigurableProvider getProvider(String providerId) throws NoSuchProviderException {
//        IdentityProviderEntity pe = providerService.getProvider(providerId);
//
//        return fromEntity(pe);
//
//    }

    /*
     * Configuration templates TODO drop templates and rework as custom authorities
     * extending a base
     */

    private Collection<ConfigurableProvider> listProviderConfigurationTemplates(String type) {
        if (TYPE_IDENTITY.equals(type)) {
            // we support only idp templates
            List<ConfigurableProvider> templates = new ArrayList<>();
            for (IdentityAuthority ia : authorityManager
                    .listIdentityAuthorities()) {
                templates.addAll(ia.getConfigurableProviderTemplates());
            }

            return templates;

        }
        return Collections.emptyList();

    }

    public Collection<ConfigurableProvider> listProviderConfigurationTemplates(String realm, String type) {
        Collection<ConfigurableProvider> templates = listProviderConfigurationTemplates(type);
        // keep only those matching realm or with realm == null
        return templates.stream().filter(t -> (t.getRealm() == null
                || realm.equals(t.getRealm())
                || SystemKeys.REALM_GLOBAL.equals(t.getRealm()))).collect(Collectors.toList());

    }

    /*
     * Configuration schemas
     */

    public ConfigurableProperties getConfigurableProperties(String realm, String type, String authority) {
        if (TYPE_IDENTITY.equals(type)) {
            return identityProviderService.getConfigurableProperties(authority);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            // TODO
            return null;
        }
        throw new IllegalArgumentException("invalid provider type");
    }

    public JsonSchema getConfigurationSchema(String realm, String type, String authority) {
        if (TYPE_IDENTITY.equals(type)) {
            return identityProviderService.getConfigurationSchema(authority);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return attributeProviderService.getConfigurationSchema(authority);
        }
        throw new IllegalArgumentException("invalid provider type");
    }

    public static final String TYPE_IDENTITY = SystemKeys.RESOURCE_IDENTITY;
    public static final String TYPE_ATTRIBUTES = SystemKeys.RESOURCE_ATTRIBUTES;

    /*
     * Helpers
     */

}
