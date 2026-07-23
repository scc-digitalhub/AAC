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

package it.smartcommunitylab.aac.oauth.event;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.Assert;

public class TokenGrantEvent extends OAuth2Event {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private final OAuth2AccessToken token;
    private final OAuth2Authentication authentication;
    private final OAuth2ClientAuthenticationToken clientAuth;

    // the grant type that produced this token (e.g. authorization_code, refresh_token)
    private final String grantType;

    public TokenGrantEvent(OAuth2AccessToken token, OAuth2Authentication authentication, OAuth2ClientAuthenticationToken clientAuth) {
        this(token, authentication, clientAuth, null);
    }

    public TokenGrantEvent(
            OAuth2AccessToken token,
            OAuth2Authentication authentication,
            OAuth2ClientAuthenticationToken clientAuth,
            String grantType
    ) {
        super(authentication.getOAuth2Request());
        Assert.notNull(token, "token can not be null");
        this.token = token;
        this.authentication = authentication;
        this.clientAuth = clientAuth;
        this.grantType = grantType;
    }

    public OAuth2AccessToken getToken() {
        return token;
    }

    public OAuth2Authentication getAuthentication() {
        return authentication;
    }

    public OAuth2ClientAuthenticationToken getClientAuthentication() {
        return clientAuth;
    }

    public String getGrantType() {
        return grantType;
    }
}
