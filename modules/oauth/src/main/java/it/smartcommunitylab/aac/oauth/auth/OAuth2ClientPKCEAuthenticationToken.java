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
 * A usernamePassword auth token to be used for clientId+verifier auth
 */

public class OAuth2ClientPKCEAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private String codeVerifier;

    private String code;

    public OAuth2ClientPKCEAuthenticationToken(String clientId, String code, String codeVerifier) {
        super(clientId);
        this.codeVerifier = codeVerifier;
        this.code = code;
        setAuthenticated(false);
    }

    public OAuth2ClientPKCEAuthenticationToken(
        String clientId,
        String code,
        String codeVerifier,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(clientId, authorities);
        this.codeVerifier = codeVerifier;
        this.code = code;
    }

    @Override
    public String getCredentials() {
        return this.codeVerifier;
    }

    public String getCode() {
        return this.code;
    }

    public String getCodeVerifier() {
        return this.codeVerifier;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.codeVerifier = null;
        this.code = null;
    }
}
