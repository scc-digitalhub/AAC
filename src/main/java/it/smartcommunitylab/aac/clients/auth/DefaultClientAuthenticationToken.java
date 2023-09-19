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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public abstract class DefaultClientAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // clientId is principal
    protected final String principal;

    // keep realm separated to support clients authentication in different realms
    protected String realm;

    // client details
    protected ClientDetails clientDetails;

    // web authentication details
    protected WebAuthenticationDetails webAuthenticationDetails;

    protected String authenticationMethod;

    public DefaultClientAuthenticationToken(String clientId) {
        super(null);
        this.principal = clientId;
        setAuthenticated(false);
    }

    public DefaultClientAuthenticationToken(String clientId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = clientId;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public Object getDetails() {
        return clientDetails;
    }

    public ClientDetails getClient() {
        return clientDetails;
    }

    public void setClient(ClientDetails clientDetails) {
        this.clientDetails = clientDetails;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public WebAuthenticationDetails getWebAuthenticationDetails() {
        return webAuthenticationDetails;
    }

    public void setWebAuthenticationDetails(WebAuthenticationDetails webAuthenticationDetails) {
        this.webAuthenticationDetails = webAuthenticationDetails;
    }

    public void setDetails(ClientDetails details) {
        this.clientDetails = details;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    @Override
    public String getCredentials() {
        return null;
    }

    @Override
    public String getPrincipal() {
        return this.principal;
    }

    @Override
    public String getName() {
        return principal;
    }

    public String getClientId() {
        return this.principal;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

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
        // nothing to do
    }
}
