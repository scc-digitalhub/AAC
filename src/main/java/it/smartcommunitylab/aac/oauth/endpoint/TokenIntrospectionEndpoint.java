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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.common.ServerErrorException;
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
@Tag(name = "OAuth 2.0 Token Introspection" )
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
     * client_id should match audience, as per security considerations
     * https://tools.ietf.org/html/rfc7662#section-4
     */

//    @SuppressWarnings("unchecked")
    @Operation(summary = "Get token metadata")
    @RequestMapping(method = RequestMethod.POST, value = TOKEN_INTROSPECTION_URL)
    public ResponseEntity<TokenIntrospection> getTokenInfo(
            @RequestParam(name = "token") String tokenValue,
            @RequestParam(required = false, name = "token_type_hint") Optional<String> tokenTypeHint,
            Authentication authentication) {

        logger.debug("request introspection of token " + tokenValue + " hint " + String.valueOf(tokenTypeHint));

        if (!(authentication instanceof OAuth2ClientAuthenticationToken) || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException(
                    "Invalid client authentication");
        }

        // clientAuth should be available
        OAuth2ClientAuthenticationToken introspectClientAuth = (OAuth2ClientAuthenticationToken) authentication;
        OAuth2ClientDetails introspectClientDetails = introspectClientAuth.getOAuth2ClientDetails();
        String introspectClientId = introspectClientDetails.getClientId();

        if (!StringUtils.hasText(tokenValue)) {
            throw new InvalidRequestException("missing or invalid token");
        }

        // check hint
        if (tokenTypeHint.isPresent()) {
            String typeHint = tokenTypeHint.get();
            if (!"access_token".equals(typeHint) && !"refresh_token".equals(typeHint)) {
                // unsupported token type, drop
                throw new InvalidRequestException("invalid token type hint");
            }
        }
        // as per spec this is a suggestion, if we don't find the token we need to
        // extend the search to all token types
        // as such we ignore hint for now, we can distinguish tokens by looking them up

        // load refresh token if present
        OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(tokenValue);
        if (refreshToken != null) {
            logger.trace("load auth for refresh token " + refreshToken.getValue());

            // load authentication
            OAuth2Authentication auth = tokenStore.readAuthenticationForRefreshToken(refreshToken);

            TokenIntrospection result = introspectRefreshToken(introspectClientId, auth, refreshToken);
            return ResponseEntity.ok(result);
        }

        // as per spec, try as access_token independently of hint
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
        if (accessToken != null) {
            logger.trace("load auth for access token " + accessToken.getValue());

            // load authentication
            OAuth2Authentication auth = tokenStore.readAuthentication(accessToken);

            TokenIntrospection result = introspectAccessToken(introspectClientId, auth, accessToken);
            return ResponseEntity.ok(result);
        }

        // no token found
        // as per spec return a response with active=false
        return ResponseEntity.ok(new TokenIntrospection(false));

    }

    private TokenIntrospection introspectAccessToken(String introspectClientId,
            OAuth2Authentication auth, OAuth2AccessToken accessToken) {

        if (accessToken == null || auth == null) {
            throw new InvalidTokenException("invalid access token");
        }
        Date now = new Date();
        if (accessToken.getExpiration().before(now)) {
            throw new InvalidTokenException("access token expired");
        }
        // TODO add check for notBefore

        // extract request
        OAuth2Request request = auth.getOAuth2Request();
        String clientId = request.getClientId();
        List<String> audience = Collections.singletonList(clientId);
        if (accessToken instanceof AACOAuth2AccessToken) {
            String[] aud = ((AACOAuth2AccessToken) accessToken).getAudience();
            audience = Arrays.asList(aud);
        }

        logger.trace("token clientId " + clientId);
        logger.trace("client auth requesting introspection  " + introspectClientId);

        // spec-compliant: check client is either azp or in audience
        // "If the token can be used only at certain resource servers, the
        // authorization server MUST determine whether or not the token can
        // be used at the resource server making the introspection call"

        // TODO remove permitAll config
        if (!permitAll && !clientId.equals(introspectClientId) && !audience.contains(introspectClientId)) {
            throw new UnauthorizedClientException("client is not a valid audience");
        }

        // if token exists here is valid since revoked ones are not returned from
        // tokenStore
        TokenIntrospection result = new TokenIntrospection(true);

        // add base response
        result.setIssuer(issuer);
        result.setClientId(clientId);

        // add only if requesting client is in audience
        // TODO remove when we clear permitAll shortcut
        if (audience.contains(introspectClientId)) {
            result.setAudience(audience);
        }

        Set<String> scopes = request.getScope();
        result.setScope(scopes);

        // TODO read from accessToken when we introduce proper support
        result.setTokenType("bearer");

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
            // TODO remove when we clear permitAll shortcut
            if (audience.contains(introspectClientId)) {
                result.setJti(token.getToken());
                result.setClaims(token.getClaims());
            }
        }

        return result;

    }

    private TokenIntrospection introspectRefreshToken(String introspectClientId,
            OAuth2Authentication auth, OAuth2RefreshToken refreshToken) {

        if (refreshToken == null || auth == null) {
            throw new InvalidTokenException("invalid refresh token");
        }

        Date now = new Date();
        if (refreshToken instanceof ExpiringOAuth2RefreshToken
                && ((ExpiringOAuth2RefreshToken) refreshToken).getExpiration().before(now)) {
            throw new InvalidTokenException("refresh token expired");
        }

        // extract request
        OAuth2Request request = auth.getOAuth2Request();
        String clientId = request.getClientId();

        logger.trace("token clientId " + clientId);
        logger.trace("client auth requesting introspection  " + introspectClientId);

        // spec-compliant: refresh token are valid only for azp
        if (!clientId.equals(introspectClientId)) {
            throw new UnauthorizedClientException("client is not the owner");
        }

        // if token exists here is valid since revoked ones are not returned from
        // tokenStore
        TokenIntrospection result = new TokenIntrospection(true);

        // add base response
        result.setIssuer(issuer);
        result.setClientId(clientId);

        // client is the only audience
        result.setAudience(clientId);

        // we add scopes to indicate which are authorized
        Set<String> scopes = request.getScope();
        result.setScope(scopes);

        // these make sense only on refresh tokens with expiration
        if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
            ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) refreshToken;
            result.setExpirationTime((int) (expiringRefreshToken.getExpiration().getTime() / 1000));
            // no iat in refreshToken, we should add..
        }

        // we should be able to fetch user auth since refresh_token are user only
        if (auth.getUserAuthentication() != null) {
            result.setSubject(auth.getUserAuthentication().getName());
        }

        return result;

    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<TokenIntrospection> handleInvalidTokenException(InvalidTokenException e) throws Exception {
        logger.error("Invalid token: " + e.getMessage());
        return ResponseEntity.ok(new TokenIntrospection(false));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<TokenIntrospection> handleAuthenticationException(AuthenticationException e) throws Exception {
        ResponseEntity<TokenIntrospection> response = buildResponse(new BadClientCredentialsException());
        // handle 401 as per https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
        // TODO respond with header matching authentication scheme used by client

        return response;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TokenIntrospection> handleException(Exception e) throws Exception {
        return buildResponse(new ServerErrorException(e.getMessage(), e));
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<TokenIntrospection> handleOAuth2Exception(OAuth2Exception e) throws Exception {
        logger.error("Invalid token: " + e.getMessage());
        return buildResponse(e);
    }

    private ResponseEntity<TokenIntrospection> buildResponse(OAuth2Exception e) throws IOException {
        logger.error("Error: " + e.getMessage());

        // don't leak error, token is invalid
        return ResponseEntity.ok(new TokenIntrospection(false));
    }

}
