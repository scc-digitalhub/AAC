package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;

@Service
public class ProviderManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private IdentityProviderService identityProviderService;

    private AuthorityManager authorityManager;

    private RealmService realmService;

//    // keep a local map for global providers since these are not in db
//    // key is providerId
//    private Map<String, IdentityProvider> systemIdps;
//    private Map<String, IdentityService> systemIdss;
////    private Map<String, AttributeProvider> globalAttrps;

    public ProviderManager(
            AuthorityManager authorityManager,
            IdentityProviderService identityProviderService, RealmService realmService) {
        Assert.notNull(authorityManager, "authority manager is mandatory");
        Assert.notNull(identityProviderService, "identity provider service is mandatory");
        Assert.notNull(realmService, "realm service is mandatory");

        this.authorityManager = authorityManager;
        this.identityProviderService = identityProviderService;
        this.realmService = realmService;

//        // now load all realm providers from storage
//        // we iterate by authority to load consistently
//        // disabled, move to bootstrap thread not on init
//        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
//            List<ConfigurableProvider> storeProviders = listProvidersByAuthority(ia.getAuthorityId());
//            for (ConfigurableProvider provider : storeProviders) {
//                // check match
//                if (!TYPE_IDENTITY.equals(provider.getType())) {
//                    continue;
//                }
//
//                // try register
//                if (provider.isEnabled()) {
//                    try {
//                        ia.registerIdentityProvider(provider);
//                    } catch (Exception e) {
//                        logger.error("error registering provider " + provider.getProvider() + " for realm "
//                                + provider.getRealm() + ": " + e.getMessage());
//                    }
//                }
//            }
//        }

    }

    /*
     * Public API: realm providers only.
     * 
     * For global providers we enable only a subset of features (RO)
     * 
     * TODO add permissions
     */
    public Collection<ConfigurableProvider> listProviders(String realm) throws NoSuchRealmException {
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new SystemException("global providers are immutable");
        }

        Realm re = realmService.getRealm(realm);
        List<ConfigurableProvider> providers = new ArrayList<>();
        // identity providers
        Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(re.getSlug());
        providers.addAll(idps);
        // attribute providers
        return providers;
    }

    public Collection<? extends ConfigurableProvider> listProviders(String realm, String type)
            throws NoSuchRealmException {
        if (TYPE_IDENTITY.equals(type)) {
            return listIdentityProviders(realm);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider findProvider(String realm, String type, String providerId) {
        if (TYPE_IDENTITY.equals(type)) {
            return findIdentityProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider getProvider(String realm, String type, String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        if (TYPE_IDENTITY.equals(type)) {
            return getIdentityProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider addProvider(String realm,
            ConfigurableProvider provider) throws RegistrationException, SystemException, NoSuchRealmException {

        if (provider instanceof ConfigurableIdentityProvider) {
            return addIdentityProvider(realm, (ConfigurableIdentityProvider) provider);
        }

        throw new IllegalArgumentException("invalid provider");
    }

    public ConfigurableProvider updateProvider(String realm,
            String providerId, ConfigurableProvider provider)
            throws NoSuchProviderException, NoSuchRealmException {
        if (provider instanceof ConfigurableIdentityProvider) {
            return updateIdentityProvider(realm, providerId, (ConfigurableIdentityProvider) provider);
        }

        throw new IllegalArgumentException("invalid provider");
    }

    public void deleteProvider(String realm, String type, String providerId)
            throws SystemException, NoSuchProviderException, NoSuchRealmException {
        if (TYPE_IDENTITY.equals(type)) {
            deleteIdentityProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider registerProvider(
            String realm, String type,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {
        if (TYPE_IDENTITY.equals(type)) {
            registerIdentityProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    public ConfigurableProvider unregisterProvider(
            String realm, String type,
            String providerId) throws SystemException, NoSuchRealmException, NoSuchProviderException {
        if (TYPE_IDENTITY.equals(type)) {
            unregisterIdentityProvider(realm, providerId);
        }

        throw new IllegalArgumentException("invalid type");
    }

    /*
     * Identity Providers
     */

    public Collection<ConfigurableIdentityProvider> listIdentityProviders(String realm) throws NoSuchRealmException {
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new SystemException("global providers are immutable");
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
            ConfigurableIdentityProvider provider) throws RegistrationException, SystemException, NoSuchRealmException {

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
            throw new IllegalArgumentException("active providers can not be registered again");
        }

        // check if already enabled in config, or update
        if (!ip.isEnabled()) {
            ip.setEnabled(true);
            ip = identityProviderService.updateProvider(providerId,
                    ip);
        }

        IdentityProvider idp = authorityManager.registerIdentityProvider(ip);
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
     * Compatibility
     * 
     * Support checking registration status
     */
    public boolean isProviderRegistered(ConfigurableProvider provider) throws SystemException {
        if (TYPE_IDENTITY.equals(provider.getType())) {
            return authorityManager.isIdentityProviderRegistered(provider.getAuthority(), provider.getProvider());
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

    public Collection<ConfigurableProvider> listProviderConfigurationTemplates(String type) {
        if (TYPE_IDENTITY.equals(type)) {
            // we support only idp templates
            List<ConfigurableProvider> templates = new ArrayList<>();
            for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
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

    public JsonSchema getConfigurationSchema(String type, String authority) {
        if (TYPE_IDENTITY.equals(type)) {
            return identityProviderService.getConfigurationSchema(authority);
        }

        throw new IllegalArgumentException("invalid provider type");
    }

    public static final String TYPE_IDENTITY = SystemKeys.RESOURCE_IDENTITY;
    public static final String TYPE_ATTRIBUTES = SystemKeys.RESOURCE_ATTRIBUTES;

    /*
     * Helpers
     */

}
