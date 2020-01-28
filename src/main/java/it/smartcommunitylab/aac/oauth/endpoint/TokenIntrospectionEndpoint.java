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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.dto.AACTokenIntrospection;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * OAuth2.0 Token introspection controller as of RFC7662:
 * https://tools.ietf.org/html/rfc7662.
 * 
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC OAuth 2.0 Token Introspection (IETF RFC7662)" })
public class TokenIntrospectionEndpoint {

    public final static String TOKEN_INTROSPECTION_URL = "/oauth/introspect";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ResourceServerTokenServices resourceServerTokenServices;

    @Autowired
    private ClientDetailsRepository clientDetailsRepository;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private UserManager userManager;

    @Autowired
    private RegistrationManager registrationManager;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${oauth2.introspection.permitAll}")
    private boolean permitAll;

    private JsonParser objectMapper = JsonParserFactory.create();

    // grant types bounded to user auth
    private final String[] userGrantTypes = {
            Config.GRANT_TYPE_AUTHORIZATION_CODE,
            Config.GRANT_TYPE_IMPLICIT,
            Config.GRANT_TYPE_PASSWORD
    };

    private final String[] applicationGrantTypes = {
            Config.GRANT_TYPE_CLIENT_CREDENTIALS
    };

    /*
     * TODO: evaluate if client_id should match audience, as per security
     * considerations https://tools.ietf.org/html/rfc7662#section-4
     */

    @ApiOperation(value = "Get token metadata")
    @RequestMapping(method = RequestMethod.POST, value = TOKEN_INTROSPECTION_URL)
    public ResponseEntity<AACTokenIntrospection> getTokenInfo(
            @RequestParam String token,
            @RequestParam(required = false, defaultValue = "access_token") String token_type_hint) {

        logger.debug("request introspection of token " + token + " hint " + String.valueOf(token_type_hint));

        // TODO drop DTO, leverage JWT structure to output a json
        AACTokenIntrospection result = new AACTokenIntrospection();

        try {
            OAuth2Authentication auth = null;
            OAuth2RefreshToken refreshToken = null;
            OAuth2AccessToken accessToken = null;

            // check hint
            if ("refresh_token".equals(token_type_hint)) {
                // load refresh token if present
                refreshToken = tokenStore.readRefreshToken(token);
                if (refreshToken != null) {
                    logger.trace("load auth for refresh token " + refreshToken.getValue());

                    // load authentication
                    auth = tokenStore.readAuthenticationForRefreshToken(refreshToken);
                }
            }

            if (auth == null) {
                // either token is access_token, or
                // as per spec, try as access_token independently of hint
                accessToken = tokenStore.readAccessToken(token);
                if (accessToken != null) {
                    logger.trace("load auth for access token " + accessToken.getValue());

                    // load authentication
                    auth = tokenStore.readAuthentication(accessToken);
                }

            }

            if (auth == null) {
                // not found
                throw new InvalidTokenException("no token for value " + token);
            }

            String clientId = auth.getOAuth2Request().getClientId();
            logger.trace("token clientId " + clientId);

            // load auth from context (basic auth or post if supported)
            Authentication cauth = SecurityContextHolder.getContext().getAuthentication();
            logger.trace("client auth requesting introspection  " + String.valueOf(cauth.getName()));

            if (permitAll || clientId.equals(cauth.getName())) {

                // if token exists is valid since revoked ones are not returned from tokenStore
                result.setActive(true);

                // only bearer tokens supported
                result.setToken_type(OAuth2AccessToken.BEARER_TYPE);

                // set base claims
                result.setClient_id(clientId);
                result.setScope(String.join(" ", auth.getOAuth2Request().getScope()));
                result.setIss(issuer);
                result.setAud(new String[] { clientId });

                // build response depending grant type
                String grantType = auth.getOAuth2Request().getGrantType();

                // fallback values, fetch from auth
                String userId = auth.getName();
                boolean applicationToken = false;
                String tenant = "";
                User user = null;

                if (ArrayUtils.contains(userGrantTypes, grantType)) {
                    // fetch user and populate claims
                    Object principal = auth.getPrincipal();

                    if (principal instanceof String) {
                        Registration reg = registrationManager.getUserByEmail((String) principal);
                        user = userManager.findOne(Long.parseLong(reg.getUserId()));

                    } else if (principal instanceof org.springframework.security.core.userdetails.User) {
                        org.springframework.security.core.userdetails.User u = (org.springframework.security.core.userdetails.User) principal;
                        user = userManager.findOne(Long.parseLong(u.getUsername()));
                    }

                }

                if (ArrayUtils.contains(applicationGrantTypes, grantType)) {
                    applicationToken = true;
                    // TODO evaluate if it makes sense
                    // fetch owner as user?
                    ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
                    applicationToken = true;
                    user = userManager.findOne(client.getDeveloperId());
                }

                if (user != null) {
                    userId = Long.toString(user.getId());

                    // fetch AAC tenant and userName
                    String internalName = userManager.getUserInternalName(user.getId());
                    String localName = internalName.substring(0, internalName.lastIndexOf('@'));
                    tenant = internalName.substring(internalName.lastIndexOf('@') + 1);

                    // TODO use userName instead of localName?
                    result.setUsername(localName);

                }

                result.setSub(userId);

                // additional AAC specific claims
                result.setAac_user_id(userId);
                result.setAac_grantType(grantType);
                result.setAac_applicationToken(applicationToken);
                result.setAac_am_tenant(tenant);

                if (refreshToken != null) {
                    // these make sense only on refresh tokens with expiration
                    if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
                        ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) refreshToken;
                        result.setExp((int) (expiringRefreshToken.getExpiration().getTime() / 1000));
                        //no iat in refreshToken..
                    }
                }

                if (accessToken != null) {
                    // these make sense only on access tokens
                    result.setExp((int) (accessToken.getExpiration().getTime() / 1000));
                    int iat = (int)(new Date().getTime() / 1000);
                    int nbf = iat;

                    if (accessToken instanceof AACOAuth2AccessToken) {
                        iat = (int)(((AACOAuth2AccessToken)accessToken).getIssuedAt().getTime() / 1000);
                        nbf = (int)(((AACOAuth2AccessToken)accessToken).getNotBeforeTime().getTime() / 1000);
                    } else {
                        iat = result.getExp() - accessToken.getExpiresIn();
                        nbf = iat;
                    }                 
                    
                    result.setIat(iat);
                    result.setNbf(nbf);
                    
                    // check if token is JWT then return jti
                    if (isJwt(accessToken.getValue())) {
                        // TODO remove extra check, maybe not needed
                        // check again expiration status
                        long now = new Date().getTime() / 1000;
                        long expiration = (accessToken.getExpiration().getTime() / 1000);
                        if (expiration < now) {
                            throw new InvalidTokenException("token expired");
                        }

                        // extract tokenId from jti field
                        Jwt old = JwtHelper.decode(accessToken.getValue());
                        Map<String, Object> claims = this.objectMapper.parseMap(old.getClaims());
                        result.setJti((String) claims.get("jti"));
                        // replace aud with JWT aud
                        if (claims.get("aud") instanceof String) {
                            result.setAud(new String[] { (String) claims.get("aud") });
                        } else {
                            result.setAud(((List<String>) claims.get("aud")).toArray(new String[0]));
                        }

                    }
                }

            } else {
                throw new UnauthorizedClientException("client is not the owner of the token");
            }
//
//            String userName = null;
//            String userId = null;
//            boolean applicationToken = false;
//
//            // WRONG reasoning, if auth is String for local users
//            // this will return client owner information
//            // need another way to check if client token
//            if (auth.getPrincipal() instanceof User) {
//                User principal = (User) auth.getPrincipal();
//                userId = principal.getUsername();
//            } else {
//                ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
//                applicationToken = true;
//                userId = "" + client.getDeveloperId();
//            }
//            userName = userManager.getUserInternalName(Long.parseLong(userId));
//            String localName = userName.substring(0, userName.lastIndexOf('@'));
//            String tenant = userName.substring(userName.lastIndexOf('@') + 1);
//
//            result.setUsername(localName);
//            result.setClient_id(clientId);
//            result.setScope(StringUtils.collectionToDelimitedString(auth.getOAuth2Request().getScope(), " "));
//            result.setExp((int) (storedToken.getExpiration().getTime() / 1000));
//            result.setIat(result.getExp() - storedToken.getExpiresIn());
//            result.setIss(issuer);
//            result.setNbf(result.getIat());
//            result.setSub(userId);
//            result.setAud(new String[] { clientId });
//
//            // check if token is JWT then return jti
//            if (isJwt(storedToken.getValue())) {
//                // check again expiration status
//                long now = new Date().getTime() / 1000;
//                long expiration = (storedToken.getExpiration().getTime() / 1000);
//                if (expiration < now) {
//                    throw new InvalidTokenException("token expired");
//                }
//
//                // extract tokenId from jti field
//                Jwt old = JwtHelper.decode(storedToken.getValue());
//                Map<String, Object> claims = this.objectMapper.parseMap(old.getClaims());
//                result.setJti((String) claims.get("jti"));
//                // replace aud with JWT aud
//                if (claims.get("aud") instanceof String) {
//                    result.setAud(new String[] { (String) claims.get("aud") });
//                } else {
//                    result.setAud(((List<String>) claims.get("aud")).toArray(new String[0]));
//                }
//
//            }
//
//            // if token exists is valid since revoked ones are deleted
//            result.setActive(true);
//
//            result.setAac_user_id(userId);
//            result.setAac_grantType(auth.getOAuth2Request().getGrantType());
//            result.setAac_applicationToken(applicationToken);
//            result.setAac_am_tenant(tenant);
        } catch (Exception e) {
            logger.error("Error getting info for token: " + e.getMessage());
            result = new AACTokenIntrospection();
            result.setActive(false);
        }
        return ResponseEntity.ok(result);
    }

    private boolean isJwt(String value) {
        // simply check for format header.body.signature
        int firstPeriod = value.indexOf('.');
        int lastPeriod = value.lastIndexOf('.');

        if (firstPeriod <= 0 || lastPeriod <= firstPeriod) {
            return false;
        } else {
            return true;
        }
    }

}
