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
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.model.Subject;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class DefaultUserAuthenticationToken extends UserAuthentication {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // subject userDetails with multiple identities bound
    private UserDetails details;

    // we collect authentications for identities
    // this way consumers will be able to verify if a identity is authenticated
    // (by default only authenticated identities should populate userDetails)
    // note: we could have more than one token for the same identity, someone else
    // should evaluate
    // we should also purge expired auth tokens
    private final Set<ExtendedAuthenticationToken> tokens;

    // web authentication details
    private WebAuthenticationDetails webAuthenticationDetails;

    // audit
    // TODO

    public DefaultUserAuthenticationToken(
        Subject principal,
        String realm,
        ExtendedAuthenticationToken auth,
        UserIdentity identity,
        Collection<UserAttributes> attributeSets,
        Collection<? extends GrantedAuthority> authorities
    ) {
        // we set authorities via super
        // we don't support null authorities list
        super(principal, realm, authorities, auth != null ? auth.isAuthenticated() : false);
        Assert.notNull(auth, "auth token for identity is required");
        Assert.notNull(identity, "identity is required");

        this.details = new UserDetails(principal.getSubjectId(), realm, identity, attributeSets, authorities);

        this.tokens = new HashSet<>();
        this.tokens.add(auth);
    }

    public DefaultUserAuthenticationToken(
        Subject principal,
        String realm,
        Collection<? extends GrantedAuthority> authorities,
        UserAuthentication... authenticationTokens
    ) {
        super(
            principal,
            realm,
            authorities,
            authenticationTokens.length > 0 ? authenticationTokens[0].isAuthenticated() : false
        );
        Assert.notEmpty(authorities, "authorities can not be empty");
        Assert.notEmpty(authenticationTokens, "at least one authentication token is required");

        this.tokens = new HashSet<>();

        // use first token as base
        UserAuthentication token = authenticationTokens[0];
        this.details =
            new UserDetails(
                principal.getSubjectId(),
                realm,
                token.getUser().getIdentities(),
                token.getUser().getAttributeSets(true),
                authorities
            );

        // add auth tokens
        this.tokens.addAll(token.getAuthentications());

        // process additional tokens
        Arrays
            .stream(authenticationTokens)
            .skip(1)
            .forEach(t -> {
                // identities
                for (UserIdentity i : t.getUser().getIdentities()) {
                    details.addIdentity(i);
                }

                // attributes
                for (UserAttributes ras : t.getUser().getAttributeSets(true)) {
                    details.addAttributeSet(ras);
                }

                // tokens
                tokens.addAll(t.getAuthentications());
            });
    }

    @Override
    public Object getDetails() {
        return details;
    }

    @JsonIgnore
    public UserDetails getUser() {
        return details;
    }

    /*
     * Auth tokens
     */

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
        return "UserAuthenticationToken [principal=" + principal + ", details=" + details + ", tokens=" + tokens + "]";
    }
}
