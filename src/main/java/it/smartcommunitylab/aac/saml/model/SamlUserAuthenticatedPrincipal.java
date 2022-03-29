package it.smartcommunitylab.aac.saml.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAuthenticatedPrincipal;

public class SamlUserAuthenticatedPrincipal extends AbstractAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    // subject identifier from external provider
    private final String subjectId;
    private String username;

    // link attributes
    private String email;
    private Boolean emailVerified;

    private Saml2AuthenticatedPrincipal principal;

    // locally set attributes, for example after custom mapping
    private Map<String, Serializable> attributes;

    public SamlUserAuthenticatedPrincipal(String provider, String realm, String userId, String subjectId) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm, userId);
        Assert.notNull(subjectId, "subjectId cannot be null");
        this.subjectId = subjectId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    @Override
    public String getId() {
        return subjectId;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmailAddress() {
        return email;
    }

    @Override
    public boolean isEmailVerified() {
        boolean verified = emailVerified != null ? emailVerified.booleanValue() : false;
        return StringUtils.hasText(email) && verified;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> result = new HashMap<>();

        if (principal != null) {
            // we implement only first attribute
            Set<String> keys = principal.getAttributes().keySet();

            // map only string attributes
            // TODO implement a mapper via script handling a json representation without
            // security related attributes
            for (String key : keys) {
                Object value = principal.getFirstAttribute(key);
                if (value != null) {
                    result.put(key, value.toString());
                }
            }

        }

        if (attributes != null) {
            // local attributes overwrite saml attributes when set
            attributes.entrySet().forEach(e -> result.putIfAbsent(e.getKey(), e.getValue()));
        }

        // make sure these are never overridden
        result.put("provider", getProvider());
        result.put("subjectId", subjectId);
        result.put("id", subjectId);

        if (StringUtils.hasText(username)) {
            result.put("username", username);
        }

        if (StringUtils.hasText(email)) {
            result.put("email", email);
        }

        if (emailVerified != null) {
            result.put("emailVerified", emailVerified.booleanValue());
        }
        return result;
    }

    public Saml2AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Saml2AuthenticatedPrincipal principal) {
        this.principal = principal;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

}