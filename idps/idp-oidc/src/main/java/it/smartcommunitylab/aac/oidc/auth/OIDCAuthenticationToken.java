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

package it.smartcommunitylab.aac.oidc.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

public class OIDCAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    private final String subject;

    private OAuth2User principal;

    private OAuth2AccessToken accessToken;

    private OAuth2RefreshToken refreshToken;

    public OIDCAuthenticationToken(String subject) {
        super(null);
        this.subject = subject;
        this.setAuthenticated(false);
    }

    public OIDCAuthenticationToken(
        String subject,
        OAuth2User principal,
        OAuth2AccessToken accessToken,
        OAuth2RefreshToken refreshToken,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        Assert.hasText(subject, "sub can not be null or empty");
        Assert.notNull(principal, "principal cannot be null");
        Assert.notNull(accessToken, "accessToken cannot be null");
        this.subject = subject;
        this.principal = principal;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.setAuthenticated(true);
    }

    @Override
    public OAuth2User getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    public String getSubject() {
        return subject;
    }

    public OAuth2AccessToken getAccessToken() {
        return accessToken;
    }

    public OAuth2RefreshToken getRefreshToken() {
        return refreshToken;
    }
}
