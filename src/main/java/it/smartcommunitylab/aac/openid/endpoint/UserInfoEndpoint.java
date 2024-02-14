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

package it.smartcommunitylab.aac.openid.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import it.smartcommunitylab.aac.oauth.token.ClaimsTokenEnhancer;
import it.smartcommunitylab.aac.users.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.ClientAuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InsufficientScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author raman
 *
 */
@Controller
@Tag(name = "OpenID Connect Core")
public class UserInfoEndpoint {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String USERINFO_URL = "/userinfo";

    public static final String JOSE_MEDIA_TYPE_VALUE = "application/jwt";
    public static final MediaType JOSE_MEDIA_TYPE = new MediaType("application", "jwt");

    @Autowired
    private UserService userService;

    @Autowired
    private ClientDetailsService clientService;

    @Autowired
    private OAuth2ClientDetailsService oauth2ClientService;

    @Autowired
    private ExtTokenStore tokenStore;

    @Autowired
    private ClaimsTokenEnhancer claimsEnhancer;

    /**
     * Get information about the user as specified in the accessToken included in
     * this request
     */
    //	@PreAuthorize("hasRole('ROLE_USER') and #oauth2.hasScope('" + Config.OPENID_SCOPE + "')")
    @Operation(summary = "Get info about the authenticated End-User")
    @RequestMapping(
        value = USERINFO_URL,
        method = { RequestMethod.GET, RequestMethod.POST },
        produces = { MediaType.APPLICATION_JSON_VALUE, JOSE_MEDIA_TYPE_VALUE }
    )
    public @ResponseBody Map<String, Object> getUserInfo(
        @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader,
        BearerTokenAuthentication auth
    ) throws NoSuchUserException, NoSuchRealmException, ClientRegistrationException, NoSuchClientException {
        if (auth == null) {
            logger.error("getInfo failed; no principal. Requester is not authorized.");
            throw new InvalidTokenException("invalid_token");
        }

        // fetch token from store
        OAuth2AccessToken token = tokenStore.readAccessToken(auth.getToken().getTokenValue());
        if (token == null) {
            throw new InvalidTokenException("invalid_token");
        }

        // check if openid scope is in request
        if (!token.getScope().contains(Config.SCOPE_OPENID)) {
            throw new InsufficientScopeException("insufficient_scope");
        }

        OAuth2Authentication oauthAuth = tokenStore.readAuthentication(token);
        if (oauthAuth == null) {
            throw new InvalidRequestException("invalid_request");
        }

        // TODO refresh authentication to update userDetails + authorities etc.

        String clientId = oauthAuth.getOAuth2Request().getClientId();
        ClientDetails clientDetails = clientService.loadClient(clientId);

        // principal name is userid for user tokens
        // we don't know yet if this is a client token...
        String subjectId = auth.getName();
        String realm = clientDetails.getRealm();

        // fetch user, translated for client realm
        User user = userService.getUser(subjectId, realm);

        // content negotiation
        boolean useJwt = false;
        // start off by seeing if the client has registered for a signed/encrypted JWT
        // from here
        OAuth2ClientDetails oauth2ClientDetails = oauth2ClientService.loadClientByClientId(clientId);

        String signedResponseAlg = oauth2ClientDetails.getJwtSignAlgorithm();
        String encResponseAlg = oauth2ClientDetails.getJwtEncAlgorithm();
        String encResponseEnc = oauth2ClientDetails.getJwtEncMethod();

        List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
        MediaType.sortBySpecificityAndQuality(mediaTypes);

        // TODO rework
        if (
            StringUtils.hasText(signedResponseAlg) ||
            (StringUtils.hasText(encResponseAlg)) &&
            StringUtils.hasText(encResponseEnc)
        ) {
            // client has a preference, see if they ask for plain JSON specifically on this
            // request
            for (MediaType m : mediaTypes) {
                if (!m.isWildcardType() && m.isCompatibleWith(JOSE_MEDIA_TYPE)) {
                    useJwt = true;
                } else if (!m.isWildcardType() && m.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                    useJwt = false;
                }
            }

            // otherwise return JWT
            useJwt = true;
        } else {
            // client has no preference, see if they asked for JWT specifically on this
            // request
            for (MediaType m : mediaTypes) {
                if (!m.isWildcardType() && m.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                    useJwt = false;
                } else if (!m.isWildcardType() && m.isCompatibleWith(JOSE_MEDIA_TYPE)) {
                    useJwt = true;
                }
            }

            // otherwise return JSON
            useJwt = false;
        }

        // build response
        Map<String, Object> userInfo = new HashMap<>();

        // we rebuild all claims
        AACOAuth2AccessToken accessToken = claimsEnhancer.enhance(token, oauthAuth);

        userInfo.putAll(accessToken.getClaims());

        // TODO handle jwt/jwe response types

        return userInfo;
    }

    @ExceptionHandler(ClientAuthenticationException.class)
    public ResponseEntity<Object> handleClientAuthenticationException(ClientAuthenticationException exception) {
        // build message with handling for WWW header
        StringBuilder msg = new StringBuilder();

        msg.append("error=" + exception.getOAuth2ErrorCode());
        if (exception.getMessage() != null) {
            msg.append(",error_description=" + exception.getMessage());
        }

        return ResponseEntity
            .status(exception.getHttpErrorCode())
            .header(HttpHeaders.WWW_AUTHENTICATE, msg.toString())
            .build();
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<Object> handleOAuth2Exception(OAuth2Exception exception) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", exception.getOAuth2ErrorCode());
        response.put("error_description", exception.getMessage());

        return ResponseEntity.status(exception.getHttpErrorCode()).body(response);
    }
}
