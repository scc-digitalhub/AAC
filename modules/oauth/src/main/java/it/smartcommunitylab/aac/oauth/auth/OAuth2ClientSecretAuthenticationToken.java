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

package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/*
 * A usernamePassword auth token to be used for clientId+secret auth
 */

public class OAuth2ClientSecretAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private String credentials;
    private String authenticationMethod;

    public OAuth2ClientSecretAuthenticationToken(String clientId, String clientSecret, String authenticationMethod) {
        super(clientId);
        this.credentials = clientSecret;
        this.authenticationMethod = authenticationMethod;
        setAuthenticated(false);
    }

    public OAuth2ClientSecretAuthenticationToken(
        String clientId,
        String clientSecret,
        String authenticationMethod,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(clientId, authorities);
        this.credentials = clientSecret;
        this.authenticationMethod = authenticationMethod;
    }

    @Override
    public String getCredentials() {
        return this.credentials;
    }

    public String getClientSecret() {
        return this.credentials;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }
}
