package it.smartcommunitylab.aac.saml.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;

public class SamlAuthenticatedPrincipal implements UserAuthenticatedPrincipal {

    private final String provider;
    private final String realm;

    private final String userId;
    private String name;
    private Saml2AuthenticatedPrincipal principal;

    public SamlAuthenticatedPrincipal(String provider, String realm, String userId) {
        Assert.notNull(userId, "userId cannot be null");
        Assert.notNull(provider, "provider cannot be null");
        Assert.notNull(realm, "realm cannot be null");

        this.userId = userId;
        this.provider = provider;
        this.realm = realm;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attributes = new HashMap<>();
        if (principal != null) {
            // we implement only first attribute
            Set<String> keys = principal.getAttributes().keySet();

            // map only string attributes
            // TODO implement a mapper via script handling a json representation without
            // security related attributes
            for (String key : keys) {
                attributes.put(key, principal.getFirstAttribute(key).toString());
            }

        }
        return attributes;
    }

    public Saml2AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Saml2AuthenticatedPrincipal principal) {
        this.principal = principal;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_SAML;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getProvider() {
        return provider;
    }

}