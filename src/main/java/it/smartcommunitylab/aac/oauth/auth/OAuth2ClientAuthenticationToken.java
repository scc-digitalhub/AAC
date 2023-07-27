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
import it.smartcommunitylab.aac.core.auth.DefaultClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public abstract class OAuth2ClientAuthenticationToken extends DefaultClientAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    // oauth2 client details
    protected OAuth2ClientDetails oauth2Details;

    public OAuth2ClientAuthenticationToken(String clientId) {
        super(clientId);
    }

    public OAuth2ClientAuthenticationToken(String clientId, Collection<? extends GrantedAuthority> authorities) {
        super(clientId, authorities);
    }

    public OAuth2ClientDetails getOAuth2ClientDetails() {
        return oauth2Details;
    }

    @Override
    public void eraseCredentials() {
        // we don't reset clientSecret or jwks because we need those for JWT
    }

    public void setOAuth2ClientDetails(OAuth2ClientDetails details) {
        this.oauth2Details = details;
    }
}
