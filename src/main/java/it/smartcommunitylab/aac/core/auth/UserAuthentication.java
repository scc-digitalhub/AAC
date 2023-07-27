/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.model.Subject;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 *
 */
public abstract class UserAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // auth principal is the subject
    protected final Subject principal;

    // the token is bound to a single realm, since we can match identities only over
    // same realm by design
    protected final String realm;

    // the token is created at the given time
    protected final Instant createdAt;

    public UserAuthentication(
        Subject principal,
        String realm,
        Collection<? extends GrantedAuthority> authorities,
        boolean isAuthenticated
    ) {
        // we set authorities via super
        // we don't support null authorities list
        super(authorities);
        Assert.notEmpty(authorities, "authorities can not be empty");
        Assert.notNull(principal, "principal is required");
        Assert.notNull(realm, "realm is required");

        this.principal = principal;
        this.realm = realm;

        this.createdAt = Instant.now();

        super.setAuthenticated(isAuthenticated); // must use super, as we override
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private UserAuthentication() {
        this(null, null, null, false);
    }

    @Override
    public Object getCredentials() {
        // no credentials here, we expect those handled at account level
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.getSubjectId();
    }

    @Override
    public abstract Object getDetails();

    @JsonIgnore
    public Subject getSubject() {
        return principal;
    }

    public String getRealm() {
        return realm;
    }

    public String getSubjectId() {
        return principal.getSubjectId();
    }

    @JsonIgnore
    public abstract UserDetails getUser();

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getAge() {
        if (createdAt != null) {
            return Duration.between(createdAt, Instant.now()).getSeconds();
        }
        return -1;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead"
            );
        }

        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
    }

    /*
     * Auth tokens
     */

    public abstract ExtendedAuthenticationToken getAuthentication(String authority, String provider, String userId);

    public abstract void eraseAuthentication(ExtendedAuthenticationToken auth);

    public abstract Set<ExtendedAuthenticationToken> getAuthentications();

    public boolean isExpired() {
        // check if any token is valid
        return !getAuthentications().stream().filter(a -> !a.isExpired()).findFirst().isPresent();
    }

    /*
     * web auth details
     */
    public abstract WebAuthenticationDetails getWebAuthenticationDetails();
}
