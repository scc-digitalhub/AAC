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

package it.smartcommunitylab.aac.clients.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.model.Client;
import it.smartcommunitylab.aac.clients.model.ClientAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class DefaultClientAuthenticationToken extends AbstractAuthenticationToken implements ClientAuthentication {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // auth principal is the subject
    protected final Subject subject;

    // keep realm separated to support clients authentication in different realms
    protected String realm;

    // client details
    protected ClientDetails details;

    // the token is created at the given time
    protected final Instant createdAt;

    //actual authentication tokens
    private final Set<ExtendedAuthenticationToken<? extends AuthenticatedPrincipal>> tokens;

    // web authentication details
    protected WebAuthenticationDetails webAuthenticationDetails;

    // client resources collected at auth time
    //stored in context for convenience, consumers should refresh
    //TODO evaluate JsonIgnore
    @JsonIgnore
    private Client client;

    public DefaultClientAuthenticationToken(
        String clientId,
        String realm,
        String name,
        ClientDetails clientDetails,
        Collection<? extends GrantedAuthority> authorities,
        List<ExtendedAuthenticationToken<? extends AuthenticatedPrincipal>> tokens
    ) {
        // we set authorities via super
        // we don't support null authorities list
        super(authorities);
        Assert.notEmpty(tokens, "at least one auth token is required");
        Assert.notEmpty(authorities, "authorities can not be empty");
        Assert.hasText(clientId, "clientId is required");
        Assert.notNull(realm, "realm is required");
        Assert.notNull(clientDetails, "details are required");

        this.subject = new Subject(clientId, realm, name, SystemKeys.RESOURCE_CLIENT);
        this.realm = realm;

        this.createdAt = Instant.now();

        //at least one token should be for clients to be authenticated
        boolean isAuthenticated = tokens
            .stream()
            .anyMatch(t -> (t.getPrincipal() instanceof ClientAuthenticatedPrincipal));
        super.setAuthenticated(isAuthenticated); // must use super, as we override

        this.details = clientDetails;

        //build user context
        //TODO refactor, use oauth2 for now
        this.client = new OAuth2Client(realm, clientId);

        //store tokens
        this.tokens = Collections.unmodifiableSet(new HashSet<>(tokens));
    }

    public DefaultClientAuthenticationToken(
        String clientId,
        String realm,
        String name,
        Collection<? extends GrantedAuthority> authorities,
        List<ExtendedAuthenticationToken<? extends AuthenticatedPrincipal>> tokens
    ) {
        this(clientId, realm, name, new ClientDetails(clientId, realm, null, authorities), authorities, tokens);
    }

    public DefaultClientAuthenticationToken(
        String clientId,
        String realm,
        String name,
        Collection<? extends GrantedAuthority> authorities,
        ExtendedAuthenticationToken<? extends AuthenticatedPrincipal> token
    ) {
        this(clientId, realm, name, authorities, Collections.singletonList(token));
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private DefaultClientAuthenticationToken() {
        this(null, null, null, null, (List<ExtendedAuthenticationToken<?>>) null);
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
    public ClientDetails getClientDetails() {
        return details;
    }

    @Override
    @Nullable
    public Client getClient() {
        return client;
    }

    @Override
    public void setClient(Client client) {
        this.client = client;
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
        // check if any token is expired
        return getAuthentications().stream().anyMatch(a -> a.isExpired());
    }

    public Set<ExtendedAuthenticationToken<? extends AuthenticatedPrincipal>> getAuthentications() {
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
        return "ClientAuthenticationToken [principal=" + subject + ", details=" + details + ", tokens=" + tokens + "]";
    }
}
