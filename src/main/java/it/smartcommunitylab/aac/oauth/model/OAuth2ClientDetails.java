package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.repository.StringArraySerializer;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.Jackson2ArrayOrStringDeserializer;

@JsonInclude(Include.NON_NULL)
public class OAuth2ClientDetails implements ClientDetails {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("client_name")
    private String name;

    @JsonProperty("scope")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    @JsonSerialize(using = StringArraySerializer.class)
    private Set<String> scope = Collections.emptySet();

    @JsonProperty("resource_ids")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    @JsonSerialize(using = StringArraySerializer.class)
    private Set<String> resourceIds = Collections.emptySet();

    @JsonProperty("grant_types")
    private Set<String> authorizedGrantTypes = Collections.emptySet();

    @JsonProperty("token_endpoint_auth_method")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    @JsonSerialize(using = StringArraySerializer.class)
    private Set<String> authenticationMethods = Collections.emptySet();

    @JsonProperty("redirect_uris")
    private Set<String> registeredRedirectUris = Collections.emptySet();

    @JsonProperty("access_token_validity")
    private Integer accessTokenValiditySeconds;

    @JsonProperty("refresh_token_validity")
    private Integer refreshTokenValiditySeconds;

    @JsonProperty("id_token_validity")
    private Integer idTokenValiditySeconds;

    @JsonProperty("jwks")
    private String jwks;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("application_type")
    private String applicationType;

    @JsonProperty("subject_type")
    private String subjectType;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("response_types")
    private Set<String> responseTypes = Collections.emptySet();

    @JsonProperty("jwt_signed_response_alg")
    private String jwtSignAlgorithm;

    @JsonProperty("jwt_encrypted_response_alg")
    private String jwtEncAlgorithm;

    @JsonProperty("jwt_encrypted_response_method")
    private String jwtEncMethod;

    @JsonProperty("userinfo_signed_response_alg")
    private String userinfoSignAlgorithm;

    @JsonProperty("userinfo_encrypted_response_alg")
    private String userinfoEncAlgorithm;

    @JsonProperty("userinfo_encrypted_response_method")
    private String userinfoEncMethod;

    @JsonProperty("id_token_signed_response_alg")
    private String idTokenSignAlgorithm;

    @JsonProperty("id_token_encrypted_response_alg")
    private String idTokenEncAlgorithm;

    @JsonProperty("id_token_encrypted_response_method")
    private String idTokenEncMethod;

    @JsonProperty("request_signed_response_alg")
    private String requestSignAlgorithm;

    @JsonProperty("request_encrypted_response_alg")
    private String requestEncAlgorithm;

    @JsonProperty("request_encrypted_response_method")
    private String requestEncMethod;

    @JsonProperty("token_endpoint_auth_signing_alg")
    private String tokenEndpointAuthSignAlgorithm;

    @JsonIgnore
    private String realm;

    @JsonIgnore
    private boolean idTokenClaims = false;

    @JsonIgnore
    private boolean firstParty = false;

    @JsonIgnore
    private boolean refreshTokenRotation = false;

    @JsonIgnore
    private Map<String, Object> additionalInformation = new LinkedHashMap<String, Object>();

    // hooks
    @JsonIgnore
    private Map<String, String> hookFunctions;

    @JsonIgnore
    private Map<String, String> hookWebUrls;

    @JsonIgnore
    private String hookUniqueSpaces;

    public OAuth2ClientDetails() {}

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @JsonIgnore
    public boolean isSecretRequired() {
        return this.clientSecret != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.scope = scope;
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Set<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    public Set<String> getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(Set<String> authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
    }

    public Set<String> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(Set<String> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public Set<String> getRegisteredRedirectUris() {
        return registeredRedirectUris;
    }

    public void setRegisteredRedirectUris(Set<String> registeredRedirectUris) {
        this.registeredRedirectUris = registeredRedirectUris;
    }

    public Integer getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public Integer getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public Integer getIdTokenValiditySeconds() {
        return idTokenValiditySeconds;
    }

    public void setIdTokenValiditySeconds(Integer idTokenValiditySeconds) {
        this.idTokenValiditySeconds = idTokenValiditySeconds;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getJwtSignAlgorithm() {
        return jwtSignAlgorithm;
    }

    public void setJwtSignAlgorithm(String jwtSignAlgorithm) {
        this.jwtSignAlgorithm = jwtSignAlgorithm;
    }

    public String getJwtEncAlgorithm() {
        return jwtEncAlgorithm;
    }

    public void setJwtEncAlgorithm(String jwtEncAlgorithm) {
        this.jwtEncAlgorithm = jwtEncAlgorithm;
    }

    public String getJwtEncMethod() {
        return jwtEncMethod;
    }

    public void setJwtEncMethod(String jwtEncMethod) {
        this.jwtEncMethod = jwtEncMethod;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public boolean isIdTokenClaims() {
        return idTokenClaims;
    }

    public void setIdTokenClaims(boolean idTokenClaims) {
        this.idTokenClaims = idTokenClaims;
    }

    public boolean isFirstParty() {
        return firstParty;
    }

    public void setFirstParty(boolean firstParty) {
        this.firstParty = firstParty;
    }

    public boolean isRefreshTokenRotation() {
        return refreshTokenRotation;
    }

    public void setRefreshTokenRotation(boolean refreshTokenRotation) {
        this.refreshTokenRotation = refreshTokenRotation;
    }

    public Set<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(Set<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public Map<String, Object> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, Object> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    public Map<String, String> getHookWebUrls() {
        return hookWebUrls;
    }

    public void setHookWebUrls(Map<String, String> hookWebUrls) {
        this.hookWebUrls = hookWebUrls;
    }

    public String getHookUniqueSpaces() {
        return hookUniqueSpaces;
    }

    public void setHookUniqueSpaces(String hookUniqueSpaces) {
        this.hookUniqueSpaces = hookUniqueSpaces;
    }

    @Override
    public boolean isScoped() {
        return this.scope != null && !this.scope.isEmpty();
    }

    @Override
    public Set<String> getRegisteredRedirectUri() {
        return getRegisteredRedirectUris();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAutoApprove(String scope) {
        // we use firstParty to mark internal clients with autoApprove
        return false;
    }
}
