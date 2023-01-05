package it.smartcommunitylab.aac.oauth.request;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.UnsupportedGrantTypeException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.PlainObject;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.flow.FlowExtensionsService;
import it.smartcommunitylab.aac.oauth.flow.OAuthFlowExtensions;
import it.smartcommunitylab.aac.oauth.model.ApplicationType;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.ResponseType;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.openid.common.exceptions.InvalidRequestObjectException;
import it.smartcommunitylab.aac.openid.common.exceptions.UnsupportedRequestUriException;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

public class OAuth2RequestFactory
        implements OAuth2TokenRequestFactory, OAuth2AuthorizationRequestFactory,
        OAuth2RegistrationRequestFactory,
        InitializingBean,
        org.springframework.security.oauth2.provider.OAuth2RequestFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private FlowExtensionsService flowExtensionsService;

    // TODO remove, needed only for legacy
    private OAuth2ClientDetailsService clientDetailsService;

    private ScopeRegistry scopeRegistry;

    public OAuth2RequestFactory() {

    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientDetailsService, "client details service is mandatory");

    }

    public void setFlowExtensionsService(FlowExtensionsService flowExtensionsService) {
        this.flowExtensionsService = flowExtensionsService;
    }

    public void setClientDetailsService(OAuth2ClientDetailsService clientDetailsService) {
        this.clientDetailsService = clientDetailsService;
    }

    public void setScopeRegistry(ScopeRegistry scopeRegistry) {
        this.scopeRegistry = scopeRegistry;
    }

    @Override
    public TokenRequest createTokenRequest(Map<String, String> requestParameters, OAuth2ClientDetails clientDetails) {
        try {
            // required parameters
            String clientId = readParameter(requestParameters, "client_id", SLUG_PATTERN);
            String grantType = readParameter(requestParameters, "grant_type", STRING_PATTERN);

            // check if we didn't receive clientId, use authentication info
            if (clientId == null) {
                clientId = clientDetails.getClientId();
            } else {
                // check if clientId provided that it matches auth info
                if (!clientId.equals(clientDetails.getClientId())) {
                    throw new InvalidRequestException("wrong client");
                }
            }

            // check extensions and integrate modifications
            if (flowExtensionsService != null) {
                OAuthFlowExtensions ext = flowExtensionsService.getOAuthFlowExtensions(clientDetails);
                if (ext != null) {

                    Map<String, String> parameters = ext.onBeforeTokenGrant(requestParameters, clientDetails);
                    if (parameters != null) {
                        // merge parameters into request params
                        requestParameters.putAll(parameters);
                        // enforce base params consistency
                        // TODO rewrite with proper merge with exclusion list
                        requestParameters.put("client_id", clientId);
                        requestParameters.put("grant_type", grantType);
                    }
                }
            }

            // check if scopes are requested or fall back
            Set<String> scopes = null;
            if (requestParameters.get("scope") != null) {
                scopes = delimitedStringToSet(decodeParameters(requestParameters.get("scope")));
            }

            // we collect serviceIds as resourceIds to mark these as audience
            // TODO fix this, services are AUDIENCE not resources!
            // we need a field in tokenRequest
            Set<String> resourceIds = delimitedStringToSet(decodeParameters(requestParameters.get("resource")));

            // depend on flow
            // use per flow token request subtype
            AuthorizationGrantType authorizationGrantType = AuthorizationGrantType.parse(grantType);
            if (authorizationGrantType == AUTHORIZATION_CODE) {
                String code = readParameter(requestParameters, "code", STRING_PATTERN);
                String redirectUri = readParameter(requestParameters, "redirect_uri", URI_PATTERN);

                // use scopes as requested via authorization
                Set<String> requestScopes = scopes;
                // also load resources derived from requested scope
                resourceIds.addAll(extractResourceIds(clientDetails.getRealm(), requestScopes));

                logger.trace("create token request for " + clientId
                        + " grantType " + grantType
                        + " code " + String.valueOf(code)
                        + " redirectUri " + String.valueOf(redirectUri)
                        + " scopes " + String.valueOf(requestScopes)
                        + " resource ids " + String.valueOf(resourceIds));

                return new AuthorizationCodeTokenRequest(requestParameters, clientId,
                        code, redirectUri,
                        requestScopes, resourceIds, null);
            }
            if (authorizationGrantType == IMPLICIT) {
                // we can't build an implicit token request from params, only from authRequest
                throw new UnsupportedGrantTypeException("Grant type not supported: " + grantType);
            }
            if (authorizationGrantType == PASSWORD) {
                String username = readParameter(requestParameters, "username", EMAIL_PATTERN);
                String password = requestParameters.get("password");
                // read or load scopes
                // when request scopes are null (ie not specified as param) use all scopes
                // registered by client
                Set<String> requestScopes = scopes != null ? scopes : new HashSet<>(clientDetails.getScope());

                // remove offline_access if requested and still present
                // password flow SHOULD not support refresh tokens
                // but we let request pass, since from spec it COULD be valid
                // https://www.rfc-editor.org/rfc/rfc6749#section-4.3.3
                if (requestScopes.contains(Config.SCOPE_OFFLINE_ACCESS)) {
                    requestScopes.remove(Config.SCOPE_OFFLINE_ACCESS);
                }

                // also load resources derived from requested scope
                resourceIds.addAll(extractResourceIds(clientDetails.getRealm(), requestScopes));

                logger.trace("create token request for " + clientId
                        + " grantType " + grantType
                        + " user " + username
                        + " scopes " + String.valueOf(requestScopes)
                        + " resource ids " + String.valueOf(resourceIds));

                return new ResourceOwnerPasswordTokenRequest(requestParameters, clientId,
                        username, password,
                        requestScopes, resourceIds,
                        null);
            }

            if (authorizationGrantType == CLIENT_CREDENTIALS) {
                // read or load scopes
                // when request scopes are null (ie not specified as param) use all scopes
                // registered by client
                Set<String> requestScopes = scopes != null ? scopes : new HashSet<>(clientDetails.getScope());

                // check offline_access if requested and still present
                // client flow MUST not support refresh tokens
                // https://www.rfc-editor.org/rfc/rfc6749#section-4.4.3
                if (requestScopes.contains(Config.SCOPE_OFFLINE_ACCESS)) {
                    throw new InvalidScopeException(Config.SCOPE_OFFLINE_ACCESS);
                }

                // also load resources derived from requested scope
                resourceIds.addAll(extractResourceIds(clientDetails.getRealm(), requestScopes));

                logger.trace("create token request for " + clientId
                        + " grantType " + grantType
                        + " client " + clientId
                        + " scopes " + String.valueOf(requestScopes)
                        + " resource ids " + String.valueOf(resourceIds));

                return new TokenRequest(requestParameters, clientId, authorizationGrantType.getValue(), requestScopes,
                        resourceIds,
                        null);
            }

            if (authorizationGrantType == REFRESH_TOKEN) {
                // refresh tokens can ask scopes, but by default will get those in original
                // request and not the client default
                Set<String> requestScopes = scopes;

                // also load resources derived from requested scope
                resourceIds.addAll(extractResourceIds(clientDetails.getRealm(), requestScopes));

                logger.trace("create token request for " + clientId
                        + " grantType " + grantType
                        + " client " + clientId
                        + " scope " + String.valueOf(requestScopes)
                        + " resource id " + String.valueOf(resourceIds));

                return new TokenRequest(requestParameters, clientId, authorizationGrantType.getValue(), requestScopes,
                        resourceIds,
                        null);
            }

            throw new UnsupportedGrantTypeException("Grant type not supported: " + grantType);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    @Override
    public TokenRequest createTokenRequest(AuthorizationRequest authorizationRequest, String grantType) {
        AuthorizationGrantType authorizationGrantType = AuthorizationGrantType.parse(grantType);
        // only implicit grant can convert request
        if (authorizationGrantType == IMPLICIT) {
            // get scope from authorization request
            Set<String> scope = authorizationRequest.getScope();

            // check offline_access if requested and still present
            // implicit flow MUST not support refresh tokens
            // https://www.rfc-editor.org/rfc/rfc6749#section-4.2.2
            if (scope != null && scope.contains(Config.SCOPE_OFFLINE_ACCESS)) {
                throw new InvalidScopeException(Config.SCOPE_OFFLINE_ACCESS);
            }

            ImplicitTokenRequest tokenRequest = new ImplicitTokenRequest(
                    authorizationRequest.getRequestParameters(),
                    authorizationRequest.getClientId(),
                    scope, authorizationRequest.getRedirectUri(),
                    authorizationRequest.getResourceIds(), null);

            return tokenRequest;

        }

        throw new UnsupportedGrantTypeException("Grant type not supported: " + grantType);
    }

    @Override
    public OAuth2Request createOAuth2Request(TokenRequest tokenRequest, OAuth2ClientDetails client) {
        return tokenRequest.createOAuth2Request(client);
    }

    @Override
    public AuthorizationRequest createAuthorizationRequest(Map<String, String> requestParameters,
            OAuth2ClientDetails clientDetails, User user) {
        try {
            // *always* required parameters
            String clientId = readParameter(requestParameters, "client_id", SLUG_PATTERN);
            String responseType = readParameter(requestParameters, "response_type", SPACE_STRING_PATTERN);
            Set<String> responseTypes = delimitedStringToSet(decodeParameters(responseType));

            // optional
            String state = readParameter(requestParameters, "state", SPECIAL_PATTERN);
//        String state = null;
//        try {
//            state = readParameter(requestParameters, "state", SPECIAL_PATTERN);
//        } catch (IllegalArgumentException e) {
//            // try to re-encode as url param
//            try {
//                String raw = requestParameters.get("state");
//                if (raw != null) {
//                    // use encoded string as param for pattern matching
//                    String s = readParameter(URLEncoder.encode(raw, "UTF-8"), SPECIAL_PATTERN);
//                    // check if matches unencoded, we'll let that pass in this case
//                    if (URLDecoder.decode(s, "UTF-8").equals(raw)) {
//                        state = raw;
//                    } else {
//                        state = s;
//                    }
//                }
//            } catch (UnsupportedEncodingException ex) {
//            }
//        }

            String nonce = readParameter(requestParameters, "nonce", SPECIAL_PATTERN);

            // check if we didn't receive clientId, use authentication info
            if (clientId == null) {
                clientId = clientDetails.getClientId();
            }

            // check extensions and integrate modifications
            if (flowExtensionsService != null) {
                OAuthFlowExtensions ext = flowExtensionsService.getOAuthFlowExtensions(clientDetails);
                if (ext != null) {

                    Map<String, String> parameters = ext.onBeforeUserApproval(requestParameters, user, clientDetails);
                    if (parameters != null) {
                        // merge parameters into request params
                        requestParameters.putAll(parameters);
                        // enforce base params consistency
                        // TODO rewrite with proper merge with exclusion list
                        requestParameters.put("client_id", clientId);
                        requestParameters.put("response_type", responseType);

                        requestParameters.put("state", state);
                        requestParameters.put("nonce", nonce);
                    }
                }
            }

            String redirectUri = readParameter(requestParameters, "redirect_uri", URI_PATTERN);
            String responseMode = readParameter(requestParameters, "response_mode", STRING_PATTERN);
            if (responseMode == null) {
                responseMode = (responseTypes.contains("token") || responseTypes.contains("id_token")) ? "fragment"
                        : "query";
            }

            // check if scopes are requested or fall back
            Set<String> scopes = null;
            if (requestParameters.get("scope") != null) {
                scopes = delimitedStringToSet(decodeParameters(requestParameters.get("scope")));
            }

            // read or load scopes
            Set<String> requestScopes = scopes;
            if (scopes == null) {
                // when request scopes are null (ie not specified as param) use all scopes
                // registered by client
                scopes = new HashSet<>(clientDetails.getScope());
            }

            // we collect serviceIds as resourceIds to mark these as audience
            // TODO fix this, services are AUDIENCE not resources!
            // we need a field in tokenRequest
            Set<String> resourceIds = delimitedStringToSet(decodeParameters(requestParameters.get("resource")));

            // also load resources derived from requested scope
            resourceIds.addAll(extractResourceIds(clientDetails.getRealm(), scopes));

//            Set<String> audience = delimitedStringToSet(decodeParameters(requestParameters.get("audience")));

            Set<String> prompt = delimitedStringToSet(decodeParameters(requestParameters.get("prompt")));

            // support request param only for openid
            String request = requestParameters.get("request");
            if (StringUtils.hasText(request) && scopes.contains("openid")) {
                // this should be a JWT
                // we support only plain for now
                try {
                    JOSEObject jwt = JOSEObject.parse(request);
                    if (!(jwt instanceof PlainObject)) {
                        throw new InvalidRequestObjectException("request param type unsupported");
                    }

                    Map<String, Object> json = jwt.getPayload().toJSONObject();

                    // values in jwt superseed params
                    if (StringUtils.hasText(JSONObjectUtils.getString(json, "state"))) {
                        state = readParameter(JSONObjectUtils.getString(json, "state"), SPECIAL_PATTERN);
                    }
                    if (StringUtils.hasText(JSONObjectUtils.getString(json, "nonce"))) {
                        nonce = readParameter(JSONObjectUtils.getString(json, "nonce"), SPECIAL_PATTERN);
                    }
                    if (StringUtils.hasText(JSONObjectUtils.getString(json, "redirect_uri"))) {
                        redirectUri = readParameter(JSONObjectUtils.getString(json, "redirect_uri"), URI_PATTERN);
                    }
                    if (StringUtils.hasText(JSONObjectUtils.getString(json, "response_mode"))) {
                        responseMode = readParameter(JSONObjectUtils.getString(json, "response_mode"), STRING_PATTERN);
                    }
                    if (StringUtils.hasText(JSONObjectUtils.getString(json, "resource"))) {
                        resourceIds = delimitedStringToSet(JSONObjectUtils.getString(json, "resource"));
                    }
//                    if (StringUtils.hasText(JSONObjectUtils.getString(json, "audience"))) {
//                        audience = delimitedStringToSet(JSONObjectUtils.getString(json, "audience"));
//                    }
                    if (StringUtils.hasText(JSONObjectUtils.getString(json, "prompt"))) {
                        prompt = delimitedStringToSet(JSONObjectUtils.getString(json, "prompt"));
                    }

                } catch (ParseException e) {
                    throw new InvalidRequestObjectException("request param is malformed");
                }

            }

            // we do not support request_uri
            String requestUri = requestParameters.get("request_uri");
            if (StringUtils.hasText(requestUri)) {
                throw new UnsupportedRequestUriException("request_uri is not supported");
            }

            logger.trace("create authorization request for " + clientId
                    + " response type " + String.valueOf(responseTypes)
                    + " response mode " + String.valueOf(responseMode)
                    + " redirect " + String.valueOf(redirectUri)
                    + " scope " + String.valueOf(requestScopes)
                    + " resource ids " + String.valueOf(resourceIds));

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(requestParameters,
                    Collections.<String, String>emptyMap(),
                    clientId, requestScopes,
                    resourceIds, null, false,
                    state, redirectUri,
                    responseTypes);

            // extensions
            Map<String, Serializable> extensions = authorizationRequest.getExtensions();

//            // audience
//            extensions.put("audience", StringUtils.collectionToCommaDelimitedString(audience));

            // response mode
            extensions.put("response_mode", responseMode);

            // support NONCE
            if (StringUtils.hasText(nonce)) {
                extensions.put("nonce", nonce);
            }

            // support prompt
            if (!prompt.isEmpty()) {
                extensions.put("prompt", StringUtils.collectionToCommaDelimitedString(prompt));
            }

            return authorizationRequest;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    @Override
    public OAuth2Request createOAuth2Request(AuthorizationRequest request, OAuth2ClientDetails clientDetails) {
        return request.createOAuth2Request();
    }

    @Override
    public ClientRegistrationRequest createClientRegistrationRequest(Map<String, Serializable> registrationParameters) {
        ClientRegistration registration = null;
        SignedJWT jwt = null;

        // use mapper to convert to model
        try {
            registration = mapper.convertValue(registrationParameters, ClientRegistration.class);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("invalid registration");
        }

        // set defaults if missing
        if (registration.getResponseTypes() == null) {
            registration.setResponseTypes(Collections.singleton(ResponseType.CODE.getValue()));
        }
        if (registration.getGrantType() == null) {
            registration.setGrantType(Collections.singleton(AUTHORIZATION_CODE.getValue()));
        }

        if (registration.getApplicationType() == null) {
            registration.setApplicationType(ApplicationType.WEB.getValue());
        }

        if (registration.getAuthenticationMethods() == null) {
            registration.setAuthenticationMethods(
                    Collections.singleton(AuthenticationMethod.CLIENT_SECRET_BASIC.getValue()));
        }

        // check if software statement is provided
        try {
            String softwareStatement = (String) registrationParameters.get("software_statement");
            if (StringUtils.hasText(softwareStatement)) {
                // try to parse as JWT or error

                jwt = SignedJWT.parse(softwareStatement);

            }
        } catch (Exception e) {
            throw new InvalidRequestException("invalid software_statement");
        }

        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(registration, jwt);

        return registrationRequest;
    }

//    private Set<String> extractScopes(Set<String> scopes, Collection<String> clientScopes) {
//
//        if (scopes == null) {
//            // when request scopes are null use all scopes registered by client
//            scopes = new HashSet<>(clientScopes);
//        }
//
//        return scopes;

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
// disable check, validator will take care
//        Set<String> allowedScopes = scopes;
//
//        if (scopeRegistry != null) {
//            // keep only those matching request type
//            Set<Scope> scs = scopes.stream().map(s -> {
//                return scopeRegistry.findScope(s);
//            })
//                    .filter(s -> s != null)
//                    .filter(s -> (s.getType() == ScopeType.GENERIC || s.getType() == type))
//                    .collect(Collectors.toSet());
//
//            allowedScopes = scs.stream().map(s -> s.getScope()).collect(Collectors.toSet());
//
//        }
//
//        return allowedScopes;
//    }

    private List<String> extractResourceIds(String realm, Set<String> scopes) {
        if (scopes != null && scopeRegistry != null) {
            return scopes.stream()
                    .map(s -> scopeRegistry.tryResolveScope(realm, s))
                    .filter(s -> s != null)
                    .map(s -> scopeRegistry.resolveResourceById(realm, s.getApiResourceId()))
                    .filter(r -> r != null)
                    .map(r -> r.getResource())
                    .filter(i -> i != null)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private Set<String> delimitedStringToSet(String str) {
        String[] tokens = StringUtils.delimitedListToStringArray(str, DELIMITER);
        return new LinkedHashSet<>(Arrays.asList(tokens));
    }

    /*
     * Workaround for badly formatted param
     */
    private String decodeParameters(String value) {
        String result = value;
        if (StringUtils.hasText(result)) {

            // check if spaces are still encoded as %20
            if (result.contains("%20")) {
                // replace with spaces
                result = result.replace("%20", DELIMITER);
            }

            // check if strings are separated with comma (out of spec)
            if (result.contains(",")) {
                result = result.replace(",", DELIMITER);
            }
        }

        return result;
    }

    private final static String DELIMITER = " ";

    private final static AuthorizationGrantType AUTHORIZATION_CODE = AuthorizationGrantType.AUTHORIZATION_CODE;
    private final static AuthorizationGrantType IMPLICIT = AuthorizationGrantType.IMPLICIT;
    private final static AuthorizationGrantType CLIENT_CREDENTIALS = AuthorizationGrantType.CLIENT_CREDENTIALS;
    private final static AuthorizationGrantType PASSWORD = AuthorizationGrantType.PASSWORD;
    private final static AuthorizationGrantType REFRESH_TOKEN = AuthorizationGrantType.REFRESH_TOKEN;

    public final static String SLUG_PATTERN = SystemKeys.SLUG_PATTERN;
    public final static String STRING_PATTERN = "^[a-zA-Z0-9_:-]+$";
    public final static String EMAIL_PATTERN = SystemKeys.EMAIL_PATTERN;
    public final static String URI_PATTERN = "^[a-zA-Z0-9._:/-]+$";
    public final static String SPECIAL_PATTERN = "^[a-zA-Z0-9_!=@$&%():/\\-`.+,/\"]*$";
    public final static String SPACE_STRING_PATTERN = "^[a-zA-Z0-9 _:-]+$";

    /*
     * Legacy factory
     * 
     * TODO remove after updating token granters and rewrite properly as
     * tokenservices..
     */

    @Override
    public AuthorizationRequest createAuthorizationRequest(Map<String, String> authorizationParameters) {
        String clientId = authorizationParameters.get("client_id");
        OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
        return createAuthorizationRequest(authorizationParameters, clientDetails, null);
    }

    @Override
    public OAuth2Request createOAuth2Request(AuthorizationRequest request) {
        return request.createOAuth2Request();
    }

    @Override
    public OAuth2Request createOAuth2Request(ClientDetails client,
            org.springframework.security.oauth2.provider.TokenRequest tokenRequest) {
        OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(client.getClientId());
        return tokenRequest.createOAuth2Request(clientDetails);
    }

    @Override
    public org.springframework.security.oauth2.provider.TokenRequest createTokenRequest(
            Map<String, String> requestParameters, ClientDetails authenticatedClient) {
        String clientId = requestParameters.get("client_id");
        OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
        return createTokenRequest(requestParameters, clientDetails);
    }

    private String readParameter(Map<String, String> requestParameters, String key, String pattern)
            throws IllegalArgumentException {
        if (!requestParameters.containsKey(key)) {
            return null;
        }

        String raw = requestParameters.get(key);
        return readParameter(raw, pattern);

    }

    private String readParameter(String raw, String pattern)
            throws IllegalArgumentException {

        if (!raw.matches(pattern)) {
            throw new IllegalArgumentException("param does not match pattern " + String.valueOf(pattern));
        }

        return raw.trim();

    }

}
