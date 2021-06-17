/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.oauth.token;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.openid.service.OIDCTokenEnhancer;

/**
 * Add additional information to accessTokens via a set of enhancers
 * 
 * @author raman
 * @author mat
 *
 */
public class AACTokenEnhancer implements TokenEnhancer, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int maxHttpHeaderSize = 4 * 1024;
    private int jwtTargetSize = 16 * 1024;

    // we need these separated to apply the correct order
    private ClaimsTokenEnhancer claimsEnhancer;
    private OIDCTokenEnhancer oidcEnhancer;
    private JwtTokenConverter tokenConverter;
    // TODO refresh token should be made here if needed

    @Override
    public void afterPropertiesSet() throws Exception {
        // evaluate requiring some enhancers to be present
    }

    @Override
    public AACOAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug("enhance for token " + accessToken);
        AACOAuth2AccessToken token = new AACOAuth2AccessToken(accessToken);

        // TODO refresh token only with offline_access
//        if (accessToken.getScope().contains(Config.SCOPE_OPERATION_CONFIRMED)) {
//            AACOAuth2AccessToken token = (AACOAuth2AccessToken) accessToken;
//            token.setRefreshToken(null);
//        }

        // add claims
        if (claimsEnhancer != null) {
            token = claimsEnhancer.enhance(token, authentication);
        }

        // convert to JWT
        if (tokenConverter != null) {
            token = tokenConverter.enhance(token, authentication);
        }

        // then build id_token to calculate correct at_hash
        if (accessToken.getScope().contains(Config.SCOPE_OPENID) && oidcEnhancer != null) {
            token = oidcEnhancer.enhance(token, authentication);
        }

        // validate result
        String result = token.getValue();

        // implicit flow isn't suited for large JWT transferred as fragment
        // TODO evaluate remediation action, or throw error
        String grantType = authentication.getOAuth2Request().getGrantType();
        if (Config.GRANT_TYPE_IMPLICIT.equals(grantType)) {
            // check size and print warn if exceeds 16k
            int jwtBytesSize = result.getBytes(Charset.forName("UTF-8")).length;
            if (jwtBytesSize >= jwtTargetSize) {
                logger.warn(
                        "jwt token bytes size " + String.valueOf(jwtBytesSize) + " is exceeding the safe threshold");
            }

            // also check if we consume more than half the header space
            // this will leave no space for id token
            if (accessToken.getScope().contains("openid")
                    && jwtBytesSize > Math.max(jwtTargetSize, maxHttpHeaderSize / 2)) {
                logger.error(
                        "jwt token bytes size " + String.valueOf(jwtBytesSize)
                                + " is exceeding the space available in header");
            }
        }

        return token;
    }

    public void setClaimsEnhancer(ClaimsTokenEnhancer claimsEnhancer) {
        this.claimsEnhancer = claimsEnhancer;
    }

    public void setOidcEnhancer(OIDCTokenEnhancer oidcEnhancer) {
        this.oidcEnhancer = oidcEnhancer;
    }

    public void setTokenConverter(JwtTokenConverter tokenConverter) {
        this.tokenConverter = tokenConverter;
    }

    public int getMaxHttpHeaderSize() {
        return maxHttpHeaderSize;
    }

    public void setMaxHttpHeaderSize(int maxHttpHeaderSize) {
        this.maxHttpHeaderSize = maxHttpHeaderSize;
    }

    public int getJwtTargetSize() {
        return jwtTargetSize;
    }

    public void setJwtTargetSize(int jwtTargetSize) {
        this.jwtTargetSize = jwtTargetSize;
    }

}
