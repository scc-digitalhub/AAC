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

package it.smartcommunitylab.aac.users.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class DefaultUserAuthenticationToken extends AbstractAuthenticationToken implements UserAuthentication {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // auth principal is the subject
    protected final Subject subject;

    // user userDetails
    protected UserDetails details;

    // the auth is bound to a single realm,
    protected final String realm;

    // the token is created at the given time
    protected final Instant createdAt;

    // we collect authentications for users
    // do note that tokens are *supposed* to be associated to the same user
    // but this is *not* a hard-requirement: we could store client auth
    // when relevant to the user auth, for example for MFA
    private final Set<ExtendedAuthenticationToken> tokens;

    // web authentication details
    private WebAuthenticationDetails webAuthenticationDetails;

    // user resources collected at auth time
    //stored in context for convenience, consumers should refresh
    //TODO evaluate JsonIgnore
    @JsonIgnore
    private User user;

    // audit
    // TODO

    public DefaultUserAuthenticationToken(
        Subject principal,
        String realm,
        String username,
        Collection<? extends GrantedAuthority> authorities,
        List<ExtendedAuthenticationToken> tokens
    ) {
        // we set authorities via super
        // we don't support null authorities list
        super(authorities);
        Assert.notEmpty(tokens, "at least one auth token is required");
        Assert.notEmpty(authorities, "authorities can not be empty");
        Assert.notNull(principal, "principal is required");
        Assert.notNull(realm, "realm is required");

        this.subject = principal;
        this.realm = realm;

        this.createdAt = Instant.now();

        super.setAuthenticated(true); // must use super, as we override

        //build details
        this.details = new UserDetails(principal.getSubjectId(), realm, username, authorities);

        //store tokens
        this.tokens = new HashSet<>(tokens);
    }

    public DefaultUserAuthenticationToken(
        Subject principal,
        String realm,
        String username,
        Collection<? extends GrantedAuthority> authorities,
        ExtendedAuthenticationToken tokens
    ) {
        this(principal, realm, username, authorities, Arrays.asList(tokens));
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private DefaultUserAuthenticationToken() {
        this(null, null, null, null, (List<ExtendedAuthenticationToken>) null);
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public UserDetails getUserDetails() {
        return details;
    }

    @Override
    @Nullable
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
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

        //we let managers de-authenticate user
        super.setAuthenticated(false);
    }

    /*
     * Auth tokens
     */

    public boolean isExpired() {
        // check if any token is valid
        return !getAuthentications().stream().filter(a -> !a.isExpired()).findFirst().isPresent();
    }

    public void addAuthentication(ExtendedAuthenticationToken auth) {
        // TODO implement a proper lock
        synchronized (this) {
            if (!realm.equals(auth.getRealm())) {
                throw new IllegalArgumentException("realm does not match");
            }

            this.tokens.add(auth);
        }
    }

    public ExtendedAuthenticationToken getAuthentication(String authority, String provider, String userId) {
        ExtendedAuthenticationToken token = null;
        for (ExtendedAuthenticationToken t : tokens) {
            if (
                t.getAuthority().equals(authority) &&
                t.getProvider().equals(provider) &&
                t.getPrincipal().getUserId().equals(userId)
            ) {
                token = t;
                break;
            }
        }

        // we return the original
        // we expect consumers to avoid mangling the token or resetting the
        // authenticated flag
        return token;
    }

    public void eraseAuthentication(ExtendedAuthenticationToken auth) {
        // TODO implement a proper lock
        synchronized (this) {
            this.tokens.remove(auth);
        }
    }

    public Set<ExtendedAuthenticationToken> getAuthentications() {
        return tokens;
    }

    /*
     * web auth details
     */
    public WebAuthenticationDetails getWebAuthenticationDetails() {
        return webAuthenticationDetails;
    }

    public void setWebAuthenticationDetails(WebAuthenticationDetails webAuthenticationDetails) {
        this.webAuthenticationDetails = webAuthenticationDetails;
    }

    @Override
    public String toString() {
        return "UserAuthenticationToken [principal=" + subject + ", details=" + details + ", tokens=" + tokens + "]";
    }
}
