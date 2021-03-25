package it.smartcommunitylab.aac.oauth.request;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.DefaultSecurityContextAuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.oauth.RealmAuthorizationRequest;
import it.smartcommunitylab.aac.oauth.RealmTokenRequest;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

public class AACOAuth2RequestFactory implements OAuth2RequestFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String NONCE = "nonce";

    private final OAuth2ClientDetailsService clientDetailsService;

    private AuthenticationHelper authenticationHelper = new DefaultSecurityContextAuthenticationHelper();

    public AACOAuth2RequestFactory(OAuth2ClientDetailsService clientDetailsService) {
        Assert.notNull(clientDetailsService, "client details service is mandatory");
        this.clientDetailsService = clientDetailsService;
    }

    public void setAuthenticationHelper(AuthenticationHelper authenticationHelper) {
        this.authenticationHelper = authenticationHelper;
    }

    /*
     * Create a realm authorization request
     * 
     * Clients can ask for matching realm or "common" (i.e. all realms), anything
     * else is prohibited
     * 
     */

    public RealmAuthorizationRequest createAuthorizationRequest(Map<String, String> authorizationParameters) {
        logger.trace(authorizationParameters.toString());
        String clientId = authorizationParameters.get(OAuth2Utils.CLIENT_ID);
        String state = authorizationParameters.get(OAuth2Utils.STATE);
        String redirectUri = authorizationParameters.get(OAuth2Utils.REDIRECT_URI);
        Set<String> responseTypes = OAuth2Utils.parseParameterList(
                decodeParameters(authorizationParameters.get(OAuth2Utils.RESPONSE_TYPE)));

        Set<String> scopes = extractScopes(
                OAuth2Utils.parseParameterList(
                        decodeParameters(authorizationParameters.get(OAuth2Utils.SCOPE))),
                clientId);

        String realm = authorizationParameters.get("realm");

        // we collect serviceIds as resourceIds to mark these as audience
        Set<String> resourceIds = OAuth2Utils.parseParameterList(
                decodeParameters(authorizationParameters.get("resource_id")));

        // workaround to support "id_token" requests
        // TODO fix with proper support in AuthorizationEndpoint
        if (responseTypes.contains(Config.RESPONSE_TYPE_ID_TOKEN)) {
            // ensure one of code or token is requested
            if (!responseTypes.contains(Config.RESPONSE_TYPE_CODE)
                    && !responseTypes.contains(Config.RESPONSE_TYPE_TOKEN)) {
                // treat it like an implicit flow call
                responseTypes.add(Config.RESPONSE_TYPE_TOKEN);
            }

        }

        logger.trace("create authorization request for " + clientId + " realm " + String.valueOf(realm)
                + " response " + responseTypes.toString()
                + " scope " + String.valueOf(authorizationParameters.get(OAuth2Utils.SCOPE))
                + " extracted scope " + scopes.toString()
                + " redirect " + redirectUri);

        // workaround to support "id_token" requests
        // TODO fix with proper support in AuthorizationEndpoint
        if (responseTypes.contains(Config.RESPONSE_TYPE_ID_TOKEN)) {
            // ensure one of code or token is requested
            if (!responseTypes.contains(Config.RESPONSE_TYPE_CODE)
                    && !responseTypes.contains(Config.RESPONSE_TYPE_TOKEN)) {
                // treat it like an implicit flow call
                responseTypes.add(Config.RESPONSE_TYPE_TOKEN);
            }

        }

        logger.trace("create authorization request for " + clientId + " realm " + String.valueOf(realm)
                + " response " + responseTypes.toString()
                + " scope " + String.valueOf(authorizationParameters.get(OAuth2Utils.SCOPE))
                + " extracted scope " + scopes.toString()
                + " redirect " + redirectUri);

        RealmAuthorizationRequest request = new RealmAuthorizationRequest(
                authorizationParameters, Collections.<String, String>emptyMap(),
                clientId, realm,
                scopes, resourceIds, null, false,
                state, redirectUri,
                responseTypes);

        if (authorizationParameters.containsKey(NONCE)) {
            request.getExtensions().put(NONCE, authorizationParameters.get(NONCE));
        }

        OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
        // store client authorities to be used consistently during request
        request.setAuthorities(clientDetails.getAuthorities());

        // load resourceIds from params, fallback if missing
        if (resourceIds.isEmpty()) {
            request.setResourceIds(clientDetails.getResourceIds());
        }

        // validate request for realm matching
        // TODO move to requestValidator, but authEndpoint calls only checkScopes for
        // now..
        if (!SystemKeys.REALM_COMMON.equals(realm) && !clientDetails.getRealm().equals(realm)) {
            throw new OAuth2AccessDeniedException();
        }

        // also validate that user has an identity in the asked realm
        UserAuthenticationToken userAuth = authenticationHelper.getUserAuthentication();
        if (userAuth == null) {
            // this should not happen
            throw new UnauthorizedUserException("missing user auth");
        }

        if (!SystemKeys.REALM_COMMON.equals(realm) && !userAuth.getRealm().equals(realm)) {
            // throw an error, we should really ask user to re-authenticate with the given
            // realm
            // by delegating to the authenticationEntryPoint
            // TODO
            throw new OAuth2AccessDeniedException();
        }

        return request;

    }

    public OAuth2Request createOAuth2Request(AuthorizationRequest request) {
        return request.createOAuth2Request();
    }

    public RealmTokenRequest createTokenRequest(Map<String, String> requestParameters,
            ClientDetails authenticatedClient) {

        String clientId = requestParameters.get(OAuth2Utils.CLIENT_ID);
        if (clientId == null) {
            // if the clientId wasn't passed in in the map, we add pull it from the
            // authenticated client object
            clientId = authenticatedClient.getClientId();
        } else {
            // otherwise, make sure that they match
            if (!clientId.equals(authenticatedClient.getClientId())) {
                throw new InvalidClientException("Given client ID does not match authenticated client");
            }
        }

        String grantType = requestParameters.get(OAuth2Utils.GRANT_TYPE);

        Set<String> scopes = new HashSet<>();
        // check grantType and act accordingly to parse scopes
        if (Config.GRANT_TYPE_PASSWORD.equals(grantType) ||
                Config.GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType) ||
                Config.GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
            scopes = extractScopes(OAuth2Utils.parseParameterList(
                    decodeParameters(requestParameters.get(OAuth2Utils.SCOPE))),
                    clientId);
        }

        String realm = requestParameters.get("realm");

        // we collect serviceIds as resourceIds to mark these as audience
        Set<String> resourceIds = OAuth2Utils.parseParameterList(
                decodeParameters(requestParameters.get("resource_id")));

        logger.trace("create token request for " + clientId + " realm " + String.valueOf(realm)
                + " grantType " + grantType
                + " scope " + String.valueOf(requestParameters.get(OAuth2Utils.SCOPE))
                + " extracted scope " + scopes.toString());

        RealmTokenRequest request = new RealmTokenRequest(requestParameters,
                clientId, realm,
                scopes, resourceIds,
                null,
                grantType);

        OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
        // store client authorities to be used consistently during request
        request.setAuthorities(clientDetails.getAuthorities());

        // load resourceIds from params, fallback if missing
        if (resourceIds.isEmpty()) {
            request.setResourceIds(clientDetails.getResourceIds());
        }

        // validate request for realm matching
        // TODO move to requestValidator, but tokenEndpoint calls only checkScopes for
        // now..
        if (!SystemKeys.REALM_COMMON.equals(realm) && !clientDetails.getRealm().equals(realm)) {
            throw new OAuth2AccessDeniedException();
        }

        return request;
    }

    public TokenRequest createTokenRequest(AuthorizationRequest authorizationRequest, String grantType) {
        TokenRequest tokenRequest = new TokenRequest(authorizationRequest.getRequestParameters(),
                authorizationRequest.getClientId(), authorizationRequest.getScope(), grantType);
        return tokenRequest;
    }

    public OAuth2Request createOAuth2Request(ClientDetails client, TokenRequest tokenRequest) {
        return tokenRequest.createOAuth2Request(client);
    }

    /*
     * Workaround for badly formatted scope param
     */
    private String decodeParameters(String value) {
        String result = value;
        if (StringUtils.hasText(result)) {

            // check if spaces are still encoded as %20
            if (result.contains("%20")) {
                // replace with spaces
                result = result.replace("%20", " ");
            }

            // check if strings are separated with comma (out of spec)
            if (result.contains(",")) {
                result = result.replace(",", " ");
            }
        }

        return result;
    }

    // TODO cleanup requestParameters and rework checkUserScopes
    private Set<String> extractScopes(Set<String> scopes, String clientId) {
        logger.trace("scopes from parameters " + scopes.toString());
        ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

        if ((scopes == null || scopes.isEmpty())) {
            // If no scopes are specified in the incoming data, use the default values
            // registered with the client
            // (the spec allows us to choose between this option and rejecting the request
            // completely, so we'll take the
            // least obnoxious choice as a default).
            scopes = clientDetails.getScope();
        }

        // TODO rework 2FA
//        boolean addStrongOperationScope = false;
//        if (scopes.contains(Config.SCOPE_OPERATION_CONFIRMED)) {
//            Object authDetails = SecurityContextHolder.getContext().getAuthentication().getDetails();
//            if (authDetails != null && authDetails instanceof AACOAuthRequest) {
//                if (((AACOAuthRequest) authDetails).isMobile2FactorConfirmed()) {
//                    addStrongOperationScope = true;
//                }
//                // clear for inappropriate access
//                ((AACOAuthRequest) authDetails).unsetMobile2FactorConfirmed();
//            }
//            if (!addStrongOperationScope) {
//                throw new InvalidScopeException("The operation.confirmed scope is not authorized by user");
//            }
//        }

        return scopes;
    }

}
