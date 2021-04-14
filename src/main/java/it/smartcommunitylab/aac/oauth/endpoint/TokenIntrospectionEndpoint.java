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

package it.smartcommunitylab.aac.oauth.endpoint;

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.TokenIntrospection;

/**
 * OAuth2.0 Token introspection controller as of RFC7662:
 * https://tools.ietf.org/html/rfc7662.
 * 
 * @author raman
 *
 */
@Controller
@Api(tags = { "OAuth 2.0 Token Introspection" })
public class TokenIntrospectionEndpoint {

    public final static String TOKEN_INTROSPECTION_URL = "/oauth/introspect";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${oauth2.introspection.permitAll}")
    private boolean permitAll;

    @Autowired
    private TokenStore tokenStore;

    /*
     * TODO: evaluate if client_id should match audience, as per security
     * considerations https://tools.ietf.org/html/rfc7662#section-4
     */

//    @SuppressWarnings("unchecked")
    @ApiOperation(value = "Get token metadata")
    @RequestMapping(method = RequestMethod.POST, value = TOKEN_INTROSPECTION_URL)
    public ResponseEntity<TokenIntrospection> getTokenInfo(
            @RequestParam(name = "token") String tokenValue,
            @RequestParam(required = false, defaultValue = "access_token") String token_type_hint,
            Authentication authentication) {

        logger.debug("request introspection of token " + tokenValue + " hint " + String.valueOf(token_type_hint));

        if (!(authentication instanceof OAuth2ClientAuthenticationToken)) {
            throw new InsufficientAuthenticationException(
                    "There is no client authentication. Try adding an appropriate authentication filter.");
        }

        // clientAuth should be available
        OAuth2ClientAuthenticationToken introspectClientAuth = (OAuth2ClientAuthenticationToken) authentication;
        OAuth2ClientDetails introspectClientDetails = introspectClientAuth.getOAuth2ClientDetails();
        String introspectClientId = introspectClientDetails.getClientId();

        if (!StringUtils.hasText(tokenValue)) {
            throw new InvalidRequestException("missing or invalid token");
        }

        TokenIntrospection result;

        try {
            OAuth2Authentication auth = null;
            OAuth2RefreshToken refreshToken = null;
            OAuth2AccessToken accessToken = null;

            // check hint
            if ("refresh_token".equals(token_type_hint)) {
                // load refresh token if present
                refreshToken = tokenStore.readRefreshToken(tokenValue);
                if (refreshToken != null) {
                    logger.trace("load auth for refresh token " + refreshToken.getValue());

                    // load authentication
                    auth = tokenStore.readAuthenticationForRefreshToken(refreshToken);
                }
            }

            if (auth == null) {
                // either token is access_token, or
                // as per spec, try as access_token independently of hint
                accessToken = tokenStore.readAccessToken(tokenValue);
                if (accessToken != null) {
                    logger.trace("load auth for access token " + accessToken.getValue());

                    // load authentication
                    auth = tokenStore.readAuthentication(accessToken);
                }

            }

            if (auth == null) {
                // not found
                throw new InvalidTokenException("no token for value " + tokenValue);
            }

            // extract request
            OAuth2Request request = auth.getOAuth2Request();
            String clientId = request.getClientId();
            Set<String> scopes = request.getScope();

            logger.trace("token clientId " + clientId);
            logger.trace("client auth requesting introspection  " + introspectClientId);

            if (permitAll || clientId.equals(introspectClientId)) {

                // if token exists is valid since revoked ones are not returned from tokenStore
                result = new TokenIntrospection(true);

                // add base response
                result.setIssuer(issuer);
                result.setClientId(clientId);
                result.setScope(StringUtils.collectionToDelimitedString(scopes, " "));
                // TODO read from accessToken when we introduce proper support
                result.setTokenType(TokenType.BEARER.getValue());

                // check tokenType
                if (refreshToken != null) {
                    // these make sense only on refresh tokens with expiration
                    if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
                        ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) refreshToken;
                        result.setExpirationTime((int) (expiringRefreshToken.getExpiration().getTime() / 1000));
                        // no iat in refreshToken..
                    }
                }

                if (accessToken != null) {
                    // these make sense only on access tokens
                    result.setExpirationTime((int) (accessToken.getExpiration().getTime() / 1000));
                    int iat = (int) (new Date().getTime() / 1000);
                    int nbf = iat;
                    iat = result.getExpirationTime() - accessToken.getExpiresIn();
                    nbf = iat;
                    result.setIssuedAt(iat);
                    result.setNotBeforeTime(nbf);

                    if (accessToken instanceof AACOAuth2AccessToken) {
                        AACOAuth2AccessToken token = (AACOAuth2AccessToken) accessToken;
                        result.setSubject(token.getSubject());
                        result.setAuthorizedParty(token.getAuthorizedParty());

                        iat = (int) (token.getIssuedAt().getTime() / 1000);
                        nbf = (int) (token.getNotBeforeTime().getTime() / 1000);
                        result.setIssuedAt(iat);
                        result.setNotBeforeTime(nbf);

                        // add claims only if requesting client is in audience
                        String[] audience = token.getAudience();
                        if (ArrayUtils.contains(audience, introspectClientId)) {
                            result.setAudience(audience);
                            result.setJti(token.getToken());

                            result.setClaims(token.getClaims());

                        }
                    }

                }

            } else {
                throw new UnauthorizedClientException("client is not authorized");
            }

        } catch (Exception e) {
            // we catch every error to avoid discosing info on token existence
            logger.error("Error getting info for token: " + e.getMessage());
            result = new TokenIntrospection(false);
        }

        return ResponseEntity.ok(result);
    }

}
