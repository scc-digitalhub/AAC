package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;

@Service
public class AuthorityManager {

    public static final String TYPE_IDENTITY = SystemKeys.RESOURCE_IDENTITY;
    public static final String TYPE_ATTRIBUTES = SystemKeys.RESOURCE_ATTRIBUTES;

    @Autowired
    private SessionManager sessionManager;

    /*
     * Identity
     */
    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private InternalIdentityAuthority internalAuthority;

    @Autowired
    private OIDCIdentityAuthority oidcAuthority;

    @Autowired
    private SamlIdentityAuthority samlAuthority;

    @Autowired
    private SpidIdentityAuthority spidAuthority;

    /*
     * Identity providers
     */

    public IdentityAuthority getIdentityAuthority(String authority) {
        if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
            return internalAuthority;
        } else if (SystemKeys.AUTHORITY_OIDC.equals(authority)) {
            return oidcAuthority;
        } else if (SystemKeys.AUTHORITY_SAML.equals(authority)) {
            return samlAuthority;
        } else if (SystemKeys.AUTHORITY_SPID.equals(authority)) {
            return spidAuthority;
        }
        return null;
    }

    public List<IdentityAuthority> listIdentityAuthorities() {
        List<IdentityAuthority> result = new ArrayList<>();
        result.add(internalAuthority);
        result.add(oidcAuthority);
        result.add(samlAuthority);
        result.add(spidAuthority);
        return result;
    }

    /*
     * Private loaders
     * 
     * TODO add loadingCache, this should be used only to resolve authorities
     */

    private Collection<? extends ConfigurableProvider> listProviders(String type, String realm) {
        if (TYPE_IDENTITY.equals(type)) {
            return identityProviderService.listProviders(realm);
        }

        return Collections.emptyList();
    }

    private ConfigurableProvider findProvider(String type, String providerId) {
        ConfigurableProvider cp = null;
        if (TYPE_IDENTITY.equals(type)) {
            cp = identityProviderService.findProvider(providerId);
        }

        return cp;
    }

    private ConfigurableProvider getProvider(String type, String providerId)
            throws NoSuchProviderException {
        ConfigurableProvider cp = null;
        if (TYPE_IDENTITY.equals(type)) {
            cp = identityProviderService.getProvider(providerId);
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

    public IdentityProvider findIdentityProvider(String providerId) {
        try {
            ConfigurableProvider provider = getProvider(TYPE_IDENTITY, providerId);

            // lookup in authority
            IdentityAuthority ia = getIdentityAuthority(provider.getAuthority());
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
        // lookup in authority
        IdentityAuthority ia = getIdentityAuthority(authority);
        if (ia == null) {
            return null;
        }
        try {
            return ia.getIdentityProvider(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public Collection<IdentityProvider> getIdentityProviders(String realm) throws NoSuchRealmException {
        Collection<? extends ConfigurableProvider> providers = listProviders(TYPE_IDENTITY, realm);

        // fetch each active provider from authority
        List<IdentityProvider> idps = new ArrayList<>();
        for (ConfigurableProvider provider : providers) {
            // lookup in authority
            IdentityAuthority ia = getIdentityAuthority(provider.getAuthority());
            try {
                IdentityProvider idp = ia.getIdentityProvider(provider.getProvider());
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
    public Collection<IdentityProvider> fetchIdentityProviders(String realm) {
        List<IdentityProvider> providers = new ArrayList<>();
        for (IdentityAuthority ia : listIdentityAuthorities()) {
            providers.addAll(ia.getIdentityProviders(realm));
        }

        return providers;

    }

    // fast load, skips db lookup
    public Collection<IdentityProvider> fetchIdentityProviders(String authority, String realm) {
        IdentityAuthority ia = getIdentityAuthority(authority);
        if (ia != null) {
            return ia.getIdentityProviders(realm);
        }

        return null;
    }

    /*
     * Identity services
     * 
     * idp with persistence can expose an identity service for operations on stored
     * users, such as search,update,delete etc
     */

    public IdentityService findIdentityService(String providerId) {
        try {
            ConfigurableProvider provider = getProvider(TYPE_IDENTITY, providerId);

            // lookup in authority
            IdentityAuthority ia = getIdentityAuthority(provider.getAuthority());
            return ia.getIdentityService(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    public IdentityService getIdentityService(String providerId) throws NoSuchProviderException {
        IdentityService idp = findIdentityService(providerId);
        if (idp == null) {
            // provider is not active or not existing
            // TODO add dedicated exception?
            throw new NoSuchProviderException("provider not found");
        }

        return idp;
    }

    // fast load, skips db lookup, returns null if missing
    public IdentityService fetchIdentityService(String authority, String providerId) {
        // lookup in authority
        IdentityAuthority ia = getIdentityAuthority(authority);
        if (ia == null) {
            return null;
        }
        return ia.getIdentityService(providerId);
    }

    public Collection<IdentityService> getIdentityServices(String realm) throws NoSuchRealmException {
        Collection<? extends ConfigurableProvider> providers = listProviders(TYPE_IDENTITY, realm);

        // fetch each active provider from authority
        List<IdentityService> idps = new ArrayList<>();
        for (ConfigurableProvider provider : providers) {
            // lookup in authority
            IdentityAuthority ia = getIdentityAuthority(provider.getAuthority());
            IdentityService idp = ia.getIdentityService(provider.getProvider());
            if (idp != null) {
                idps.add(idp);
            }
        }

        return idps;
    }

    // fast load, skips db lookup
    public Collection<IdentityService> fetchIdentityServices(String realm) {
        List<IdentityService> providers = new ArrayList<>();
        for (IdentityAuthority ia : listIdentityAuthorities()) {
            providers.addAll(ia.getIdentityServices(realm));
        }

        return providers;

    }

    // fast load, skips db lookup
    public Collection<IdentityService> fetchIdentityServices(String authority, String realm) {
        IdentityAuthority ia = getIdentityAuthority(authority);
        if (ia != null) {
            return ia.getIdentityServices(realm);
        }

        return null;
    }

    /*
     * Public API: check provider registration with authorities
     */

    public boolean isIdentityProviderRegistered(String providerId) throws SystemException, NoSuchProviderException {
        ConfigurableProvider p = getProvider(TYPE_IDENTITY, providerId);
        return isIdentityProviderRegistered(p.getAuthority(), p.getProvider());
    }

    public boolean isIdentityProviderRegistered(String authority, String providerId) throws SystemException {
        IdentityAuthority a = getIdentityAuthority(authority);
        return a.hasIdentityProvider(providerId);
    }

    public boolean isProviderRegistered(String type, String authority, String providerId) throws SystemException {
        // we support only idp now
        if (TYPE_IDENTITY.equals(type)) {
            return isIdentityProviderRegistered(authority, providerId);
        } else if (TYPE_ATTRIBUTES.equals(type)) {
            // TODO attribute providers
            throw new IllegalArgumentException("unsupported provider type");
        } else {
            throw new IllegalArgumentException("unsupported provider type");
        }

    }

    /*
     * Enable/disable providers with authorities
     */

    public IdentityProvider registerIdentityProvider(ConfigurableIdentityProvider provider) throws SystemException {
        if (!provider.isEnabled()) {
            throw new IllegalArgumentException("provider is disabled");
        }

        IdentityAuthority a = getIdentityAuthority(provider.getAuthority());
        return a.registerIdentityProvider(provider);
    }

    public void unregisterIdentityProvider(ConfigurableIdentityProvider provider) throws SystemException {
        IdentityAuthority a = getIdentityAuthority(provider.getAuthority());
        String providerId = provider.getProvider();

        // terminate sessions
        sessionManager.destroyProviderSessions(providerId);
        a.unregisterIdentityProvider(providerId);
    }

}
