package it.smartcommunitylab.aac.oauth.request;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.DefaultSecurityContextAuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.RealmAuthorizationRequest;
import it.smartcommunitylab.aac.oauth.RealmTokenRequest;
import it.smartcommunitylab.aac.oauth.flow.FlowExtensionsService;
import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensions;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

//TODO add support for audience in request
public class AACOAuth2RequestFactory implements OAuth2RequestFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String NONCE = "nonce";

    private final OAuth2ClientDetailsService clientDetailsService;
    private FlowExtensionsService flowExtensionsService;
    private UserService userService;
    private ScopeRegistry scopeRegistry;

    private AuthenticationHelper authenticationHelper = new DefaultSecurityContextAuthenticationHelper();

    public AACOAuth2RequestFactory(OAuth2ClientDetailsService clientDetailsService) {
        Assert.notNull(clientDetailsService, "client details service is mandatory");
        this.clientDetailsService = clientDetailsService;
    }

    public void setAuthenticationHelper(AuthenticationHelper authenticationHelper) {
        this.authenticationHelper = authenticationHelper;
    }

    public void setScopeRegistry(ScopeRegistry scopeRegistry) {
        this.scopeRegistry = scopeRegistry;
    }

    public void setFlowExtensionsService(FlowExtensionsService flowExtensionsService) {
        this.flowExtensionsService = flowExtensionsService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
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

        // fetch base params
        String clientId = authorizationParameters.get(OAuth2Utils.CLIENT_ID);
        String state = authorizationParameters.get(OAuth2Utils.STATE);
        Set<String> responseTypes = OAuth2Utils.parseParameterList(
                decodeParameters(authorizationParameters.get(OAuth2Utils.RESPONSE_TYPE)));
        String realm = authorizationParameters.get("realm");

        try {
            OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

            // also validate that user has an identity in the asked realm
            UserAuthentication userAuth = authenticationHelper.getUserAuthentication();
            if (userAuth == null) {
                // this should not happen
                throw new UnauthorizedUserException("missing user auth");
            }

//            // check extensions and integrate modifications
//            if (flowExtensionsService != null) {
//                OAuthFlowExtensions ext = flowExtensionsService.getOAuthFlowExtensions(clientDetails);
//                if (ext != null) {
//                    User user = new User(userAuth.getUser());
//                    if (userService != null) {
//                        user = userService.getUser(userAuth.getUser(), clientDetails.getRealm());
//                    }
//
//                    Map<String, String> parameters = ext.onBeforeUserApproval(authorizationParameters, user,
//                            clientDetails);
//                    if (parameters != null) {
//                        // merge parameters into request params
//                        authorizationParameters.putAll(parameters);
//                        // enforce base params consistency
//                        // TODO rewrite with proper merge with exclusion list
//                        authorizationParameters.put(OAuth2Utils.CLIENT_ID, clientId);
//                        authorizationParameters.put(OAuth2Utils.STATE, state);
//                        authorizationParameters.put(OAuth2Utils.RESPONSE_TYPE,
//                                OAuth2Utils.formatParameterList(responseTypes));
//                        authorizationParameters.put("realm", realm);
//                    }
//                }
//            }

            String redirectUri = authorizationParameters.get(OAuth2Utils.REDIRECT_URI);
            Set<String> scopes = extractScopes(
                    OAuth2Utils.parseParameterList(
                            decodeParameters(authorizationParameters.get(OAuth2Utils.SCOPE))),
                    clientDetails.getScope(), false);

            // we collect serviceIds as resourceIds to mark these as audience
            Set<String> resourceIds = new HashSet<>();
            resourceIds.addAll(OAuth2Utils.parseParameterList(
                    decodeParameters(authorizationParameters.get("resource"))));

            // also load resources derived from requested scope
            resourceIds.addAll(extractResourceIds(scopes));
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
                    + " resource ids " + resourceIds.toString()
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

            // store client authorities to be used consistently during request
            request.setAuthorities(clientDetails.getAuthorities());

            // load resourceIds from params, fallback if missing
            if (resourceIds.isEmpty()) {
                request.setResourceIds(new HashSet<>(clientDetails.getResourceIds()));
            }

            // validate request for realm matching
            // TODO move to requestValidator, but authEndpoint calls only checkScopes for
            // now..
            if (!SystemKeys.REALM_COMMON.equals(realm) && !clientDetails.getRealm().equals(realm)) {
                throw new OAuth2AccessDeniedException();
            }

            if (!SystemKeys.REALM_COMMON.equals(realm) && !userAuth.getRealm().equals(realm)) {
                // throw an error, we should really ask user to re-authenticate with the given
                // realm
                // by delegating to the authenticationEntryPoint
                // TODO
                throw new OAuth2AccessDeniedException();
            }

            return request;
        } catch (ClientRegistrationException e) {
            throw new InvalidClientException("invalid client");
        }

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
        String realm = requestParameters.get("realm");

        try {
            OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

//            // check extensions and integrate modifications
//            if (flowExtensionsService != null) {
//                OAuthFlowExtensions ext = flowExtensionsService.getOAuthFlowExtensions(clientDetails);
//                if (ext != null) {
//
//                    Map<String, String> parameters = ext.onBeforeTokenGrant(requestParameters, clientDetails);
//                    if (parameters != null) {
//                        // merge parameters into request params
//                        requestParameters.putAll(parameters);
//                        // enforce base params consistency
//                        // TODO rewrite with proper merge with exclusion list
//                        requestParameters.put(OAuth2Utils.CLIENT_ID, clientId);
//                        requestParameters.put(OAuth2Utils.GRANT_TYPE, grantType);
//                        requestParameters.put("realm", realm);
//                    }
//                }
//            }

            Set<String> scopes = new HashSet<>();
            // check grantType and act accordingly to parse scopes
            if (Config.GRANT_TYPE_PASSWORD.equals(grantType) ||
                    Config.GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType) ||
                    Config.GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
                scopes = extractScopes(OAuth2Utils.parseParameterList(
                        decodeParameters(requestParameters.get(OAuth2Utils.SCOPE))),
                        clientDetails.getScope(), isClientRequest(grantType));
            }

            // we collect serviceIds as resourceIds to mark these as audience
            Set<String> resourceIds = new HashSet<>();
            resourceIds.addAll(OAuth2Utils.parseParameterList(
                    decodeParameters(requestParameters.get("resource"))));

            // also load resources derived from requested scope
            resourceIds.addAll(extractResourceIds(scopes));

            logger.trace("create token request for " + clientId + " realm " + String.valueOf(realm)
                    + " grantType " + grantType
                    + " scope " + String.valueOf(requestParameters.get(OAuth2Utils.SCOPE))
                    + " extracted scope " + scopes.toString()
                    + " resource ids " + resourceIds.toString());

            RealmTokenRequest request = new RealmTokenRequest(requestParameters,
                    clientId, realm,
                    scopes, resourceIds,
                    null,
                    grantType);

            // store client authorities to be used consistently during request
            request.setAuthorities(clientDetails.getAuthorities());

            // load resourceIds from params, fallback if missing to default
            if (resourceIds.isEmpty()) {
                request.setResourceIds(new HashSet<>(clientDetails.getResourceIds()));
            }

            // validate request for realm matching
            // TODO move to requestValidator, but tokenEndpoint calls only checkScopes for
            // now..
            if (!SystemKeys.REALM_COMMON.equals(realm) && !clientDetails.getRealm().equals(realm)) {
                throw new OAuth2AccessDeniedException();
            }

            return request;
        } catch (ClientRegistrationException e) {
            throw new InvalidClientException("invalid client");
        }
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
    private Set<String> extractScopes(Set<String> scopes, Collection<String> clientScopes, boolean isClient) {
        logger.trace("scopes from parameters " + scopes.toString());
        ScopeType type = isClient ? ScopeType.CLIENT : ScopeType.USER;

        if ((scopes == null || scopes.isEmpty())) {
            // If no scopes are specified in the incoming data, use the default values
            // registered with the client
            // (the spec allows us to choose between this option and rejecting the request
            // completely, so we'll take the
            // least obnoxious choice as a default).
            scopes = new HashSet<>(clientScopes);
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

        Set<String> allowedScopes = scopes;

        if (scopeRegistry != null) {
            // keep only those matching request type
            Set<Scope> scs = scopes.stream().map(s -> {
                return scopeRegistry.findScope(s);
            })
                    .filter(s -> s != null)
                    .filter(s -> (s.getType() == ScopeType.GENERIC || s.getType() == type))
                    .collect(Collectors.toSet());

            allowedScopes = scs.stream().map(s -> s.getScope()).collect(Collectors.toSet());

        }

        return allowedScopes;
    }

    private Set<String> extractResourceIds(Set<String> scopes) {
        if (scopeRegistry != null) {
            return scopes.stream().map(s -> {
                return scopeRegistry.findScope(s);
            })
                    .filter(s -> s != null)
                    .map(s -> s.getResourceId())
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();

    }

    private boolean isClientRequest(String grantType) {
        return Config.GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType);
    }

}
