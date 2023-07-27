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
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public abstract class WrappedAuthenticationToken implements Authentication, CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAuthenticationToken token;

    // audit
    protected WebAuthenticationDetails authenticationDetails;

    public WrappedAuthenticationToken(AbstractAuthenticationToken token) {
        Assert.notNull(token, "token can not be null");
        this.token = token;
    }

    public AbstractAuthenticationToken getAuthenticationToken() {
        return token;
    }

    @Override
    public boolean isAuthenticated() {
        return token.isAuthenticated();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return token.getAuthorities();
    }

    @Override
    public Object getPrincipal() {
        return token.getPrincipal();
    }

    @Override
    public Object getCredentials() {
        // no credentials exposed, refer to embedded token
        return null;
    }

    @Override
    public String getName() {
        return token.getName();
    }

    @Override
    public Object getDetails() {
        return token.getDetails();
    }

    @Override
    public void eraseCredentials() {
        token.eraseCredentials();
    }

    public WebAuthenticationDetails getAuthenticationDetails() {
        return authenticationDetails;
    }

    public void setAuthenticationDetails(WebAuthenticationDetails authenticationDetails) {
        this.authenticationDetails = authenticationDetails;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot set this token to trusted");
    }
}
