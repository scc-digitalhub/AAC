package it.smartcommunitylab.aac.oauth.endpoint;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.common.exceptions.UnsupportedGrantTypeException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.common.ServerErrorException;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.TokenResponse;
import it.smartcommunitylab.aac.oauth.request.OAuth2TokenRequestFactory;
import it.smartcommunitylab.aac.oauth.request.OAuth2TokenRequestValidator;
import it.smartcommunitylab.aac.openid.common.IdToken;
import it.smartcommunitylab.aac.openid.token.IdTokenServices;

@Controller
@Tag(name = "OAuth 2.0 Token Endpoint")
public class TokenEndpoint implements InitializingBean {

    public static final String TOKEN_URL = "/oauth/token";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TokenGranter tokenGranter;

    @Autowired
    private OAuth2TokenRequestFactory oauth2RequestFactory;

    @Autowired
    private OAuth2TokenRequestValidator oauth2RequestValidator;

    @Autowired
    private IdTokenServices idTokenServices;

    @Autowired
    private ResourceServerTokenServices tokenServices;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(tokenGranter, "token granter is required");
        Assert.notNull(oauth2RequestFactory, "token request factory is required");
        Assert.notNull(oauth2RequestValidator, "token request validator is required");
    }

    @Operation(summary = "Get token", parameters = {}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = it.smartcommunitylab.aac.oauth.request.TokenRequest.class)) }))
    @RequestMapping(value = { TOKEN_URL }, method = RequestMethod.POST)
    public ResponseEntity<TokenResponse> postAccessToken(
            @RequestParam Map<String, String> parameters,
            Authentication authentication)
            throws ClientRegistrationException, OAuth2Exception, SystemException {

        if (!(authentication instanceof OAuth2ClientAuthenticationToken) || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Invalid client authentication");
        }

        OAuth2ClientAuthenticationToken clientAuth = (OAuth2ClientAuthenticationToken) authentication;
        String clientId = clientAuth.getClientId();
        OAuth2ClientDetails clientDetails = clientAuth.getOAuth2ClientDetails();

        // fetch tokenRequest via factory
        TokenRequest tokenRequest = oauth2RequestFactory.createTokenRequest(parameters, clientDetails);
        if (tokenRequest == null) {
            throw new InvalidRequestException("Invalid request");
        }

        // validate match between authentication and client request
        // note: refresh_token requests must additionally check that token was issued to
        // the same client, will check in granter
        if (!clientId.equals(tokenRequest.getClientId())) {
            throw new InvalidClientException("Client id does not match authentication");
        }

//        // validate realm accessibility for client
//        if (!realm.equals(clientDetails.getRealm()) && !realm.equals(SystemKeys.REALM_COMMON)) {
//            throw new UnauthorizedClientException("client is not authorized for realm " + realm);
//        }

        // check grantType
        AuthorizationGrantType authorizationGrantType = AuthorizationGrantType.parse(tokenRequest.getGrantType());
        if (authorizationGrantType == null) {
            throw new InvalidRequestException("Missing or invalid grant type");
        }

        // implicit flow can't ask for tokens
        if (authorizationGrantType == AuthorizationGrantType.IMPLICIT) {
            throw new InvalidRequestException("Implicit grant can't get tokens from token endpoint");
        }

        // check if client is enabled for this grant
        String grantType = authorizationGrantType.getValue();
        if (!clientDetails.getAuthorizedGrantTypes().contains(grantType)) {
            throw new UnauthorizedClientException("client is not authorized for flow " + grantType);
        }

        // validate scopes
        oauth2RequestValidator.validateScope(tokenRequest, clientDetails);

        // validate request via validator
        oauth2RequestValidator.validate(tokenRequest, clientDetails);

        // get token from granter, if flow is unsupported result will be null
        OAuth2AccessToken token = tokenGranter.grant(grantType, tokenRequest);
        if (token == null) {
            throw new UnsupportedGrantTypeException("Grant type not supported: " + grantType);
        }

        IdToken idToken = null;
        // TODO check scopes in tokenrequest, but we need to fetch authorizationRequest
        // via factory
        // otherwise for authcode scope will be empty
        if (authorizationGrantType == AuthorizationGrantType.AUTHORIZATION_CODE
                && token.getScope().contains("openid")) {
            // we build id token independently of response_type
            // TODO make sure idToken is a match for the one returned in
            // authorizationResponse, except at_hash
            // note that for code id_token token response we will build 2 idtoken bounded to
            // 2 different accesstokens, spec is not clear...

            // read back authentication used for token
            // TODO rewrite from scratch tokenGranter interfaces, tokenServices should call
            // them not the other way around..
            OAuth2Authentication oauth2Authentication = tokenServices.loadAuthentication(token.getValue());

            idToken = idTokenServices.createIdToken(oauth2Authentication, token);

        }

        // invalidate session now
        // this should be meaningless since we expect this endpoint to live under a
        // sessionless context
        SecurityContextHolder.getContext().setAuthentication(null);

        return buildResponse(token, idToken);
    }

    private ResponseEntity<TokenResponse> buildResponse(OAuth2AccessToken accessToken, IdToken idToken) {
        // build a proper response, as per rfc6749
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("Content-Type", "application/json;charset=UTF-8");

        TokenResponse response = new TokenResponse(accessToken);
        if (idToken != null) {
            response.setIdToken(idToken.getValue());
        }

        // enforce offline_access scope for refresh_token
        Set<String> scopes = accessToken.getScope();
        if (!scopes.contains(Config.SCOPE_OFFLINE_ACCESS) && response.getRefreshToken() != null) {
            response.setRefreshToken(null);
        }

        return new ResponseEntity<TokenResponse>(response, headers, HttpStatus.OK);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<OAuth2Exception> handleAccessDeniedException(AccessDeniedException e) throws Exception {
        return buildResponse(new UnauthorizedClientException("client is not authorized"));
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
        if (logger.isTraceEnabled()) {
            e.printStackTrace();
        }

        HttpStatus status = HttpStatus.valueOf(e.getHttpErrorCode());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");

        // exceptions have a predefined message
        ResponseEntity<OAuth2Exception> response = new ResponseEntity<OAuth2Exception>(e, headers, status);
        return response;

    }

}
