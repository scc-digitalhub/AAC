package it.smartcommunitylab.aac.openid.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.core.base.AbstractAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.OIDCKeys;

public class OIDCUserAuthenticatedPrincipal extends AbstractAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    // subject identifier from external provider is local id
    private final String subject;

    // link attributes
    private Boolean emailVerified;

    private OAuth2User principal;

    // locally set attributes, for example after custom mapping
    private Map<String, Serializable> attributes;

    public OIDCUserAuthenticatedPrincipal(String provider, String realm, String userId, String subject) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm, userId, subject);
    }

    public OIDCUserAuthenticatedPrincipal(String authority, String provider, String realm, String userId,
            String subject) {
        super(authority, provider, realm, userId);
        Assert.hasText(subject, "subject can not be null or empty");
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String getPrincipalId() {
        return subject;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public boolean isEmailVerified() {
        boolean verified = emailVerified != null ? emailVerified.booleanValue() : false;
        return StringUtils.hasText(emailAddress) && verified;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> result = new HashMap<>();

        if (principal != null) {
            // map only string attributes
            // TODO implement a mapper via script handling a json representation without
            // security related attributes
            principal.getAttributes().entrySet().stream()
                    .filter(e -> !OIDCKeys.JWT_ATTRIBUTES.contains(e.getKey()))
                    .filter(e -> (e.getValue() != null))
                    .forEach(e -> {
                        // put if absent to pick only first value when repeated
                        // TODO handle full mapping
                        result.putIfAbsent(e.getKey(), e.getValue().toString());
                    });

            if (isOidcUser()) {
                ((OidcUser) principal).getClaims()
                        .entrySet().stream()
                        .filter(e -> !OIDCKeys.JWT_ATTRIBUTES.contains(e.getKey()))
                        .filter(e -> (e.getValue() != null))
                        .forEach(e -> {
                            // put if absent to pick only first value when repeated
                            // TODO handle full mapping
                            result.putIfAbsent(e.getKey(), e.getValue().toString());
                        });
            }
        }

        if (attributes != null) {
            // local attributes overwrite oauth attributes when set
            attributes.entrySet().forEach(e -> result.put(e.getKey(), e.getValue()));
        }

        // make sure these are never overridden
        result.put("authority", getAuthority());
        result.put("provider", getProvider());
        result.put("sub", subject);
        result.put("id", subject);

        if (StringUtils.hasText(username)) {
            result.put("name", username);
        }

        if (StringUtils.hasText(emailAddress)) {
            result.put("email", emailAddress);
        }

        if (emailVerified != null) {
            result.put(OpenIdAttributesSet.EMAIL_VERIFIED, emailVerified.booleanValue());
        }

        return result;
    }

    public OAuth2User getPrincipal() {
        return principal;
    }

    public void setPrincipal(OAuth2User principal) {
        this.principal = principal;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    public String getEmail() {
        return emailAddress;
    }

    public void setEmail(String email) {
        this.emailAddress = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

}
