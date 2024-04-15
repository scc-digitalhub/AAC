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

package it.smartcommunitylab.aac.openid.token;

import it.smartcommunitylab.aac.openid.common.IdToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public interface IdTokenServices {
    /*
     * Create idToken
     */
    public IdToken createIdToken(OAuth2Authentication authentication) throws AuthenticationException;

    public IdToken createIdToken(OAuth2Authentication authentication, OAuth2AccessToken accessToken)
        throws AuthenticationException;

    public IdToken createIdToken(OAuth2Authentication authentication, String code) throws AuthenticationException;

    public IdToken createIdToken(OAuth2Authentication authentication, OAuth2AccessToken accessToken, String code)
        throws AuthenticationException;
}
