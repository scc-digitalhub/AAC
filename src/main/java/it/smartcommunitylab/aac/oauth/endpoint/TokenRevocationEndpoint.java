package it.smartcommunitylab.aac.oauth.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.common.ServerErrorException;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OAuth2.0 Token Revocation controller as of RFC7009:
 * https://tools.ietf.org/html/rfc7009
 *
 */
@Controller
@Tag(name = "OAuth 2.0 Token Revocation")
public class TokenRevocationEndpoint {

    public static final String TOKEN_REVOCATION_URL = "/oauth/revoke";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ExtTokenStore tokenStore;

    /**
     * Revoke the access token and the associated refresh token.
     *
     * @param token
     */
    @Operation(summary = "Revoke token")
    @RequestMapping(method = RequestMethod.POST, value = TOKEN_REVOCATION_URL)
    public ResponseEntity<String> revokeTokenWithParam(
        @RequestParam(name = "token") String token,
        @RequestParam(required = false, name = "token_type_hint") Optional<String> tokenTypeHint,
        Authentication authentication
    ) {
        if (!(authentication instanceof OAuth2ClientAuthenticationToken) || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Invalid client authentication");
        }

        logger.debug(
            "request revoke of token {} hint {}",
            StringUtils.trimAllWhitespace(token),
            StringUtils.trimAllWhitespace(String.valueOf(tokenTypeHint))
        );

        OAuth2ClientAuthenticationToken clientAuth = (OAuth2ClientAuthenticationToken) authentication;

        // load token from store
        OAuth2Authentication auth = null;
        OAuth2RefreshToken refreshToken = null;
        OAuth2AccessToken accessToken = null;

        // check hint
        // as per spec this is a suggestion, if we don't find the token we need to
        // extend the search to all token types
        // as such we ignore hint for now, we can distinguish tokens by looking them up

        // load refresh token if present
        refreshToken = tokenStore.readRefreshToken(token);
        if (refreshToken != null) {
            logger.trace("load auth for refresh token " + refreshToken.getValue());

            // load authentication
            auth = tokenStore.readAuthenticationForRefreshToken(refreshToken);
        }

        if (auth == null) {
            // as per spec, try as access_token independently of hint
            accessToken = tokenStore.readAccessToken(token);
            if (accessToken != null) {
                logger.trace("load auth for access token " + accessToken.getValue());

                // load authentication
                auth = tokenStore.readAuthentication(accessToken);
            }
        }

        if (auth == null) {
            // not token found
            // as per spec, return 200 since the token is invalid
            return ResponseEntity.ok("");
        }

        // check if client is authorized to remove this
        String clientId = auth.getOAuth2Request().getClientId();
        logger.trace("token clientId " + clientId);

        // load auth from context (basic auth or post if supported)
        logger.trace("client auth requesting revoke  " + String.valueOf(clientAuth.getClientId()));

        if (clientId.equals(clientAuth.getClientId())) {
            // remove
            // TODO rewrite to handle a revocation status or
            // move to a different store, we want to keep revoked tokens around
            // for a while to discover usage etc
            if (refreshToken != null) {
                logger.trace("remove refresh token for " + refreshToken.getValue());
                tokenStore.removeRefreshToken(refreshToken);

                // as per spec SHOULD also remove access tokens binded to refresh
                accessToken = tokenStore.readAccessTokenForRefreshToken(refreshToken.getValue());
            }

            if (accessToken != null) {
                // DISABLED removal of refresh for access
                // spec says we MAY remove these
                // TODO add a config flag
                //                    // check if refresh embedded
                //                    if (accessToken.getRefreshToken() != null) {
                //                        logger.trace("remove refresh token for " + accessToken.getRefreshToken().getValue());
                //                        tokenStore.removeRefreshToken(accessToken.getRefreshToken());
                //                    }
                logger.trace("remove access token for " + accessToken.getValue());
                tokenStore.removeAccessToken(accessToken);
            }
        } else {
            throw new UnauthorizedClientException("client is not the owner of the token");
        }

        return ResponseEntity.ok("");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<OAuth2Exception> handleAuthenticationException(AuthenticationException e) throws Exception {
        ResponseEntity<OAuth2Exception> response = buildResponse(new BadClientCredentialsException());
        // handle 401 as per https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
        // TODO respond with header matching authentication scheme used by client

        return response;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OAuth2Exception> handleException(Exception e) throws Exception {
        return buildResponse(new ServerErrorException(e.getMessage(), e));
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<OAuth2Exception> handleOAuth2Exception(OAuth2Exception e) throws Exception {
        return buildResponse(e);
    }

    private ResponseEntity<OAuth2Exception> buildResponse(OAuth2Exception e) throws IOException {
        logger.error("Error: " + e.getMessage());

        HttpStatus status = HttpStatus.valueOf(e.getHttpErrorCode());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");

        // exceptions have a predefined message
        ResponseEntity<OAuth2Exception> response = new ResponseEntity<OAuth2Exception>(e, headers, status);
        return response;
    }
}
