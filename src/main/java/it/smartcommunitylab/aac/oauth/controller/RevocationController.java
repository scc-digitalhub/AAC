package it.smartcommunitylab.aac.oauth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.annotations.Api;

/**
 * OAuth2.0 Token Revocation controller as of RFC7009:
 * https://tools.ietf.org/html/rfc7009
 *
 */
@Controller
@Api(tags = { "AAC OAuth 2.0 Token Revocation (IETF RFC7009)" })
public class RevocationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ResourceServerTokenServices resourceServerTokenServices;

    @Autowired
    private TokenStore tokenStore;

//    
//    /**
//     * Revoke the access token and the associated refresh token.
//     * 
//     * @param token
//     */
//    @RequestMapping("/eauth/revoke/{token}")
//    public @ResponseBody String revokeToken(@PathVariable String token) {
//        
//        OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(token);        
//        OAuth2AccessToken storedToken = tokenStore.getAccessToken(auth);        
//        
//        OAuth2AccessToken accessTokenObj = tokenStore.readAccessToken(token);
//        if (accessTokenObj != null) {
//            if (accessTokenObj.getRefreshToken() != null) {
//                tokenStore.removeRefreshToken(accessTokenObj.getRefreshToken());
//            }
//            tokenStore.removeAccessToken(accessTokenObj);
//        }
//        return "";
//    }
//
    /**
     * Revoke the access token and the associated refresh token.
     * 
     * @param token
     */
    @RequestMapping(method = RequestMethod.POST, value = "/token_revoke")
    public ResponseEntity<String> revokeTokenWithParam(
            @RequestParam String token,
            @RequestParam(required = false) String token_type_hint) {
        try {
            OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(token);

            logger.trace("request revoke of token " + token);

            // check if client is authorized to remove this
            String clientId = auth.getOAuth2Request().getClientId();
            logger.trace("token clientId " + clientId);

            Authentication cauth = SecurityContextHolder.getContext().getAuthentication();
            logger.trace("client auth requesting revoke  " + String.valueOf(cauth.getName()));

            if (clientId.equals(cauth.getName())) {

                // check if token is refresh
                // TODO

                OAuth2AccessToken accessToken = tokenStore.getAccessToken(auth);
                if (accessToken.getRefreshToken() != null) {
                    logger.trace("remove refresh token for " + token);
                    tokenStore.removeRefreshToken(accessToken.getRefreshToken());
                }

                logger.trace("remove access token for " + token);
                tokenStore.removeAccessToken(accessToken);
            } else {
                throw new UnauthorizedClientException("client is not the owner of the token");
            }

            return ResponseEntity.ok("");

        } catch (Exception e) {
            logger.error("Error revoke for token: " + e.getMessage());
            // TODO map error response as per
            // https://tools.ietf.org/html/rfc7009#section-2.1
            return ResponseEntity.ok("");
        }

    }
}
