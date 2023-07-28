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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public abstract class ExtendedAuthenticationProvider<P extends UserAuthenticatedPrincipal, A extends UserAccount>
    extends AbstractProvider
    implements AuthenticationProvider {

    public ExtendedAuthenticationProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_AUTHENTICATION;
    }

    @Override
    public ExtendedAuthenticationToken authenticate(Authentication authentication) throws AuthenticationException {
        // process authentication
        // subclasses should implement the validation
        // TODO implement preAuthChecks
        Authentication authResponse = doAuthenticate(authentication);

        // TODO implement postAuthChecks

        // we expect a fully populated user principal
        P principal = createUserPrincipal(authResponse.getPrincipal());

        // create the ext token
        // subclasses could re-implement the method

        return createExtendedAuthentication(principal, authResponse);
    }

    // subclasses need to implement this, they should have the knowledge
    protected abstract Authentication doAuthenticate(Authentication authentication);

    protected abstract P createUserPrincipal(Object principal);

    protected Instant expiresAt(Authentication authentication) {
        // default to no expiration set
        return null;
    }

    protected ExtendedAuthenticationToken createExtendedAuthentication(P principal, Authentication authentication) {
        // build the token with both the extracted userPrincipal and the original auth
        // note that the token could contain the original credentials
        // we want those for further processing, will be cleared later by manager
        // note we don't transfer granted authorities from provider, we will handle
        // those on subject
        return new ExtendedAuthenticationToken(
            getAuthority(),
            getProvider(),
            getRealm(),
            principal,
            authentication,
            expiresAt(authentication)
        );
    }
}
