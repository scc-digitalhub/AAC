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

package it.smartcommunitylab.aac.oauth.token;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.scope.OAuth2DCRResource;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

public class DCRTokenEnhancer implements TokenEnhancer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int DEFAULT_TOKEN_VALIDITY = 60 * 60 * 24 * 90; // 90 days
    private int tokenValiditySeconds;

    public DCRTokenEnhancer() {
        tokenValiditySeconds = DEFAULT_TOKEN_VALIDITY;
    }

    public void setTokenValiditySeconds(int tokenValiditySeconds) {
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    @Override
    public AACOAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug(
            "enhance access token " +
            accessToken.getTokenType() +
            " for " +
            authentication.getName() +
            " value " +
            accessToken.toString()
        );

        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        AACOAuth2AccessToken token = new AACOAuth2AccessToken(accessToken);

        if (isDcrToken(authentication) && clientId.equals(token.getSubject())) {
            // modify expiration to extended value
            Instant expiration = Instant.now().plusSeconds(tokenValiditySeconds);
            token.setExpiration(Date.from(expiration));

            // clear claims
            token.setClaims(Collections.emptyMap());

            // revert value to opaque
            token.setValue(token.getToken());
        }

        return token;
    }

    private boolean isDcrToken(OAuth2Authentication authentication) {
        OAuth2Request request = authentication.getOAuth2Request();
        if (!request.getScope().contains(Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION) || request.getScope().size() > 1) {
            return false;
        }

        if (!request.getResourceIds().contains(OAuth2DCRResource.RESOURCE_ID)) {
            return false;
        }

        if (authentication.getUserAuthentication() != null) {
            return false;
        }

        return true;
    }
}
