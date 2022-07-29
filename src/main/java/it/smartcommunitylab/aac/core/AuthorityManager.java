package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.AttributeAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeService;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;

@Service
public class AuthorityManager implements InitializingBean {

    public static final String TYPE_IDENTITY = SystemKeys.RESOURCE_IDENTITY;
    public static final String TYPE_ATTRIBUTES = SystemKeys.RESOURCE_ATTRIBUTES;

    @Autowired
    private SessionManager sessionManager;

    /*
     * Services
     */
    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    private final Map<String, IdentityProviderAuthority> identityAuthorities;
    private final Map<String, AttributeAuthority> attributeAuthorities;

    /*
     * Constructor
     */
    public AuthorityManager(Collection<IdentityProviderAuthority> identityAuthorities,
            Collection<AttributeAuthority> attributeAuthorities) {

        Map<String, IdentityProviderAuthority> ias = identityAuthorities.stream()
                .collect(Collectors.toMap(e -> e.getAuthorityId(), e -> e));
        Map<String, AttributeAuthority> aas = attributeAuthorities.stream()
                .collect(Collectors.toMap(e -> e.getAuthorityId(), e -> e));

        this.identityAuthorities = Collections.unmodifiableMap(ias);
        this.attributeAuthorities = Collections.unmodifiableMap(aas);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(identityAuthorities, "at least one identity authority is required");
    }

    /*
     * Identity providers
     */

    public IdentityProviderAuthority getIdentityAuthority(String authority) {
        return identityAuthorities.get(authority);
    }

    public Collection<IdentityProviderAuthority> listIdentityAuthorities() {
        return identityAuthorities.values();
    }

    /*
     * Attribute
     */

    /*
     * Attribute providers
     */

    public AttributeAuthority getAttributeAuthority(String authority) {
        return attributeAuthorities.get(authority);
    }

    public Collection<AttributeAuthority> listAttributeAuthorities() {
        return attributeAuthorities.values();
    }

    /*
     * Private loaders
     * 
     * TODO add loadingCache, this should be used only to resolve authorities
     */

    private Collection<? extends ConfigurableProvider> listProviders(String type, String realm) {
        if (TYPE_IDENTITY.equals(type)) {
            return identityProviderService.listProviders(realm);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return attributeProviderService.listProviders(realm);
        }

        return Collections.emptyList();
    }

    private ConfigurableProvider findProvider(String type, String providerId) {
        ConfigurableProvider cp = null;
        if (TYPE_IDENTITY.equals(type)) {
            cp = identityProviderService.findProvider(providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            cp = attributeProviderService.findProvider(providerId);
        }

        return cp;
    }

    private ConfigurableProvider getProvider(String type, String providerId)
            throws NoSuchProviderException {
        ConfigurableProvider cp = null;
        if (TYPE_IDENTITY.equals(type)) {
            cp = identityProviderService.getProvider(providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            cp = attributeProviderService.getProvider(providerId);
        }

        return cp;
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

    public IdentityProvider<? extends UserIdentity> findIdentityProvider(
            String providerId) {
        try {
            ConfigurableProvider provider = getProvider(TYPE_IDENTITY, providerId);

            // lookup in authority
            IdentityProviderAuthority ia = getIdentityAuthority(provider.getAuthority());
            IdentityProvider<? extends UserIdentity> idp = ia
                    .getIdentityProvider(providerId);
            return idp;
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public IdentityProvider<? extends UserIdentity> getIdentityProvider(String providerId)
            throws NoSuchProviderException {
        IdentityProvider<? extends UserIdentity> idp = findIdentityProvider(providerId);
        if (idp == null) {
            // provider is not active or not existing
            // TODO add dedicated exception?
            throw new NoSuchProviderException("provider not found");
        }

        return idp;
    }

    // fast load, skips db lookup, returns null if missing
    public IdentityProvider<? extends UserIdentity> fetchIdentityProvider(String authority, String providerId) {
        // lookup in authority
        IdentityProviderAuthority ia = getIdentityAuthority(authority);
        if (ia == null) {
            return null;
        }
        try {
            return ia.getIdentityProvider(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public Collection<IdentityProvider<? extends UserIdentity>> getIdentityProviders(String realm)
            throws NoSuchRealmException {
        Collection<? extends ConfigurableProvider> providers = listProviders(TYPE_IDENTITY, realm);

        // fetch each active provider from authority
        List<IdentityProvider<? extends UserIdentity>> idps = new ArrayList<>();
        for (ConfigurableProvider provider : providers) {
            // lookup in authority
            IdentityProviderAuthority ia = getIdentityAuthority(provider.getAuthority());
            try {
                IdentityProvider<? extends UserIdentity> idp = ia.getIdentityProvider(provider.getProvider());
                if (idp != null) {
                    idps.add(idp);
                }
            } catch (NoSuchProviderException e) {
                // skip
            }

        }

        return idps;
    }

    // fast load, skips db lookup
    public Collection<IdentityProvider<? extends UserIdentity>> fetchIdentityProviders(String realm) {
        List<IdentityProvider<? extends UserIdentity>> providers = new ArrayList<>();
        for (IdentityProviderAuthority ia : listIdentityAuthorities()) {
            providers.addAll(ia.getIdentityProviders(realm));
        }

        return providers;

    }

    // fast load, skips db lookup
    public Collection<IdentityProvider<? extends UserIdentity>> fetchIdentityProviders(String authority, String realm) {
        List<IdentityProvider<? extends UserIdentity>> providers = new ArrayList<>();
        IdentityProviderAuthority ia = getIdentityAuthority(authority);
        if (ia != null) {
            providers.addAll(ia.getIdentityProviders(realm));
        }

        return providers;
    }

    /*
     * Identity services
     * 
     * idp with persistence can expose an identity service for operations on stored
     * users, such as search,update,delete etc
     */

    public IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials> findIdentityService(
            String providerId) {
        try {
            ConfigurableProvider provider = getProvider(TYPE_IDENTITY, providerId);

            // lookup in authority
            IdentityProviderAuthority ia = getIdentityAuthority(provider.getAuthority());
            return ia.getIdentityService(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials> getIdentityService(
            String providerId)
            throws NoSuchProviderException {
        IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials> idp = findIdentityService(
                providerId);
        if (idp == null) {
            // provider is not active or not existing
            // TODO add dedicated exception?
            throw new NoSuchProviderException("provider not found");
        }

        return idp;
    }

    // fast load, skips db lookup, returns null if missing
    public IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials> fetchIdentityService(
            String authority,
            String providerId) {
        // lookup in authority
        IdentityProviderAuthority ia = getIdentityAuthority(authority);
        if (ia == null) {
            return null;
        }
        return ia.getIdentityService(providerId);
    }

    public Collection<IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials>> getIdentityServices(
            String realm)
            throws NoSuchRealmException {
        Collection<? extends ConfigurableProvider> providers = listProviders(TYPE_IDENTITY, realm);

        // fetch each active provider from authority
        List<IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials>> idps = new ArrayList<>();
        for (ConfigurableProvider provider : providers) {
            // lookup in authority
            IdentityProviderAuthority ia = getIdentityAuthority(provider.getAuthority());
            IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials> idp = ia
                    .getIdentityService(provider.getProvider());
            if (idp != null) {
                idps.add(idp);
            }
        }

        return idps;
    }

    // fast load, skips db lookup
    public Collection<IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials>> fetchIdentityServices(
            String realm) {
        List<IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials>> providers = new ArrayList<>();
        for (IdentityProviderAuthority ia : listIdentityAuthorities()) {
            providers.addAll(ia.getIdentityServices(realm));
        }

        return providers;

    }

    // fast load, skips db lookup
    public Collection<IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials>> fetchIdentityServices(
            String authority, String realm) {
        List<IdentityService<? extends UserIdentity, ? extends UserAccount, ? extends UserCredentials>> providers = new ArrayList<>();
        IdentityProviderAuthority ia = getIdentityAuthority(authority);
        if (ia != null) {
            providers.addAll(ia.getIdentityServices(realm));
        }

        return providers;
    }

    /*
     * Public API: check provider registration with authorities
     */

    public boolean isIdentityProviderRegistered(String providerId) throws SystemException, NoSuchProviderException {
        ConfigurableProvider p = getProvider(TYPE_IDENTITY, providerId);
        return isIdentityProviderRegistered(p.getAuthority(), p.getProvider());
    }

    public boolean isIdentityProviderRegistered(String authority, String providerId) throws SystemException {
        IdentityProviderAuthority a = getIdentityAuthority(authority);
        return a.hasIdentityProvider(providerId);
    }

    public boolean isProviderRegistered(String type, String authority, String providerId) throws SystemException {
        // we support only idp now
        if (TYPE_IDENTITY.equals(type)) {
            return isIdentityProviderRegistered(authority, providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            return isAttributeProviderRegistered(authority, providerId);
        } else {
            throw new IllegalArgumentException("unsupported provider type");
        }

    }

    /*
     * Enable/disable providers with authorities
     */

    public IdentityProvider<? extends UserIdentity> registerIdentityProvider(
            ConfigurableIdentityProvider provider) throws SystemException {
        if (!provider.isEnabled()) {
            throw new IllegalArgumentException("provider is disabled");
        }

        IdentityProviderAuthority a = getIdentityAuthority(provider.getAuthority());
        return a.registerIdentityProvider(provider);
    }

    public void unregisterIdentityProvider(ConfigurableIdentityProvider provider)
            throws SystemException {
        IdentityProviderAuthority a = getIdentityAuthority(provider.getAuthority());
        String providerId = provider.getProvider();

        // terminate sessions
        sessionManager.destroyProviderSessions(providerId);
        a.unregisterIdentityProvider(providerId);
    }

    /*
     * Attribute providers
     * 
     * we expose only getters to ensure consumers won't update config. Also only
     * active (ie registered with an authority) providers are exposed.
     * 
     * we assume that registered providers are a match for stored configuration,
     * since config is immutable in authorities
     */

    public AttributeProvider findAttributeProvider(String providerId) {
        try {
            ConfigurableProvider provider = getProvider(TYPE_ATTRIBUTES, providerId);

            // lookup in authority
            AttributeAuthority ia = getAttributeAuthority(provider.getAuthority());
            return ia.getAttributeProvider(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public AttributeProvider getAttributeProvider(String providerId) throws NoSuchProviderException {
        AttributeProvider idp = findAttributeProvider(providerId);
        if (idp == null) {
            // provider is not active or not existing
            // TODO add dedicated exception?
            throw new NoSuchProviderException("provider not found");
        }

        return idp;
    }

    // fast load, skips db lookup, returns null if missing
    public AttributeProvider fetchAttributeProvider(String authority, String providerId) {
        // lookup in authority
        AttributeAuthority ia = getAttributeAuthority(authority);
        if (ia == null) {
            return null;
        }
        try {
            return ia.getAttributeProvider(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public Collection<AttributeProvider> getAttributeProviders(String realm) throws NoSuchRealmException {
        Collection<? extends ConfigurableProvider> providers = listProviders(TYPE_ATTRIBUTES, realm);

        // fetch each active provider from authority
        List<AttributeProvider> idps = new ArrayList<>();
        for (ConfigurableProvider provider : providers) {
            // lookup in authority
            AttributeAuthority ia = getAttributeAuthority(provider.getAuthority());
            try {
                AttributeProvider idp = ia.getAttributeProvider(provider.getProvider());
                if (idp != null) {
                    idps.add(idp);
                }
            } catch (NoSuchProviderException e) {
                // skip
            }

        }

        return idps;
    }

    // fast load, skips db lookup
    public Collection<AttributeProvider> fetchAttributeProviders(String realm) {
        List<AttributeProvider> providers = new ArrayList<>();
        for (AttributeAuthority ia : listAttributeAuthorities()) {
            providers.addAll(ia.getAttributeProviders(realm));
        }

        return providers;

    }

    // fast load, skips db lookup
    public Collection<AttributeProvider> fetchAttributeProviders(String authority, String realm) {
        AttributeAuthority ia = getAttributeAuthority(authority);
        if (ia != null) {
            return ia.getAttributeProviders(realm);
        }

        return null;
    }

    /*
     * Attribute services
     * 
     */

    public AttributeService findAttributeService(String providerId) {
        try {
            ConfigurableProvider provider = getProvider(TYPE_ATTRIBUTES, providerId);

            // lookup in authority
            AttributeAuthority ia = getAttributeAuthority(provider.getAuthority());
            return ia.getAttributeService(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public AttributeService getAttributeService(String providerId) throws NoSuchProviderException {
        AttributeService ap = findAttributeService(providerId);
        if (ap == null) {
            // provider is not active or not existing
            // TODO add dedicated exception?
            throw new NoSuchProviderException("provider not found");
        }

        return ap;
    }

    // fast load, skips db lookup, returns null if missing
    public AttributeService fetchAttributeService(String authority, String providerId) {
        // lookup in authority
        AttributeAuthority ia = getAttributeAuthority(authority);
        if (ia == null) {
            return null;
        }
        return ia.getAttributeService(providerId);
    }

    public Collection<AttributeService> getAttributeServices(String realm) throws NoSuchRealmException {
        Collection<? extends ConfigurableProvider> providers = listProviders(TYPE_ATTRIBUTES, realm);

        // fetch each active provider from authority
        List<AttributeService> aps = new ArrayList<>();
        for (ConfigurableProvider provider : providers) {
            // lookup in authority
            AttributeAuthority ia = getAttributeAuthority(provider.getAuthority());
            AttributeService ap = ia.getAttributeService(provider.getProvider());
            if (ap != null) {
                aps.add(ap);
            }
        }

        return aps;
    }

    // fast load, skips db lookup
    public Collection<AttributeService> fetchAttributeServices(String realm) {
        List<AttributeService> providers = new ArrayList<>();
        for (AttributeAuthority ia : listAttributeAuthorities()) {
            providers.addAll(ia.getAttributeServices(realm));
        }

        return providers;

    }

    // fast load, skips db lookup
    public Collection<AttributeService> fetchAttributeServices(String authority, String realm) {
        AttributeAuthority ia = getAttributeAuthority(authority);
        if (ia != null) {
            return ia.getAttributeServices(realm);
        }

        return null;
    }

    /*
     * Public API: check provider registration with authorities
     */

    public boolean isAttributeProviderRegistered(String providerId) throws SystemException, NoSuchProviderException {
        ConfigurableProvider p = getProvider(TYPE_ATTRIBUTES, providerId);
        return isAttributeProviderRegistered(p.getAuthority(), p.getProvider());
    }

    public boolean isAttributeProviderRegistered(String authority, String providerId) throws SystemException {
        AttributeAuthority a = getAttributeAuthority(authority);
        return a.hasAttributeProvider(providerId);
    }

    /*
     * Enable/disable providers with authorities
     */

    public AttributeProvider registerAttributeProvider(ConfigurableAttributeProvider provider) throws SystemException {
        if (!provider.isEnabled()) {
            throw new IllegalArgumentException("provider is disabled");
        }

        AttributeAuthority a = getAttributeAuthority(provider.getAuthority());
        return a.registerAttributeProvider(provider);
    }

    public void unregisterAttributeProvider(ConfigurableAttributeProvider provider) throws SystemException {
        AttributeAuthority a = getAttributeAuthority(provider.getAuthority());
        String providerId = provider.getProvider();

        a.unregisterAttributeProvider(providerId);
    }

}
