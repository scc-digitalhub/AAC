package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.MapperAttributeAuthority;
import it.smartcommunitylab.aac.attributes.ScriptAttributeAuthority;
import it.smartcommunitylab.aac.attributes.WebhookAttributeAuthority;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.AttributeAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeService;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.internal.InternalAttributeAuthority;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;

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
    private InternalIdentityAuthority internalIdentityAuthority;

    @Autowired
    private OIDCIdentityAuthority oidcIdentityAuthority;

    @Autowired
    private SamlIdentityAuthority samlIdentityAuthority;

    @Autowired
    private SpidIdentityAuthority spidIdentityAuthority;

    @Autowired
    private WebAuthnIdentityAuthority webAuthnIdentityAuthority;

    /*
     * Identity providers
     */

    public IdentityAuthority getIdentityAuthority(String authority) {
        if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
            return internalIdentityAuthority;
        } else if (SystemKeys.AUTHORITY_OIDC.equals(authority)) {
            return oidcIdentityAuthority;
        } else if (SystemKeys.AUTHORITY_SAML.equals(authority)) {
            return samlIdentityAuthority;
        } else if (SystemKeys.AUTHORITY_SPID.equals(authority)) {
            return spidIdentityAuthority;
        } else if (SystemKeys.AUTHORITY_WEBAUTHN.equals(authority)) {
            return webAuthnIdentityAuthority;
        }
        return null;
    }

    public List<IdentityAuthority> listIdentityAuthorities() {
        List<IdentityAuthority> result = new ArrayList<>();
        result.add(internalIdentityAuthority);
        result.add(oidcIdentityAuthority);
        result.add(samlIdentityAuthority);
        result.add(spidIdentityAuthority);
        result.add(webAuthnIdentityAuthority);
        return result;
    }

    /*
     * Attribute
     */
    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private InternalAttributeAuthority internalAttributeAuthority;

    @Autowired
    private MapperAttributeAuthority mapperAttributeAuthority;

    @Autowired
    private ScriptAttributeAuthority scriptAttributeAuthority;

    @Autowired
    private WebhookAttributeAuthority webhookAttributeAuthority;

    /*
     * Attribute providers
     */

    public AttributeAuthority getAttributeAuthority(String authority) {
        if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
            return internalAttributeAuthority;
        } else if (SystemKeys.AUTHORITY_MAPPER.equals(authority)) {
            return mapperAttributeAuthority;
        } else if (SystemKeys.AUTHORITY_SCRIPT.equals(authority)) {
            return scriptAttributeAuthority;
        } else if (SystemKeys.AUTHORITY_WEBHOOK.equals(authority)) {
            return webhookAttributeAuthority;
        }
        return null;
    }

    public List<AttributeAuthority> listAttributeAuthorities() {
        List<AttributeAuthority> result = new ArrayList<>();
        result.add(internalAttributeAuthority);
        result.add(mapperAttributeAuthority);
        result.add(scriptAttributeAuthority);
        result.add(webhookAttributeAuthority);
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
            return isAttributeProviderRegistered(authority, providerId);
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
