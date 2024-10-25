/*******************************************************************************
 * Copyright 2015-2019 Smart Community Lab, FBK
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.oauth.store;

import java.util.Collection;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.token.TokenStore;

/**
 * @author raman
 *
 */
public interface ExtTokenStore extends TokenStore {
    /**
     * Read access token using the corresponding refresh token
     *
     * @param tokenValue
     * @return
     */
    public OAuth2AccessToken readAccessTokenForRefreshToken(String tokenValue);

    /**
     * Read refresh token using the corresponding access token
     *
     * @param tokenValue
     * @return
     */
    public OAuth2RefreshToken readRefreshTokenForAccessToken(String tokenValue);

    /**
     * Read refresh token FOR update
     *
     * @param tokenValue
     * @return
     */
    public OAuth2RefreshToken readRefreshTokenForUpdate(String token);


    /**
     * @param clientId the client id to search
     * @return a collection of access tokens
     */
    Collection<OAuth2AccessToken> findTokensByUserName(String userName);


    /**
     * Delete tokens expired since interval
     * @param interval
     */
    public void deleteExpiredAccessTokens(long interval);

    /**
     * Delete tokens expired since interval
     * @param interval
     */
    public void deleteExpiredRefreshTokens(long interval);
}
