package it.smartcommunitylab.aac.oauth.request;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.ResponseType;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

public class OAuth2RequestValidator implements OAuth2TokenRequestValidator, OAuth2AuthorizationRequestValidator,
        OAuth2RegistrationRequestValidator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ScopeRegistry scopeRegistry;

    public void setScopeRegistry(ScopeRegistry scopeRegistry) {
        this.scopeRegistry = scopeRegistry;
    }

    @Override
    public void validate(TokenRequest tokenRequest, OAuth2ClientDetails clientDetails) throws InvalidRequestException {

        // check grant type and act accordingly
        String grantType = tokenRequest.getGrantType();
        AuthorizationGrantType authorizationGrantType = AuthorizationGrantType.parse(grantType);
        if (authorizationGrantType == AUTHORIZATION_CODE) {
            if (!tokenRequest.getScope().isEmpty()) {
                throw new InvalidRequestException(
                        "token request for " + grantType + " should not have scopes associated");
            }

            if (tokenRequest instanceof AuthorizationCodeTokenRequest) {
                AuthorizationCodeTokenRequest request = (AuthorizationCodeTokenRequest) tokenRequest;

                if (!StringUtils.hasText(request.getCode())) {
                    throw new InvalidRequestException("missing or empty code");
                }

                // require exact match for redirectUri to registered (when provided)
                // match with authorizationRequest will be checked in tokenGranter
                // TODO handle localhost properly
                String redirectUri = request.getRedirectUri();
                if (StringUtils.hasText(redirectUri)) {
                    validateRedirectUri(redirectUri, clientDetails.getRegisteredRedirectUri());
                }
            }
        }

        if (authorizationGrantType == IMPLICIT) {
            if (tokenRequest instanceof ImplicitTokenRequest) {
                ImplicitTokenRequest request = (ImplicitTokenRequest) tokenRequest;
                // require exact match for redirectUri to registered (when provided)
                // TODO handle localhost properly
                String redirectUri = request.getRedirectUri();
                if (StringUtils.hasText(redirectUri)) {
                    validateRedirectUri(redirectUri, clientDetails.getRegisteredRedirectUri());
                }
            }
        }

        if (authorizationGrantType == PASSWORD) {
            if (tokenRequest instanceof ResourceOwnerPasswordTokenRequest) {
                ResourceOwnerPasswordTokenRequest request = (ResourceOwnerPasswordTokenRequest) tokenRequest;
                String username = request.getUsername();
                if (!StringUtils.hasText(username)) {
                    throw new InvalidRequestException("missing or empty username");
                }
            }
        }

        if (authorizationGrantType == CLIENT_CREDENTIALS) {
            // TODO additional evaluate validation for client credentials
        }

        if (authorizationGrantType == REFRESH_TOKEN) {
            // scopes should be either empty or a subset of already authorized
            // validated in token granter

            // validate token
            if (tokenRequest instanceof RefreshTokenTokenRequest) {
                RefreshTokenTokenRequest request = (RefreshTokenTokenRequest) tokenRequest;
                String refreshToken = request.getRefreshToken();
                if (!StringUtils.hasText(refreshToken)) {
                    throw new InvalidRequestException("missing or empty refresh_token");

                }
            }
        }

    }

    @Override
    public void validateScope(TokenRequest tokenRequest, OAuth2ClientDetails client) throws InvalidScopeException {

        Set<String> requestScopes = tokenRequest.getScope();
        Set<String> clientScopes = client.getScope();
        boolean isClient = CLIENT_CREDENTIALS.getValue().equals(tokenRequest.getGrantType());

        if (requestScopes != null && !requestScopes.isEmpty()) {
            validateScope(requestScopes, clientScopes, isClient);
        }
    }

    @Override
    public void validate(AuthorizationRequest authorizationRequest, OAuth2ClientDetails clientDetails, User user)
            throws InvalidRequestException {

        Set<String> responseType = authorizationRequest.getResponseTypes();
        if (responseType.stream().anyMatch(r -> ResponseType.parse(r) == null)) {
            throw new InvalidRequestException("invalid response types");
        }

        Set<ResponseType> responseTypes = responseType.stream().map(r -> ResponseType.parse(r))
                .collect(Collectors.toSet());

        String responseMode = (String) authorizationRequest.getExtensions().get("response_mode");

        if (responseTypes.contains(ResponseType.ID_TOKEN) && !authorizationRequest.getScope().contains("openid")) {
            // openid is required to obtain an id token
            throw new InvalidRequestException("missing openid scope");
        }

        // ensure valid combinations only
        // TODO evaluate dedicated validator
        if (responseTypes.size() == 1 && responseTypes.contains(ResponseType.CODE)) {
            // authCode flow
        } else if (responseTypes.size() == 1 && responseTypes.contains(ResponseType.TOKEN)) {
            // implicit flow oauth2, no idtoken
        } else if (responseTypes.size() == 1 && responseTypes.contains(ResponseType.ID_TOKEN)) {
            // implicit flow only idToken
        } else if (responseTypes.contains(ResponseType.TOKEN) && responseTypes.contains(ResponseType.CODE)) {
            if (StringUtils.hasText(responseMode) && "query".equals(responseMode)) {
                throw new InvalidRequestException(
                        "query response_mode is incompatible with response_type token, use fragment or form_post");
            }
        } else if (responseTypes.size() > 1 && responseTypes.contains(ResponseType.ID_TOKEN)) {
            if (StringUtils.hasText(responseMode) && "query".equals(responseMode)) {
                throw new InvalidRequestException(
                        "query response_mode is incompatible with hybrid flow, use fragment or form_post");
            }
        }

        if (responseTypes.contains(ResponseType.ID_TOKEN)
                && !authorizationRequest.getExtensions().containsKey("nonce")) {
            throw new InvalidRequestException("nonce is required for implicit and hybrid flows");
        }

        // require exact match for redirectUri to registered (when provided)
        // TODO handle localhost properly
        String redirectUri = authorizationRequest.getRedirectUri();
        if (StringUtils.hasText(redirectUri)) {
            validateRedirectUri(redirectUri, clientDetails.getRegisteredRedirectUri());
        }

    }

    @Override
    public void validateScope(AuthorizationRequest authorizationRequest, OAuth2ClientDetails client)
            throws InvalidScopeException {

        Set<String> requestScopes = authorizationRequest.getScope();
        Set<String> clientScopes = client.getScope();

        if (requestScopes != null && !requestScopes.isEmpty()) {
            validateScope(requestScopes, clientScopes, false);
        }

    }

    @Override
    public void validate(ClientRegistrationRequest registrationRequest) throws InvalidRequestException {
        // no required fields from spec, check consistency of config
        ClientRegistration registration = registrationRequest.getRegistration();

        // check scopes
        if (registration.getScope() != null && scopeRegistry != null) {
            Set<String> invalidScopes = registration.getScope().stream().filter(s -> {
                Scope sc = scopeRegistry.findScope(s);
                return sc == null;
            }).collect(Collectors.toSet());

            if (!invalidScopes.isEmpty()) {
                throw new InvalidScopeException("Invalid scope: " + invalidScopes, Collections.emptySet());
            }
        }

        // check type
        if (registration.getGrantType() != null) {
            Set<String> grantType = registration.getGrantType();
            Set<AuthorizationGrantType> grantTypes = grantType.stream().map(r -> AuthorizationGrantType.parse(r))
                    .collect(Collectors.toSet());
            if (grantTypes.contains(null)) {
                throw new InvalidRequestException("unsupported grant_type");
            }

            if (grantTypes.contains(AUTHORIZATION_CODE) || grantTypes.contains(IMPLICIT)) {
                if (registration.getRedirectUris() == null || registration.getRedirectUris().isEmpty()) {
                    throw new InvalidRequestException("redirect_uri is mandatory");
                }
            }
        } else {
            throw new InvalidRequestException("at least one grant_type is required");

        }

        // check auth methods
        if (registration.getAuthenticationMethods() == null) {
            throw new InvalidRequestException("at least one authentication method is required");
        }

        if (!StringUtils.hasText(registration.getApplicationType())) {
            throw new InvalidRequestException("application type is required");
        }

        // require a valid name
        if (!StringUtils.hasText(registration.getName())) {
            throw new InvalidRequestException("client_name is required");
        }

    }

    private void validateRedirectUri(String redirectUri, Set<String> registeredRedirectUris) {
        // TODO handle as per
        // https://datatracker.ietf.org/doc/html/rfc6749#section-3.1.2.3
        // TODO handle localhost properly
        // TODO handle queryParam (permitted but not registered)
        // TODO handle fragments (not permitted)
        // TODO use a (rewritten) redirectResolver to consolidate logic
        if (!registeredRedirectUris.contains(redirectUri)) {
            throw new InvalidRequestException("invalid redirect_uri");
        }
    }

    private void validateScope(Set<String> requestScopes, Set<String> clientScopes, boolean isClient) {

        logger.trace("validate scopes requested " + String.valueOf(requestScopes.toString())
                + " against client " + String.valueOf(clientScopes.toString()));

        // check if scopes are valid via registry
        Set<String> existingScopes = (scopeRegistry == null) ? requestScopes
                : requestScopes.stream()
                        .filter(s -> (scopeRegistry.findScope(s) != null))
                        .collect(Collectors.toSet());

        Set<String> validScopes = (clientScopes != null ? clientScopes : Collections.emptySet());

        // each scope has to be pre-authorized
        Set<String> unauthorizedScopes = requestScopes.stream().filter(
                s -> (!validScopes.contains(s) || !existingScopes.contains(s)))
                .collect(Collectors.toSet());

        if (!unauthorizedScopes.isEmpty()) {
            String invalidScopes = String.join(" ", unauthorizedScopes);
            throw new InvalidScopeException("Invalid scope: " + invalidScopes, validScopes);
        }

        if (scopeRegistry != null) {
            // also check that type matches grant
            ScopeType type = isClient ? ScopeType.CLIENT : ScopeType.USER;
            Set<String> matchingScopes = clientScopes.stream().filter(
                    s -> {
                        Scope sc = scopeRegistry.findScope(s);
                        if (sc == null) {
                            return false;
                        }

                        return (sc.getType() == type || sc.getType() == ScopeType.GENERIC);
                    })
                    .collect(Collectors.toSet());

            Set<String> wrongScopes = requestScopes.stream().filter(
                    s -> (!matchingScopes.contains(s)))
                    .collect(Collectors.toSet());

            if (!wrongScopes.isEmpty()) {
                String invalidScopes = String.join(" ", wrongScopes);
                throw new InvalidScopeException("Invalid scope: " + invalidScopes, matchingScopes);
            }
        }

    }

    private AuthorizationGrantType AUTHORIZATION_CODE = AuthorizationGrantType.AUTHORIZATION_CODE;
    private AuthorizationGrantType IMPLICIT = AuthorizationGrantType.IMPLICIT;
    private AuthorizationGrantType CLIENT_CREDENTIALS = AuthorizationGrantType.CLIENT_CREDENTIALS;
    private AuthorizationGrantType PASSWORD = AuthorizationGrantType.PASSWORD;
    private AuthorizationGrantType REFRESH_TOKEN = AuthorizationGrantType.REFRESH_TOKEN;

}
