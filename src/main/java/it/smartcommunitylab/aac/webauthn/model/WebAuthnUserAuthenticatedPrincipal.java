package it.smartcommunitylab.aac.webauthn.model;

import java.util.HashMap;
import java.util.Map;

import com.yubico.webauthn.data.ByteArray;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnUserAuthenticatedPrincipal implements UserAuthenticatedPrincipal, CredentialsContainer {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String provider;
    private final String realm;

    private final String userId;
    private String name;
    private WebAuthnUserAccount principal;

    public WebAuthnUserAuthenticatedPrincipal(String provider, String realm, String userId) {
        Assert.notNull(userId, "userId cannot be null");
        Assert.notNull(provider, "provider cannot be null");
        Assert.notNull(realm, "realm cannot be null");

        this.userId = userId;
        this.provider = provider;
        this.realm = realm;
    }

    @Override
    public void eraseCredentials() {
        this.principal.setUserHandle(null);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attributes = new HashMap<>();
        if (principal != null) {
            // map base attributes, these will be available for custom mapping
            attributes.put("id", Long.toString(principal.getId()));

            String username = principal.getUsername();
            if (StringUtils.hasText(username)) {
                attributes.put("username", username);
            }
            String userHandle = ByteArray.fromBase64(principal.getUserHandle()).getBase64Url();
            if (StringUtils.hasText(userHandle)) {
                attributes.put("userHandle", userHandle);
            }
        }

        return attributes;
    }

    public void setPrincipal(WebAuthnUserAccount principal) {
        this.principal = principal;
    }

    public Object getPrincipal() {
        return this.principal;
    }

}
