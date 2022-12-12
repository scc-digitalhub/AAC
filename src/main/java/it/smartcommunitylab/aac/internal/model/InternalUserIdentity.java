package it.smartcommunitylab.aac.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.model.UserCredentialsIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserIdentity extends AbstractIdentity implements UserCredentialsIdentity {
    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_IDENTITY + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_INTERNAL;

    // authentication principal (if available)
    private final InternalUserAuthenticatedPrincipal principal;

    // internal user account
    private final InternalUserAccount account;

    // credentials (when available)
    // TODO evaluate exposing on abstract identity model for all providers
    private List<UserCredentials> credentials;

    // attributes map for sets associated with this identity
    private Map<String, UserAttributes> attributes;

    public InternalUserIdentity(String authority, String provider, String realm, InternalUserAccount account) {
        super(authority, provider);
        Assert.notNull(account, "account can not be null");

        this.account = account;
        this.principal = null;
        this.attributes = Collections.emptyMap();

        setUserId(account.getUserId());
        setUuid(account.getUuid());
        setRealm(realm);
    }

    public InternalUserIdentity(
            String authority, String provider, String realm,
            InternalUserAccount account, InternalUserAuthenticatedPrincipal principal) {
        super(authority, provider);
        Assert.notNull(account, "account can not be null");

        this.account = account;
        this.principal = principal;
        this.attributes = Collections.emptyMap();

        setUserId(account.getUserId());
        setUuid(account.getUuid());
        setRealm(realm);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public InternalUserAuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public InternalUserAccount getAccount() {
        return account;
    }

    @Override
    public Collection<UserAttributes> getAttributes() {
        return attributes.values();
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new HashMap<>();
        if (attributes != null) {
            attributes.forEach(a -> this.attributes.put(a.getIdentifier(), a));
        }
    }

    public String getUsername() {
        return account.getUsername();
    }

    public String getEmailAddress() {
        return account.getEmail();
    }

    public List<UserCredentials> getCredentials() {
        return credentials;
    }

    public void setCredentials(Collection<? extends UserCredentials> credentials) {
        this.credentials = new ArrayList<>(credentials);
    }

    @Override
    public void eraseCredentials() {
        if (this.credentials != null) {
            credentials.stream().forEach(c -> c.eraseCredentials());
        }

    }

}
