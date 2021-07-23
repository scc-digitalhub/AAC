package it.smartcommunitylab.aac.spid.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;

public class SpidAuthenticatedPrincipal implements UserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    private final String provider;
    private final String realm;

    // spid upstream idp
    private final String idp;
    // transient id
    private final String userId;
    // unique id
    private String spidCode;

    private String name;
    private Saml2AuthenticatedPrincipal principal;

    public SpidAuthenticatedPrincipal(String provider, String realm, String idp, String userId) {
        Assert.notNull(userId, "userId cannot be null");
        Assert.notNull(provider, "provider cannot be null");
        Assert.notNull(realm, "realm cannot be null");
        Assert.notNull(idp, "idp cannot be null");

        this.idp = idp;
        this.userId = userId;
        this.provider = provider;
        this.realm = realm;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public String getIdp() {
        return idp;
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

            // map as string attributes
            for (String key : keys) {
                attributes.put(key, String.valueOf(principal.getFirstAttribute(key)));
            }

        }
        return attributes;
    }

    public Saml2AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public String getSpidCode() {
        return spidCode;
    }

    public void setSpidCode(String spidCode) {
        this.spidCode = spidCode;
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