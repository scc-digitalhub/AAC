package it.smartcommunitylab.aac.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.implicit.ImplicitTokenRequest;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.claims.DefaultClaimsService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.RealmAuthorizationRequest;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.request.AACOAuth2RequestFactory;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

@Component
public class DevManager {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private AACOAuth2RequestFactory oauth2RequestFactory;

    @Autowired
    private OAuth2ClientDetailsService oauth2ClientDetailsService;

    @Autowired
    private AuthorizationCodeServices authorizationCodeServices;

    @Autowired
    private TokenGranter oauth2TokenGranter;

    @Autowired
    private ClaimsService claimsService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private ScriptExecutionService scriptExecutionService;

    /*
     * Claims
     */

    public Map<String, Serializable> testClientClaimMapping(String realm, String clientId, String functionCode)
            throws NoSuchClientException, SystemException, NoSuchResourceException, InvalidDefinitionException {
        // fetch context
        // TODO evaluate mock userDetails for testing
        UserDetails userDetails = authHelper.getUserDetails();
        if (userDetails == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        ClientDetails clientDetails = clientDetailsService.loadClient(clientId);
        if (!clientDetails.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        // fetch and validate scopes and resourceids, we need approvals
        Collection<String> clientScopes = clientDetails.getScopes();
        Set<String> resourceIds = new HashSet<>(clientDetails.getResourceIds());
        Set<String> approvedScopes = getClientApprovedScopes(clientDetails, userDetails, clientScopes);

        // clear hookFunctions already set and pass only test function
        Map<String, String> functions = new HashMap<>();
        functions.put(DefaultClaimsService.CLAIM_MAPPING_FUNCTION, functionCode);
        clientDetails.setHookFunctions(functions);

        // fetch claims
        Map<String, Serializable> claims = claimsService.getUserClaims(userDetails, realm, clientDetails,
                approvedScopes, resourceIds);

        return claims;
    }

    /*
     * OAuth2
     */

    public OAuth2AccessToken testOAuth2Flow(String realm, String clientId, String grantType)
            throws NoSuchClientException {
        // TODO evaluate mock userDetails for testing
        UserAuthenticationToken userAuth = authHelper.getUserAuthentication();
        if (userAuth == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }
        UserDetails userDetails = userAuth.getUser();

        ClientDetails clientDetails = clientDetailsService.loadClient(clientId);
        OAuth2ClientDetails oauthClientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);
        if (!clientDetails.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        // check if flow is enabled
        AuthorizationGrantType gt = AuthorizationGrantType.parse(grantType);
        if (gt == null || !oauthClientDetails.getAuthorizedGrantTypes().contains(grantType)) {
            throw new IllegalArgumentException("unauthorized grant type");
        }

        Set<String> approvedScopes = getClientApprovedScopes(clientDetails, userDetails, oauthClientDetails.getScope());

        // build base params
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("realm", realm);
        requestParams.put("client_id", clientId);
        requestParams.put("scope", String.join(" ", approvedScopes));
        requestParams.put("resource_id", String.join(" ", clientDetails.getResourceIds()));

        // build flow params
        if (gt == AuthorizationGrantType.AUTHORIZATION_CODE || gt == AuthorizationGrantType.IMPLICIT) {

            if (oauthClientDetails.getRegisteredRedirectUri().isEmpty()) {
                throw new IllegalArgumentException("missing redirect uri");
            }

            String responseType = "code";
            if (gt == AuthorizationGrantType.IMPLICIT) {
                responseType = "token";
            }

            String state = RandomStringUtils.randomAlphanumeric(8);
            String nonce = RandomStringUtils.randomAlphanumeric(12);
            String redirectUri = oauthClientDetails.getRegisteredRedirectUri().iterator().next();

            requestParams.put("response_type", responseType);
            requestParams.put("state", state);
            requestParams.put("nonce", nonce);
            requestParams.put("redirect_uri", redirectUri);

            OAuth2AccessToken accessToken = testOAuth2AuthorizationFlow(realm, userAuth, oauthClientDetails,
                    requestParams);

            return accessToken;

        } else if (gt == AuthorizationGrantType.CLIENT_CREDENTIALS) {

            requestParams.put("grant_type", gt.getValue());

            OAuth2AccessToken accessToken = testOAuth2TokenFlow(realm, oauthClientDetails, requestParams);

            return accessToken;
        }

        // we don't support any other flow
        return null;

    }

    private OAuth2AccessToken testOAuth2AuthorizationFlow(String realm, UserAuthenticationToken userAuth,
            OAuth2ClientDetails clientDetails,
            Map<String, String> authorizationParams) {

        // build request, temporary replace realm to enable access to all users
        authorizationParams.put("realm", "common");
        RealmAuthorizationRequest authorizationRequest = oauth2RequestFactory
                .createAuthorizationRequest(authorizationParams);
        authorizationRequest.setRealm(realm);

        // set approved for test, we override approval stage because we
        // are sure scopes are allowed
        authorizationRequest.setApproved(true);

        Set<String> responseTypes = authorizationRequest.getResponseTypes();
        if (responseTypes.contains("token")) {
            // fetch implicit token
            String grantType = AuthorizationGrantType.IMPLICIT.getValue();
            TokenRequest tokenRequest = oauth2RequestFactory.createTokenRequest(authorizationRequest, grantType);
            OAuth2Request storedOAuth2Request = oauth2RequestFactory.createOAuth2Request(authorizationRequest);
            ImplicitTokenRequest implicitRequest =   new ImplicitTokenRequest(tokenRequest, storedOAuth2Request);
            OAuth2AccessToken accessToken = oauth2TokenGranter.grant(grantType, implicitRequest);

            return accessToken;
        } else {
            // fetch code and exchange
            String grantType = AuthorizationGrantType.AUTHORIZATION_CODE.getValue();
            OAuth2Request storedOAuth2Request = oauth2RequestFactory.createOAuth2Request(authorizationRequest);

            OAuth2Authentication combinedAuth = new OAuth2Authentication(storedOAuth2Request, userAuth);
            String code = authorizationCodeServices.createAuthorizationCode(combinedAuth);

            // exchange
            Map<String, String> tokenParams = new HashMap<>();
            tokenParams.put("realm", realm);
            tokenParams.put("client_id", authorizationParams.get("client_id"));
            tokenParams.put("scope", authorizationParams.get("scope"));
            tokenParams.put("resource_id", authorizationParams.get("resource_id"));
            tokenParams.put("grant_type", grantType);
            tokenParams.put("code", code);
            tokenParams.put("redirect_uri", authorizationParams.get("redirect_uri"));

            TokenRequest tokenRequest = oauth2RequestFactory.createTokenRequest(tokenParams, clientDetails);
            OAuth2AccessToken accessToken = oauth2TokenGranter.grant(grantType, tokenRequest);

            return accessToken;

        }

    }

    private OAuth2AccessToken testOAuth2TokenFlow(String realm,
            OAuth2ClientDetails clientDetails,
            Map<String, String> tokenParams) {

        // build request
        TokenRequest tokenRequest = oauth2RequestFactory.createTokenRequest(tokenParams, clientDetails);
        OAuth2AccessToken accessToken = oauth2TokenGranter.grant(tokenRequest.getGrantType(), tokenRequest);

        return accessToken;

    }

    private Set<String> getClientApprovedScopes(ClientDetails clientDetails, UserDetails userDetails,
            Collection<String> clientScopes) {
        Set<String> approvedScopes = new HashSet<>();
        for (String s : clientScopes) {
            try {
                Scope scope = scopeRegistry.getScope(s);
                ScopeApprover sa = scopeRegistry.getScopeApprover(s);
                if (sa == null) {
                    // this scope is undecided so skip
                    continue;
                }

                Approval approval = null;
                if (ScopeType.CLIENT == scope.getType()) {
                    approval = sa.approveClientScope(s, clientDetails, clientScopes);
                }
                if (ScopeType.USER == scope.getType() && userDetails != null) {
                    approval = sa.approveUserScope(s,
                            userService.getUser(userDetails, sa.getRealm()), clientDetails,
                            clientScopes);
                }
                if (ScopeType.GENERIC == scope.getType()) {
                    if (userDetails != null) {
                        approval = sa.approveUserScope(s,
                                userService.getUser(userDetails, sa.getRealm()),
                                clientDetails, clientScopes);
                    } else {
                        approval = sa.approveClientScope(s, clientDetails, clientScopes);
                    }

                }

                if (approval != null) {
                    if (approval.isApproved()) {
                        approvedScopes.add(s);
                    }
                }

            } catch (NoSuchScopeException | SystemException | InvalidDefinitionException e) {
                // ignore
            }

        }

        return approvedScopes;
    }

}
