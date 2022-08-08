package it.smartcommunitylab.aac.spid.auth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProvider;

public class SpidAuthenticatedPrincipal extends AbstractAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    // subject identifier from external provider is local id
    private final String subjectId;
    // spid upstream idp
    private String idp;
    // spidCode identifier (when available)
    private String spidCode;

    // link attributes
    private Saml2AuthenticatedPrincipal principal;

    // locally set attributes, for example after custom mapping
    private Map<String, Serializable> attributes;

    public SpidAuthenticatedPrincipal(String provider, String realm, String userId, String subjectId) {
        super(SystemKeys.AUTHORITY_SPID, provider, realm, userId);
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
    public boolean isEmailVerified() {
        return StringUtils.hasText(emailAddress);
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> result = new HashMap<>();

        if (principal != null) {
            // get allowed attributes as strings or list of strings
            principal.getAttributes().entrySet().stream()
                    .filter(e -> !ArrayUtils.contains(SpidIdentityProvider.SAML_ATTRIBUTES, e.getKey()))
                    .filter(e -> (e.getValue() != null && !e.getValue().isEmpty()))
                    .forEach(e -> {
                        String key = e.getKey();
                        // map to String
                        List<String> values = e.getValue().stream().map(o -> o.toString()).collect(Collectors.toList());
                        if (values.size() == 1) {
                            result.put(key, values.get(0));
                        } else {
                            result.put(key, new ArrayList<>(values));
                        }
                    });
        }

        if (attributes != null) {
            // local attributes overwrite saml attributes when set
            attributes.entrySet().forEach(e -> result.put(e.getKey(), e.getValue()));
        }

        // make sure these are never overridden
        result.put("provider", getProvider());
        result.put("subjectId", subjectId);
        result.put("id", subjectId);

        if (StringUtils.hasText(spidCode)) {
            result.put("spidCode", spidCode);
        }
        if (StringUtils.hasText(idp)) {
            result.put("idp", idp);
        }

        if (StringUtils.hasText(username)) {
            result.put("username", username);
        }

        if (StringUtils.hasText(emailAddress)) {
            result.put("email", emailAddress);
            // spid email is always trusted
            result.put("emailVerified", true);
        }

        return result;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSpidCode() {
        return spidCode;
    }

    public void setSpidCode(String spidCode) {
        this.spidCode = spidCode;
    }

    public String getIdp() {
        return idp;
    }

    public void setIdp(String idp) {
        this.idp = idp;
    }

    public Saml2AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Saml2AuthenticatedPrincipal principal) {
        this.principal = principal;
    }

    public void setUsername(String username) {
        this.username = username;
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

}