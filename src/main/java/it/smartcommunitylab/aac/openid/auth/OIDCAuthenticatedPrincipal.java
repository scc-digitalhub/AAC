package it.smartcommunitylab.aac.openid.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;

public class OIDCAuthenticatedPrincipal implements UserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    private final String provider;
    private final String realm;

    private final String userId;
    private String name;
    private OAuth2User principal;

    private Map<String, String> attributes;

    public OIDCAuthenticatedPrincipal(String provider, String realm, String userId) {
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
        if (attributes != null) {
            // local attributes overwrite oauth attributes when set
            return attributes;
        }

        Map<String, String> result = new HashMap<>();
        if (principal != null) {
            Map<String, Object> oauthAttributes = principal.getAttributes();

            // map only string attributes
            // TODO implement a mapper via script handling a json representation without
            // security related attributes
            for (Map.Entry<String, Object> e : oauthAttributes.entrySet()) {
                result.put(e.getKey(), e.getValue().toString());
            }

            if (isOidcUser()) {
                Map<String, Object> claims = ((OidcUser) principal).getClaims();
                for (Map.Entry<String, Object> e : claims.entrySet()) {
                    result.put(e.getKey(), e.getValue().toString());
                }
            }
        }
        return result;
    }

    public OAuth2User getPrincipal() {
        return principal;
    }

    public void setPrincipal(OAuth2User principal) {
        this.principal = principal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OAuth2User getOAuth2User() {
        return principal;
    }

    public boolean isOidcUser() {
        return (principal instanceof OidcUser);
    }

    public OidcUser getOidcUser() {
        if (isOidcUser()) {
            return (OidcUser) principal;
        }
        return null;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

//    @Override
//    public Map<String, String> getLinkingAttributes() {
//        Map<String, String> attributes = getAttributes();
//
//        // expose only realm+email
//        Map<String, String> result = new HashMap<>();
//        if (StringUtils.hasText(attributes.get("email"))) {
//            result.put("realm", getRealm());
//            result.put("email", attributes.get("email"));
//        }
//        return result;
//    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_OIDC;
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
