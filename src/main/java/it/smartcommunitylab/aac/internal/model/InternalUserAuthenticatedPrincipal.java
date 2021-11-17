package it.smartcommunitylab.aac.internal.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class InternalUserAuthenticatedPrincipal implements UserAuthenticatedPrincipal, CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String provider;
    private final String realm;

    private final String userId;
    private String name;
    private InternalUserAccount principal;

    public InternalUserAuthenticatedPrincipal(String provider, String realm, String userId) {
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
            // map base attributes, these will be available for custom mapping
            attributes.put("username", principal.getUsername());
            attributes.put("id", Long.toString(principal.getId()));
            attributes.put("confirmed", Boolean.toString(principal.isConfirmed()));

            String name = principal.getName();
            if (StringUtils.hasText(name)) {
                attributes.put("name", name);
            }

            String surname = principal.getSurname();
            if (StringUtils.hasText(surname)) {
                attributes.put("surname", surname);
            }

            String email = principal.getEmail();
            if (StringUtils.hasText(email)) {
                attributes.put("email", email);
            }

            String lang = principal.getLang();
            if (StringUtils.hasText(lang)) {
                attributes.put("lang", lang);
            }

        }

        return attributes;
    }

    public InternalUserAccount getPrincipal() {
        return principal;
    }

    public void setPrincipal(InternalUserAccount principal) {
        this.principal = principal;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_INTERNAL;
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
    public void eraseCredentials() {
        this.principal.setPassword(null);

    }

}
