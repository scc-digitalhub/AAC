package it.smartcommunitylab.aac.oauth.endpoint;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.BadClientCredentialsException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.common.ServerErrorException;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.model.ClientRegistrationResponse;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.provider.ClientRegistrationServices;
import it.smartcommunitylab.aac.oauth.request.ClientRegistrationRequest;
import it.smartcommunitylab.aac.oauth.request.OAuth2RegistrationRequestFactory;
import it.smartcommunitylab.aac.oauth.request.OAuth2RegistrationRequestValidator;
import it.smartcommunitylab.aac.oauth.request.OAuth2TokenRequestFactory;
import it.smartcommunitylab.aac.oauth.scope.OAuth2DCRResource;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

/*
 * OAuth2/OIDC 
 * supports
 * 
 * https://datatracker.ietf.org/doc/html/rfc7591
 * https://datatracker.ietf.org/doc/html/rfc7592
 * https://openid.net/specs/openid-connect-registration-1_0.html
 */

@Controller
@Tag(name = "OAuth 2.0 Dynamic client registration")
public class ClientRegistrationEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String REGISTRATION_URL = "/oauth/register";

    @Autowired
    private OAuth2RegistrationRequestFactory oauth2RegistrationRequestFactory;

    @Autowired
    private OAuth2RegistrationRequestValidator oauth2RegistrationRequestValidator;

    @Autowired
    private OAuth2TokenRequestFactory oauth2TokenRequestFactory;

    @Autowired
    private ClientRegistrationServices registrationServices;

    @Autowired
    private RealmService realmService;

    @Autowired
    private OAuth2ClientDetailsService clientDetailsService;

    @Autowired
    private AuthorizationServerTokenServices tokenServices;

    @Autowired
    private TokenStore tokenStore;

    @Operation(summary = "Register a new client", parameters = {}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ClientRegistration.class)) }))
    @RequestMapping(value = REGISTRATION_URL, method = RequestMethod.POST)
    public ResponseEntity<ClientRegistrationResponse> registerClient(
            @RequestBody Map<String, Serializable> parameters,
            Authentication authentication, HttpServletRequest request)
            throws OAuth2Exception, SystemException {

        // we require realm to be set
        String realm = (String) parameters.get("realm");

        if (authentication instanceof BearerTokenAuthentication) {
            realm = (String) ((BearerTokenAuthentication) authentication).getTokenAttributes().get("realm");
        }

        try {

            if (!StringUtils.hasText(realm)) {
                throw new ClientRegistrationException("realm is required");
            }

            Realm r = realmService.findRealm(realm);
            if (r == null) {
                throw new ClientRegistrationException("realm is invalid");
            }

            // check if realm supports DCR and if DCR is open or protected
            boolean enabled = false;
            boolean approved = false;
            if (r.getOAuthConfiguration() != null) {
                enabled = r.getOAuthConfiguration().getEnableClientRegistration();

                if (r.getOAuthConfiguration().getOpenClientRegistration()) {
                    approved = true;
                } else {
                    // we need a valid auth, matching this realm with dcr scope
                    if (authentication instanceof BearerTokenAuthentication) {
                        String authRealm = (String) ((BearerTokenAuthentication) authentication).getTokenAttributes()
                                .get("realm");
                        if (!realm.equals(authRealm)) {
                            throw new BadClientCredentialsException();
                        }

                        // DISABLED, we don't have access to scopes via introspector
//                        if (!((BearerTokenAuthentication) authentication).getToken().getScopes()
//                                .contains(Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION)) {
//                            throw new InsufficientAuthenticationException("missing dcr scope");
//                        }

                        // check via authorities
                        if (authentication.getAuthorities().stream()
                                .noneMatch(a -> "SCOPE_dcr".equals(a.getAuthority()))) {
                            throw new InsufficientAuthenticationException("missing dcr scope");
                        }

                        // same realm, valid bearer with dcr scope
                        approved = true;
                    }
                }

            }

            if (!enabled) {
                throw new ClientRegistrationException("client registration is not available");
            }
            if (!approved) {
                throw new ClientRegistrationException("client registration requires a valid authentication");
            }

            // build via factory
            ClientRegistrationRequest registrationRequest = oauth2RegistrationRequestFactory
                    .createClientRegistrationRequest(parameters);

            // validate
            oauth2RegistrationRequestValidator.validate(registrationRequest);

            // register as new
            ClientRegistration registration = registrationServices.addRegistration(realm, registrationRequest);
            String clientId = registration.getClientId();
            OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

            // generate a non-expiring access token with proper auth and scope
            String registrationToken = null;

            if (registration.getClientSecret() != null) {
                // we use client credentials with single audience
                OAuth2AccessToken token = buildRegistrationToken(clientDetails);
                if (token != null) {
                    registrationToken = token.getValue();
                }

            }

            // build registration url for updates
            String registrationUrl = ServletUriComponentsBuilder.fromServletMapping(request)
                    .path(REGISTRATION_URL + "/{id}").build()
                    .expand(registration.getClientId()).encode().toUriString();

//            if (realmKey.isPresent()) {
//                // build a realm specific url
//                registrationUrl = ServletUriComponentsBuilder.fromServletMapping(request)
//                        .path("/-/{realm}" + REGISTRATION_URL + "/{id}").build()
//                        .expand(realm, registration.getClientId()).encode().toUriString();
//            }

            ClientRegistrationResponse response = new ClientRegistrationResponse(registration);
            response.setRegistrationUri(registrationUrl);
            response.setRegistrationToken(registrationToken);

            return ResponseEntity.ok(response);
        } catch (ClientRegistrationException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (OAuth2Exception e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.error("Exception " + e.getMessage());
            throw new ServerErrorException("Error", e);
        }

    }

    @Operation(summary = "Get an existing client")
    @RequestMapping(value = REGISTRATION_URL + "/{clientId}", method = RequestMethod.GET)
    public ResponseEntity<ClientRegistrationResponse> getClient(
            @PathVariable("clientId") @Valid @NotNull String clientId,
            BearerTokenAuthentication authentication, HttpServletRequest request)
            throws OAuth2Exception, SystemException {

        if (authentication == null) {
            throw new InsufficientAuthenticationException("authentication is required");
        }

        try {
            String realm = (String) authentication.getTokenAttributes().get("realm");
            if (realm == null) {
                throw new InvalidRequestException("missing or invalid realm");
            }

            Realm r = realmService.findRealm(realm);
            if (r == null) {
                throw new ClientRegistrationException("realm is invalid");
            }

            // check if realm supports DCR and if DCR is open or protected
            boolean enabled = false;
            boolean approved = false;
            if (r.getOAuthConfiguration() != null) {
                enabled = r.getOAuthConfiguration().getEnableClientRegistration();

                if (r.getOAuthConfiguration().getOpenClientRegistration()) {
                    approved = true;
                } else {
                    // we need a valid auth, matching this realm with dcr scope
                    String authRealm = (String) ((BearerTokenAuthentication) authentication).getTokenAttributes()
                            .get("realm");
                    if (!realm.equals(authRealm)) {
                        throw new BadClientCredentialsException();
                    }

                    if (!((BearerTokenAuthentication) authentication).getToken().getScopes()
                            .contains(Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION)) {
                        throw new InsufficientAuthenticationException("missing dcr scope");
                    }
                    // same realm, valid bearer with dcr scope
                    approved = true;
                }

            }

            if (!enabled) {
                throw new ClientRegistrationException("client registration is not available");
            }
            if (!approved) {
                throw new ClientRegistrationException("client registration requires a valid authentication");
            }

            // load client
            OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

            if (!clientDetails.getRealm().equals(realm)) {
                throw new InsufficientAuthenticationException("a valid authentication is required");
            }

            // validate token
            validateRegistrationToken(authentication, clientDetails);

            // load registration via service
            ClientRegistration registration = registrationServices.loadRegistrationByClientId(clientId);

            // TODO registration token renewal

            // build registration url for updates
            String registrationUrl = ServletUriComponentsBuilder.fromServletMapping(request)
                    .path(REGISTRATION_URL + "/{id}").build()
                    .expand(registration.getClientId()).encode().toUriString();

//            if (realmKey.isPresent()) {
//                // build a realm specific url
//                registrationUrl = ServletUriComponentsBuilder.fromServletMapping(request)
//                        .path("/-/{realm}" + REGISTRATION_URL + "/{id}").build()
//                        .expand(realm, registration.getClientId()).encode().toUriString();
//            }

            ClientRegistrationResponse response = new ClientRegistrationResponse(registration);
            response.setRegistrationUri(registrationUrl);
            response.setRegistrationToken(null);

            return ResponseEntity.ok(response);

        } catch (ClientRegistrationException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (OAuth2Exception e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.error("Exception " + e.getMessage());
            throw new ServerErrorException("Error", e);
        }
    }

    @Operation(summary = "Update an existing client", parameters = {}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ClientRegistration.class)) }))
    @RequestMapping(value = REGISTRATION_URL + "/{clientId}", method = RequestMethod.PUT)
    public ResponseEntity<ClientRegistrationResponse> updateClient(
            @PathVariable("clientId") @Valid @NotNull String clientId,
            @RequestBody Map<String, Serializable> parameters,
            BearerTokenAuthentication authentication, HttpServletRequest request)
            throws OAuth2Exception, SystemException {

        if (authentication == null) {
            throw new InsufficientAuthenticationException("authentication is required");
        }

        try {
            String realm = (String) authentication.getTokenAttributes().get("realm");
            if (realm == null) {
                throw new InvalidRequestException("missing or invalid realm");
            }

            Realm r = realmService.findRealm(realm);
            if (r == null) {
                throw new ClientRegistrationException("realm is invalid");
            }

            // check if realm supports DCR and if DCR is open or protected
            boolean enabled = false;
            boolean approved = false;
            if (r.getOAuthConfiguration() != null) {
                enabled = r.getOAuthConfiguration().getEnableClientRegistration();

                if (r.getOAuthConfiguration().getOpenClientRegistration()) {
                    approved = true;
                } else {
                    // we need a valid auth, matching this realm with dcr scope
                    String authRealm = (String) ((BearerTokenAuthentication) authentication).getTokenAttributes()
                            .get("realm");
                    if (!realm.equals(authRealm)) {
                        throw new BadClientCredentialsException();
                    }

                    if (!((BearerTokenAuthentication) authentication).getToken().getScopes()
                            .contains(Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION)) {
                        throw new InsufficientAuthenticationException("missing dcr scope");
                    }
                    // same realm, valid bearer with dcr scope
                    approved = true;
                }

            }

            if (!enabled) {
                throw new ClientRegistrationException("client registration is not available");
            }
            if (!approved) {
                throw new ClientRegistrationException("client registration requires a valid authentication");
            }

            // load client
            OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

            if (!clientDetails.getRealm().equals(realm)) {
                throw new InsufficientAuthenticationException("a valid authentication is required");
            }

            // validate token
            validateRegistrationToken(authentication, clientDetails);

            // build via factory
            ClientRegistrationRequest registrationRequest = oauth2RegistrationRequestFactory
                    .createClientRegistrationRequest(parameters);

            // validate
            oauth2RegistrationRequestValidator.validate(registrationRequest);

            // update registration
            ClientRegistration registration = registrationServices.updateRegistration(clientId, registrationRequest);

            // TODO registration token renewal

            // build registration url for updates
            String registrationUrl = ServletUriComponentsBuilder.fromServletMapping(request)
                    .path(REGISTRATION_URL + "/{id}").build()
                    .expand(registration.getClientId()).encode().toUriString();

//            if (realmKey.isPresent()) {
//                // build a realm specific url
//                registrationUrl = ServletUriComponentsBuilder.fromServletMapping(request)
//                        .path("/-/{realm}" + REGISTRATION_URL + "/{id}").build()
//                        .expand(realm, registration.getClientId()).encode().toUriString();
//            }

            ClientRegistrationResponse response = new ClientRegistrationResponse(registration);
            response.setRegistrationUri(registrationUrl);
            response.setRegistrationToken(null);

            return ResponseEntity.ok(response);

        } catch (ClientRegistrationException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (OAuth2Exception e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.error("Exception " + e.getMessage());
            throw new ServerErrorException("Error", e);
        }
    }

    @Operation(summary = "Delete a client")
    @RequestMapping(value = REGISTRATION_URL + "/{clientId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteClient(
            @PathVariable("clientId") @Valid @NotNull String clientId,
            BearerTokenAuthentication authentication, HttpServletRequest request)
            throws OAuth2Exception, SystemException {

        if (authentication == null) {
            throw new InsufficientAuthenticationException("authentication is required");
        }

        try {
            String realm = (String) authentication.getTokenAttributes().get("realm");
            if (realm == null) {
                throw new InvalidRequestException("missing or invalid realm");
            }

            Realm r = realmService.findRealm(realm);
            if (r == null) {
                throw new ClientRegistrationException("realm is invalid");
            }

            // check if realm supports DCR and if DCR is open or protected
            boolean enabled = false;
            boolean approved = false;
            if (r.getOAuthConfiguration() != null) {
                enabled = r.getOAuthConfiguration().getEnableClientRegistration();

                if (r.getOAuthConfiguration().getOpenClientRegistration()) {
                    approved = true;
                } else {
                    // we need a valid auth, matching this realm with dcr scope
                    String authRealm = (String) ((BearerTokenAuthentication) authentication).getTokenAttributes()
                            .get("realm");
                    if (!realm.equals(authRealm)) {
                        throw new BadClientCredentialsException();
                    }

                    if (!((BearerTokenAuthentication) authentication).getToken().getScopes()
                            .contains(Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION)) {
                        throw new InsufficientAuthenticationException("missing dcr scope");
                    }
                    // same realm, valid bearer with dcr scope
                    approved = true;
                }

            }

            if (!enabled) {
                throw new ClientRegistrationException("client registration is not available");
            }
            if (!approved) {
                throw new ClientRegistrationException("client registration requires a valid authentication");
            }

            // load client
            OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

            if (!clientDetails.getRealm().equals(realm)) {
                throw new InsufficientAuthenticationException("a valid authentication is required");
            }

            // validate token
            validateRegistrationToken(authentication, clientDetails);

            // delete registration via service
            registrationServices.removeRegistration(clientId);

            // remove token
            String tokenValue = authentication.getToken().getTokenValue();
            OAuth2AccessToken token = tokenStore.readAccessToken(tokenValue);
            if (token != null) {
                tokenStore.removeAccessToken(token);
            }

        } catch (ClientRegistrationException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        } catch (OAuth2Exception e) {
            logger.error("OAuth2 error " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.error("Exception " + e.getMessage());
            throw new ServerErrorException("Error", e);
        }
    }

    private OAuth2AccessToken buildRegistrationToken(OAuth2ClientDetails clientDetails) {
        // we use client credentials with single audience
        String grantType = AuthorizationGrantType.CLIENT_CREDENTIALS.getValue();
        Map<String, String> tokenParameters = new HashMap<>();
        tokenParameters.put("client_id", clientDetails.getClientId());
        tokenParameters.put("client_secret", clientDetails.getClientSecret());
        tokenParameters.put("grant_type", grantType);
        tokenParameters.put("scope", Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION);
        tokenParameters.put("resource", OAuth2DCRResource.RESOURCE_ID);

        TokenRequest tokenRequest = oauth2TokenRequestFactory.createTokenRequest(tokenParameters,
                clientDetails);
        OAuth2Request storedOAuth2Request = tokenRequest.createOAuth2Request(clientDetails);
        OAuth2Authentication tokenAuthentication = new OAuth2Authentication(storedOAuth2Request, null);

        // a token enhancer will modify the token to extend lifetime
        // TODO rework after proper redesign of tokenServices..
        OAuth2AccessToken token = tokenServices.createAccessToken(tokenAuthentication);
        return token;
    }

    private void validateRegistrationToken(BearerTokenAuthentication authentication,
            OAuth2ClientDetails clientDetails) {

        String clientId = clientDetails.getClientId();

        // validate token
        org.springframework.security.oauth2.core.OAuth2AccessToken token = authentication.getToken();
        String tokenValue = token.getTokenValue();

        // read from internal store
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
        if (accessToken == null || !(accessToken instanceof AACOAuth2AccessToken)) {
            throw new InsufficientAuthenticationException("a valid authentication is required");
        }

        AACOAuth2AccessToken registrationToken = (AACOAuth2AccessToken) accessToken;

        // audience
        List<String> audience = Arrays.asList(registrationToken.getAudience());
        if (!audience.contains(OAuth2DCRResource.RESOURCE_ID)) {
            throw new InsufficientAuthenticationException("a valid authentication is required");
        }

        // client id
        if (!registrationToken.getSubject().equals(clientId)) {
            throw new UnauthorizedClientException("client is not authorized");
        }

        // scope
        if (!accessToken.getScope().contains(Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION)
                || accessToken.getScope().size() != 1) {
            throw new InsufficientAuthenticationException("a valid authentication is required");
        }

        // expiration
        // TODO make sure registration tokens do not expire
        Date now = new Date();
        if (accessToken.getExpiration().before(now)) {
            throw new InsufficientAuthenticationException("a valid authentication is required");
        }
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
