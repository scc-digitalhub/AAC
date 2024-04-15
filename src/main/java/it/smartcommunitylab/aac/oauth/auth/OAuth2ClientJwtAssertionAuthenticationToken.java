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

public class OAuth2ClientJwtAssertionAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public static final String CLIENT_ASSERTION_TYPE_VALUE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private String clientAssertion;

    public OAuth2ClientJwtAssertionAuthenticationToken(String clientId, String clientAssertion) {
        super(clientId);
        this.clientAssertion = clientAssertion;
        setAuthenticated(false);
    }

    public OAuth2ClientJwtAssertionAuthenticationToken(
        String clientId,
        String clientAssertion,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(clientId, authorities);
        this.clientAssertion = clientAssertion;
    }

    public String getClientAssertion() {
        return clientAssertion;
    }

    @Override
    public String getCredentials() {
        return this.clientAssertion;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.clientAssertion = null;
    }
}
