package it.smartcommunitylab.aac.oauth.model;

import java.util.Date;
import java.util.Set;

import org.springframework.security.oauth2.provider.client.Jackson2ArrayOrStringDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.smartcommunitylab.aac.repository.StringArraySerializer;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientRegistration {
    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("client_id_issued_at")
    private long clientIdIssuedAt;

    @JsonProperty("client_secret_expires_at")
    private long clientSecretExpiresAt;

    @JsonProperty("client_name")
    private String name;

    @JsonProperty("redirect_uris")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    private Set<String> redirectUris;

    @JsonProperty("scope")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    @JsonSerialize(using = StringArraySerializer.class)
    private Set<String> scope;

    @JsonProperty("resource_ids")
    private Set<String> resourceIds;

    @JsonProperty("response_types")
    private Set<String> responseTypes;

    @JsonProperty("grant_types")
    private Set<String> grantType;

    @JsonProperty("application_type")
    private String applicationType;

    @JsonProperty("subject_type")
    private String subjectType;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("token_endpoint_auth_method")
    private Set<String> authenticationMethods;

    @JsonProperty("token_endpoint_auth_signing_alg")
    private String tokenEndpointAuthSignAlgorithm;

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

    @JsonProperty("contacts")
    private Set<String> contacts;

    @JsonProperty("logo_uri")
    private String logoUri;

    @JsonProperty("client_uri")
    private String clientUri;

    @JsonProperty("policy_uri")
    private String policyUri;

    @JsonProperty("tos_uri")
    private String tosUri;

    @JsonProperty("sector_identifier_uri")
    private String sectorIdentifierUri;

    @JsonProperty("software_id")
    private String softwareId;

    @JsonProperty("software_version")
    private String softwareVersion;

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

    @JsonProperty("request_object_signing_alg")
    private String requestobjSignAlgorithm;

    @JsonProperty("request_object_encryption_alg")
    private String requestobjEncAlgorithm;

    @JsonProperty("request_object_encryption_enc")
    private String requestobjEncMethod;

    @JsonProperty("request_uris")
    private Set<String> requestUris;

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

    public long getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(long clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public long getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(long clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
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

    public Set<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(Set<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public Set<String> getGrantType() {
        return grantType;
    }

    public void setGrantType(Set<String> grantType) {
        this.grantType = grantType;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Set<String> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(Set<String> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public String getTokenEndpointAuthSignAlgorithm() {
        return tokenEndpointAuthSignAlgorithm;
    }

    public void setTokenEndpointAuthSignAlgorithm(String tokenEndpointAuthSignAlgorithm) {
        this.tokenEndpointAuthSignAlgorithm = tokenEndpointAuthSignAlgorithm;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
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

    public Set<String> getContacts() {
        return contacts;
    }

    public void setContacts(Set<String> contacts) {
        this.contacts = contacts;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getClientUri() {
        return clientUri;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    public String getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    public String getTosUri() {
        return tosUri;
    }

    public void setTosUri(String tosUri) {
        this.tosUri = tosUri;
    }

    public String getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
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

    public String getUserinfoSignAlgorithm() {
        return userinfoSignAlgorithm;
    }

    public void setUserinfoSignAlgorithm(String userinfoSignAlgorithm) {
        this.userinfoSignAlgorithm = userinfoSignAlgorithm;
    }

    public String getUserinfoEncAlgorithm() {
        return userinfoEncAlgorithm;
    }

    public void setUserinfoEncAlgorithm(String userinfoEncAlgorithm) {
        this.userinfoEncAlgorithm = userinfoEncAlgorithm;
    }

    public String getUserinfoEncMethod() {
        return userinfoEncMethod;
    }

    public void setUserinfoEncMethod(String userinfoEncMethod) {
        this.userinfoEncMethod = userinfoEncMethod;
    }

    public String getIdTokenSignAlgorithm() {
        return idTokenSignAlgorithm;
    }

    public void setIdTokenSignAlgorithm(String idTokenSignAlgorithm) {
        this.idTokenSignAlgorithm = idTokenSignAlgorithm;
    }

    public String getIdTokenEncAlgorithm() {
        return idTokenEncAlgorithm;
    }

    public void setIdTokenEncAlgorithm(String idTokenEncAlgorithm) {
        this.idTokenEncAlgorithm = idTokenEncAlgorithm;
    }

    public String getIdTokenEncMethod() {
        return idTokenEncMethod;
    }

    public void setIdTokenEncMethod(String idTokenEncMethod) {
        this.idTokenEncMethod = idTokenEncMethod;
    }

    public String getRequestobjSignAlgorithm() {
        return requestobjSignAlgorithm;
    }

    public void setRequestobjSignAlgorithm(String requestobjSignAlgorithm) {
        this.requestobjSignAlgorithm = requestobjSignAlgorithm;
    }

    public String getRequestobjEncAlgorithm() {
        return requestobjEncAlgorithm;
    }

    public void setRequestobjEncAlgorithm(String requestobjEncAlgorithm) {
        this.requestobjEncAlgorithm = requestobjEncAlgorithm;
    }

    public String getRequestobjEncMethod() {
        return requestobjEncMethod;
    }

    public void setRequestobjEncMethod(String requestobjEncMethod) {
        this.requestobjEncMethod = requestobjEncMethod;
    }

    public Set<String> getRequestUris() {
        return requestUris;
    }

    public void setRequestUris(Set<String> requestUris) {
        this.requestUris = requestUris;
    }

}
