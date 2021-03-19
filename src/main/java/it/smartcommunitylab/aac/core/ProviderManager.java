package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.config.ProvidersProperties;
import it.smartcommunitylab.aac.config.ProvidersProperties.ProviderConfiguration;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.ProviderService;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;

@Service
public class ProviderManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ProviderService providerService;

    private SessionManager sessionManager;

    private AuthorityManager authorityManager;

    // keep a local map for global providers since these are not in db
    // key is providerId
    private Map<String, IdentityProvider> globalIdps;
    private Map<String, AttributeProvider> globalAttrps;

    public ProviderManager(
            AuthorityManager authorityManager,
            SessionManager sessionManager,
            ProviderService providerService,
            ProvidersProperties providers) {
        Assert.notNull(authorityManager, "authority manager is mandatory");
        Assert.notNull(sessionManager, "session manager is mandatory");
        Assert.notNull(providerService, "provider service is mandatory");

        this.authorityManager = authorityManager;
        this.sessionManager = sessionManager;
        this.providerService = providerService;

        this.globalIdps = new HashMap<>();
        this.globalAttrps = new HashMap<>();

        // create global idps
        // these users access administative contexts, they will have realm=""
        // we expect no client/services in global realm!

        // always register the internal provider for the superuser account
        IdentityAuthority internal = authorityManager.getIdentityAuthority(SystemKeys.AUTHORITY_INTERNAL);

        ConfigurableProvider internalIdpConfig = new ConfigurableProvider(
                SystemKeys.AUTHORITY_INTERNAL, SystemKeys.AUTHORITY_INTERNAL,
                SystemKeys.REALM_GLOBAL);
        internalIdpConfig.setType(SystemKeys.RESOURCE_IDENTITY);
        logger.debug("register internal idp in global realm");
        IdentityProvider internalIdp = internal.registerIdentityProvider(internalIdpConfig);
        globalIdps.put(internalIdp.getProvider(), internalIdp);

        // process additional from config
        if (providers != null) {
            // identity providers
            for (ProviderConfiguration providerConfig : providers.getIdentity()) {
                try {
                    // check match
                    if (!SystemKeys.RESOURCE_IDENTITY.equals(providerConfig.getType())) {
                        continue;
                    }

                    // we handle only global providers, add others via bootstrap
                    if (SystemKeys.REALM_GLOBAL.equals(providerConfig.getRealm())
                            || providerConfig.getRealm() == null) {
                        logger.debug(
                                "register provider for " + providerConfig.getType() + " in global realm: "
                                        + providerConfig.toString());

                        // translate config
                        ConfigurableProvider provider = new ConfigurableProvider(providerConfig.getAuthority(),
                                providerConfig.getProvider(), SystemKeys.REALM_GLOBAL);
                        provider.setType(providerConfig.getType());
                        provider.setName(providerConfig.getName());
                        provider.setEnabled(true);
                        for (Map.Entry<String, String> entry : providerConfig.getConfiguration().entrySet()) {
                            provider.setConfigurationProperty(entry.getKey(), entry.getValue());
                        }

                        // by default global providers persist account + attributes
                        String persistenceLevel = SystemKeys.PERSISTENCE_LEVEL_REPOSITORY;
                        if (StringUtils.hasText(providerConfig.getPersistence())) {
                            // set persistence level
                            persistenceLevel = providerConfig.getPersistence();
                        }
                        provider.setPersistence(persistenceLevel);

                        // register
                        IdentityProvider idp = registerIdentityProvider(provider);
                        globalIdps.put(idp.getProvider(), idp);
                    }
                } catch (SystemException | IllegalArgumentException ex) {
                    logger.error("error registering provider :" + ex.getMessage(), ex);
                }
            }

            // attribute providers
            for (ProviderConfiguration providerConfig : providers.getAttributes()) {
                try {
                    // check match
                    if (!SystemKeys.RESOURCE_ATTRIBUTES.equals(providerConfig.getType())) {
                        continue;
                    }

                    // TODO
                } catch (SystemException | IllegalArgumentException ex) {
                    logger.error("error registering provider :" + ex.getMessage(), ex);
                }
            }
        }

        // now load all realm providers from storage
        // we iterate by authority to load consistently
        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
            List<ConfigurableProvider> storeProviders = listProvidersByAuthority(ia.getAuthorityId());
            for (ConfigurableProvider provider : storeProviders) {
                // check match
                if (!SystemKeys.RESOURCE_IDENTITY.equals(provider.getType())) {
                    continue;
                }

                // register
                ia.registerIdentityProvider(provider);
            }
        }

    }

    /*
     * Enable/disable providers with authorities
     */
    private IdentityProvider registerIdentityProvider(ConfigurableProvider provider) throws SystemException {
        // we support only idp now
        if (!SystemKeys.RESOURCE_IDENTITY.equals(provider.getType())) {
            throw new SystemException("unsupported provider type");
        }

        IdentityAuthority a = authorityManager.getIdentityAuthority(provider.getAuthority());
        return a.registerIdentityProvider(provider);

    }

    public void registerProvider(
            String providerId) throws NoSuchProviderException, SystemException {
        ConfigurableProvider p = getProvider(providerId);
        registerProvider(p);
    }

    public void registerProvider(
            ConfigurableProvider provider) throws SystemException {
        // we support only idp now
        if (SystemKeys.RESOURCE_IDENTITY.equals(provider.getType())) {
            IdentityAuthority a = authorityManager.getIdentityAuthority(provider.getAuthority());
            a.registerIdentityProvider(provider);
        } else if (SystemKeys.RESOURCE_ATTRIBUTES.equals(provider.getType())) {
            // TODO attribute providers
        } else {
            throw new SystemException("unsupported provider type");
        }

    }

    public void unregisterProvider(
            String providerId) throws NoSuchProviderException, SystemException {
        ConfigurableProvider p = getProvider(providerId);
        unregisterProvider(p);
    }

    public void unregisterProvider(
            ConfigurableProvider provider) throws SystemException {
        // we support only idp now
        if (SystemKeys.RESOURCE_IDENTITY.equals(provider.getType())) {
            IdentityAuthority a = authorityManager.getIdentityAuthority(provider.getAuthority());
            String providerId = provider.getProvider();

            // terminate sessions
            sessionManager.destroySessions(providerId);
            a.unregisterIdentityProvider(provider.getRealm(), providerId);
        } else if (SystemKeys.RESOURCE_ATTRIBUTES.equals(provider.getType())) {
            // TODO attribute providers
        } else {
            throw new SystemException("unsupported provider type");
        }

    }

    public boolean isProviderRegistered(String providerId) throws SystemException, NoSuchProviderException {
        ConfigurableProvider p = getProvider(providerId);
        return isProviderRegistered(p);
    }

    public boolean isProviderRegistered(ConfigurableProvider provider) throws SystemException {
        // we support only idp now
        if (SystemKeys.RESOURCE_IDENTITY.equals(provider.getType())) {
            IdentityAuthority a = authorityManager.getIdentityAuthority(provider.getAuthority());
            String providerId = provider.getProvider();

            IdentityProvider idp = a.getIdentityProvider(providerId);
            return !(idp == null);
        } else if (SystemKeys.RESOURCE_ATTRIBUTES.equals(provider.getType())) {
            // TODO attribute providers
            throw new SystemException("unsupported provider type");
        } else {
            throw new SystemException("unsupported provider type");
        }

    }

    private Collection<IdentityProvider> listGlobalIdentityProviders() {
        return globalIdps.values();
    }

    /*
     * Persist configuration
     * 
     * only for realm providers, global are configured only via app.properties to
     * avoid mangling with administrative sessions
     */

    private List<ConfigurableProvider> listProvidersByAuthority(String authority) {
        List<ProviderEntity> providers = providerService.listProvidersByAuthority(authority);
        return providers.stream().map(p -> fromEntity(p)).collect(Collectors.toList());
    }

    public Collection<ConfigurableProvider> listProviders(String realm) {
        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            // we do not persist in db global providers
            throw new SystemException("global providers are immutable");
        }

        List<ProviderEntity> providers = providerService.listProvidersByRealm(realm);
        return providers.stream().map(p -> fromEntity(p)).collect(Collectors.toList());

    }

    public Collection<ConfigurableProvider> listProviders(String realm, String type) {
        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            // we do not persist in db global providers
            throw new SystemException("global providers are immutable");
        }

        List<ProviderEntity> providers = providerService.listProvidersByRealmAndType(realm, type);
        return providers.stream().map(p -> fromEntity(p)).collect(Collectors.toList());

    }

    public ConfigurableProvider addProvider(String authority, String realm, String type,
            Map<String, String> configuration) throws SystemException {

        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            // we do not persist in db global providers
            throw new SystemException("global providers are immutable");
        }

        String providerId = generateId();
        ProviderEntity pe = providerService.addProvider(authority, providerId, realm, type, configuration);

        return fromEntity(pe);

    }

    public ConfigurableProvider updateProvider(String providerId, Map<String, String> configuration)
            throws NoSuchProviderException {
        ProviderEntity pe = providerService.getProvider(providerId);
        // check if active, we don't support update for active providers
        boolean isActive = false;
        // we support only idp now
        if (SystemKeys.RESOURCE_IDENTITY.equals(pe.getType())) {
            isActive = isProviderRegistered(providerId);
        }

        if (isActive) {
            throw new SystemException("can't update, provider is active");
        }

        // disable provider, after update users will be able to enable and activate
        pe = providerService.updateProvider(providerId, false, configuration);

        return fromEntity(pe);
    }

    public ConfigurableProvider updateProvider(String providerId, boolean enabled)
            throws NoSuchProviderException {
        ProviderEntity pe = providerService.getProvider(providerId);

        // check if active, we don't support update for active providers
        boolean isActive = false;
        // we support only idp now
        if (SystemKeys.RESOURCE_IDENTITY.equals(pe.getType())) {
            isActive = isProviderRegistered(providerId);
        }

        // check previous status
        boolean isEnabled = pe.isEnabled();

        // always sync
        pe = providerService.updateProvider(providerId, enabled, pe.getConfigurationMap());

        if (enabled && !isActive) {
            // start
            registerProvider(providerId);
        } else if (!enabled && isActive) {
            // stop
            unregisterProvider(providerId);
        }

        return fromEntity(pe);

    }

    public ConfigurableProvider getProvider(String providerId) throws NoSuchProviderException {
        ProviderEntity pe = providerService.getProvider(providerId);

        return fromEntity(pe);

    }

    public void deleteProvider(String providerId) throws SystemException, NoSuchProviderException {
        ProviderEntity pe = providerService.fetchProvider(providerId);

        if (pe != null) {
            // check if registered, we don't support delete for active providers
            boolean isActive = false;
            // we support only idp now
            if (SystemKeys.RESOURCE_IDENTITY.equals(pe.getType())) {
                isActive = isProviderRegistered(providerId);
            }

            if (isActive) {
                throw new SystemException("can't delete, provider is active");
            }

            providerService.deleteProvider(providerId);
        }
    }

    /*
     * Identity providers
     * 
     * we expose only getters to ensure consumers won't update config. Also only
     * active (ie registered with an authority) providers are exposed.
     * 
     * we assume that registered providers are a match for stored configuration,
     * since config is immutable in authorities
     */

    public IdentityProvider findIdentityProvider(String providerId) {

        // lookup in global map first
        if (globalIdps.containsKey(providerId)) {
            return globalIdps.get(providerId);
        }
        try {
            ConfigurableProvider provider = getProvider(providerId);

            if (!SystemKeys.RESOURCE_IDENTITY.equals(provider.getType())) {
                return null;
            }

            // lookup in authority
            IdentityAuthority ia = authorityManager.getIdentityAuthority(provider.getAuthority());
            return ia.getIdentityProvider(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public IdentityProvider getIdentityProvider(String providerId) throws NoSuchProviderException {
        IdentityProvider idp = findIdentityProvider(providerId);
        if (idp == null) {
            // provider is not active or not existing
            // TODO add dedicated exception?
            throw new NoSuchProviderException("provider not found");
        }

        return idp;
    }

    // fast load, skips db lookup, returns null if missing
    public IdentityProvider fetchIdentityProvider(String authority, String providerId) {
        // lookup in global map first
        if (globalIdps.containsKey(providerId)) {
            return globalIdps.get(providerId);
        }

        // lookup in authority
        IdentityAuthority ia = authorityManager.getIdentityAuthority(authority);
        if (ia == null) {
            return null;
        }
        return ia.getIdentityProvider(providerId);
    }

    public Collection<IdentityProvider> getIdentityProviders(String realm) {
        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            return listGlobalIdentityProviders();
        }

        Collection<ConfigurableProvider> providers = listProviders(realm, SystemKeys.RESOURCE_IDENTITY);

        // fetch each active provider from authority
        List<IdentityProvider> idps = new ArrayList<>();
        for (ConfigurableProvider provider : providers) {
            // lookup in authority
            IdentityAuthority ia = authorityManager.getIdentityAuthority(provider.getAuthority());
            IdentityProvider idp = ia.getIdentityProvider(provider.getProvider());
            if (idp != null) {
                idps.add(idp);
            }
        }

        return idps;
    }

    // fast load, skips db lookup
    public Collection<IdentityProvider> fetchIdentityProviders(String realm) {
        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            return listGlobalIdentityProviders();
        }

        List<IdentityProvider> providers = new ArrayList<>();
        for (IdentityAuthority ia : authorityManager.listIdentityAuthorities()) {
            providers.addAll(ia.getIdentityProviders(realm));
        }

        return providers;

    }

    // fast load, skips db lookup
    public Collection<IdentityProvider> fetchIdentityProviders(String authority, String realm) {
        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            return listGlobalIdentityProviders().stream()
                    .filter(i -> i.getAuthority().equals(authority))
                    .collect(Collectors.toList());
        }

        IdentityAuthority ia = authorityManager.getIdentityAuthority(authority);
        if (ia != null) {
            return ia.getIdentityProviders(realm);
        }

        return null;
    }

    /*
     * Helpers
     */

    private ConfigurableProvider fromEntity(ProviderEntity pe) {
        ConfigurableProvider idp = new ConfigurableProvider(pe.getAuthority(), pe.getProviderId(), pe.getRealm());
        idp.setType(pe.getType());
        idp.setConfiguration(pe.getConfigurationMap());

        return idp;

    }

    private String generateId() {
        // generate small unique id
        // TODO rewrite to avoid check
        String id = RandomStringUtils.randomAlphanumeric(8);
        try {
            ProviderEntity pe = providerService.getProvider(id);
            // re generate longer
            id = RandomStringUtils.randomAlphanumeric(10);
        } catch (NoSuchProviderException e) {

        }

        return id;
    }

}
