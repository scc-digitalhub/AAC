package it.smartcommunitylab.aac.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.implicit.ImplicitTokenRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.DefaultClaimsService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.RealmAuthorizationRequest;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.request.AACOAuth2RequestFactory;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import it.smartcommunitylab.aac.services.ScriptServiceClaimExtractor;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServicesManager;

@Component
public class DevManager {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
    private final TypeReference<ArrayList<Serializable>> serListTypeRef = new TypeReference<ArrayList<Serializable>>() {
    };

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
    private DefaultClaimsService claimsService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private ScriptExecutionService executionService;

    @Autowired
    private ServicesManager serviceManager;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RealmService realmService;

    /*
     * Claims
     */

    public FunctionValidationBean testServiceClaimMapping(String realm, String serviceId,
            FunctionValidationBean functionBean)
            throws NoSuchRealmException, NoSuchServiceException, SystemException, InvalidDefinitionException {

        // extract function call
        String kind = functionBean.getName();
        String functionCode = StringUtils.hasText(functionBean.getCode())
                ? new String(Base64.getDecoder().decode(functionBean.getCode()))
                : null;
        Set<String> scopes = functionBean.getScopes() != null ? functionBean.getScopes() : Collections.emptySet();

        // TODO handle context init here
        // TODO handle errors
        // TODO handle log

        UserDetails userDetails = authHelper.getUserDetails();
        if (userDetails == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        // mock clientDetails
        String clientId = UUID.randomUUID().toString();
        Set<GrantedAuthority> clientAuthorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_CLIENT));
        ClientDetails clientDetails = new ClientDetails(clientId, realm,
                SystemKeys.CLIENT_TYPE_OAUTH2,
                clientAuthorities);

        // fetch and validate scopes?
        // not really needed for testing
        Set<String> approvedScopes = new HashSet<>();
        for (String s : scopes) {
            Scope scope = scopeRegistry.findScope(s);
            if (scope != null) {
                approvedScopes.add(scope.getScope());
            }
        }

        clientDetails.setScopes(approvedScopes);

        // fetch service
        Service service = serviceManager.getService(realm, serviceId);

        // clear hookFunctions already set and pass only test function
        Map<String, String> functions = new HashMap<>();
        functions.put(kind, functionCode);
        service.setClaimMapping(functions);

        // build extractor
        ScriptServiceClaimExtractor e = new ScriptServiceClaimExtractor(service);
        e.setExecutionService(executionService);

        if ("user".equals(kind)) {
            // map user and load attributes
            User user = userService.getUser(userDetails, realm);
            // narrow attributes
            if (!approvedScopes.contains(Config.SCOPE_FULL_PROFILE)) {
                user.setAttributes(claimsService.narrowUserAttributes(user.getAttributes(), approvedScopes));
            }

            if (!approvedScopes.contains(Config.SCOPE_ROLE)) {
                user.setAuthorities(null);
                user.setRoles(null);
            }

            // build context to populate result
            Map<String, Serializable> ctx = e.buildUserContext(user, clientDetails, approvedScopes, null);

            // execute
            ClaimsSet claimsSet = e.extractUserClaims(service.getNamespace(), user, clientDetails, approvedScopes,
                    null);
            // get map via claimsService (hack)
            Map<String, Serializable> claims = claimsService.claimsToMap(claimsSet.getClaims());

            // save result
            functionBean.setContext(ctx);
            functionBean.setResult(claims);

            // TODO log

        } else if ("client".equals(kind)) {
            // build context to populate result
            Map<String, Serializable> ctx = e.buildClientContext(clientDetails, approvedScopes, null);

            // execute
            ClaimsSet claimsSet = e.extractClientClaims(service.getNamespace(),
                    clientDetails, approvedScopes, null);
            // get map via claimsService (hack)
            Map<String, Serializable> claims = claimsService.claimsToMap(claimsSet.getClaims());

            // save result
            functionBean.setContext(ctx);
            functionBean.setResult(claims);

            // TODO log
        } else {
            throw new IllegalArgumentException("unsupported function kind");
        }

        return functionBean;
    }

//    public Map<String, Serializable> testServiceUserClaimMapping(String realm, String serviceId, String functionCode,
//            Collection<String> scopes)
//            throws NoSuchRealmException, NoSuchServiceException, SystemException, InvalidDefinitionException {
//        // fetch context
//        // TODO evaluate mock userDetails for testing
//        UserDetails userDetails = authHelper.getUserDetails();
//        if (userDetails == null) {
//            throw new InsufficientAuthenticationException("invalid or missing user authentication");
//        }
//
//        // mock clientDetails
//        String clientId = UUID.randomUUID().toString();
//        Set<GrantedAuthority> clientAuthorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_CLIENT));
//        ClientDetails clientDetails = new ClientDetails(clientId, realm,
//                SystemKeys.CLIENT_TYPE_OAUTH2,
//                clientAuthorities);
//
//        // fetch and validate scopes?
//        // not really needed for testing
//        Set<String> approvedScopes = new HashSet<>();
//        for (String s : scopes) {
//            Scope scope = scopeRegistry.findScope(s);
//            if (scope != null) {
//                approvedScopes.add(scope.getScope());
//            }
//        }
//
//        clientDetails.setScopes(approvedScopes);
//
//        // fetch service
//        Service service = serviceManager.getService(realm, serviceId);
//
//        // clear hookFunctions already set and pass only test function
//        Map<String, String> functions = new HashMap<>();
//        functions.put("user", functionCode);
//        service.setClaimMapping(functions);
//
//        // build extractor
//        ScriptServiceClaimExtractor e = new ScriptServiceClaimExtractor(service);
//        e.setExecutionService(executionService);
//
//        // map user and load attributes
//        User user = userService.getUser(userDetails, realm);
//        // narrow attributes
//        if (!approvedScopes.contains(Config.SCOPE_FULL_PROFILE)) {
//            user.setAttributes(claimsService.narrowUserAttributes(user.getAttributes(), approvedScopes));
//        }
//
//        if (!approvedScopes.contains(Config.SCOPE_ROLE)) {
//            user.setAuthorities(null);
//            user.setRoles(null);
//        }
//
//        // build context to populate result
//        Map<String, Serializable> ctx = e.buildUserContext(user, clientDetails, approvedScopes, null);
//
//        // execute
//        ClaimsSet claimsSet = e.extractUserClaims(service.getNamespace(), user, clientDetails, approvedScopes, null);
//        // get map via claimsService (hack)
//        Map<String, Serializable> claims = claimsService.claimsToMap(claimsSet.getClaims());
//
//        return claims;
//    }
//
//    public Map<String, Serializable> testServiceClientClaimMapping(String realm, String serviceId, String functionCode,
//            Collection<String> scopes)
//            throws NoSuchRealmException, NoSuchServiceException, SystemException, InvalidDefinitionException {
//        // fetch context
//        // TODO evaluate mock userDetails for testing
//        UserDetails userDetails = authHelper.getUserDetails();
//        if (userDetails == null) {
//            throw new InsufficientAuthenticationException("invalid or missing user authentication");
//        }
//
//        // mock clientDetails
//        String clientId = UUID.randomUUID().toString();
//        Set<GrantedAuthority> clientAuthorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_CLIENT));
//        ClientDetails clientDetails = new ClientDetails(clientId, realm,
//                SystemKeys.CLIENT_TYPE_OAUTH2,
//                clientAuthorities);
//
//        // fetch and validate scopes?
//        // not really needed for testing
//        Set<String> approvedScopes = new HashSet<>();
//        for (String s : scopes) {
//            Scope scope = scopeRegistry.findScope(s);
//            if (scope != null) {
//                approvedScopes.add(scope.getScope());
//            }
//        }
//
//        clientDetails.setScopes(approvedScopes);
//
//        // fetch service
//        Service service = serviceManager.getService(realm, serviceId);
//
//        // clear hookFunctions already set and pass only test function
//        Map<String, String> functions = new HashMap<>();
//        functions.put("client", functionCode);
//        service.setClaimMapping(functions);
//
//        // build extractor
//        ScriptServiceClaimExtractor e = new ScriptServiceClaimExtractor(service);
//        e.setExecutionService(executionService);
//
//        // execute
//        ClaimsSet claimsSet = e.extractClientClaims(service.getNamespace(),
//                clientDetails, approvedScopes, null);
//        // get map via claimsService (hack)
//        Map<String, Serializable> claims = claimsService.claimsToMap(claimsSet.getClaims());
//
//        return claims;
//    }

    @SuppressWarnings("unchecked")
    public FunctionValidationBean testClientClaimMapping(String realm, String clientId,
            FunctionValidationBean functionBean)
            throws NoSuchClientException, SystemException, NoSuchResourceException, InvalidDefinitionException {
        // TODO handle context init here
        // TODO handle errors
        // TODO handle log

        // fetch context
        // TODO evaluate mock userDetails for testing, maybe read from functionBean
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
        if (functionBean.getAttributes() != null && functionBean.getAttributes().get("scopes") != null) {
            clientScopes = mapper.convertValue(functionBean.getAttributes().get("scopes"), ArrayList.class);
        }

        Set<String> resourceIds = new HashSet<>(clientDetails.getResourceIds());
        Set<String> approvedScopes = getClientApprovedScopes(clientDetails, userDetails, clientScopes);

        // clear hookFunctions already set and pass only test function
        String functionCode = StringUtils.hasText(functionBean.getCode())
                ? new String(Base64.getDecoder().decode(functionBean.getCode()))
                : null;
        Map<String, String> functions = new HashMap<>();
        functions.put(DefaultClaimsService.CLAIM_MAPPING_FUNCTION, functionCode);
        clientDetails.setHookFunctions(functions);

        // fetch claims as input
        clientDetails.setHookFunctions(null);
        Map<String, Serializable> ctx = claimsService.getUserClaims(userDetails, realm, clientDetails,
                approvedScopes, resourceIds, null);
        functionBean.setContext(ctx);

        // fetch claims with function
        clientDetails.setHookFunctions(functions);
        Map<String, Serializable> claims = claimsService.getUserClaims(userDetails, realm, clientDetails,
                approvedScopes, resourceIds, null);

        // TODO execute here claimMapping after default instead of passing to
        // claimsService new function

        functionBean.setResult(claims);

        return functionBean;
    }

    /*
     * Realm customization
     */

    public String previewRealmTemplate(String realm, String template, CustomizationBean cb, WebContext ctx)
            throws NoSuchRealmException {

        Realm re = realmService.getRealm(realm);

        if (!template.equals(cb.getIdentifier())) {
            throw new IllegalArgumentException("customization does not match template");
        }

        // build model
        Map<String, Object> model = new HashMap<>();
        model.put("realm", realm);
        model.put("displayName", re.getName());
        model.put("customization", cb.getResources());

        // Create the HTML body using Thymeleaf
        for (String var : model.keySet()) {
            ctx.setVariable(var, model.get(var));
        }

        final String html = this.templateEngine.process(template, ctx);

        return html;
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
            ImplicitTokenRequest implicitRequest = new ImplicitTokenRequest(tokenRequest, storedOAuth2Request);
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
