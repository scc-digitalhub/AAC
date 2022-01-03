package it.smartcommunitylab.aac.oauth.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.oauth.model.EncryptionMethod;
import it.smartcommunitylab.aac.oauth.model.JWEAlgorithm;
import it.smartcommunitylab.aac.oauth.model.JWSAlgorithm;
import it.smartcommunitylab.aac.oauth.model.ResponseType;

/*
 * Additional configuration holder
 * 
 * Store extra configuration (optional, non standard etc)
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2ClientAdditionalConfig implements Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("response_types")
    private Set<ResponseType> responseTypes;

    // access token jwt config
    @JsonProperty("jwt_sign_alg")
    private JWSAlgorithm jwtSignAlgorithm;
    @JsonProperty("jwt_enc_alg")
    private JWEAlgorithm jwtEncAlgorithm;
    @JsonProperty("jwt_enc_method")
    private EncryptionMethod jwtEncMethod;

    // id token jwt config
    @JsonProperty("id_token_signed_response_alg")
    private JWSAlgorithm idTokenSignAlgorithm;
    @JsonProperty("id_token_encrypted_response_alg")
    private JWEAlgorithm idTokenEncAlgorithm;
    @JsonProperty("id_token_encrypted_response_enc")
    private EncryptionMethod idTokenEncMethod;

    // userinfo jwt config
    @JsonProperty("userinfo_signed_response_alg")
    private JWSAlgorithm userinfoSignAlgorithm;
    @JsonProperty("userinfo_encrypted_response_alg")
    private JWEAlgorithm userinfoEncAlgorithm;
    @JsonProperty("userinfo_encrypted_response_enc")
    private EncryptionMethod userinfoEncMethod;

    // requestobj jwt config
    @JsonProperty("request_object_signing_alg")
    private JWSAlgorithm requestobjSignAlgorithm;
    @JsonProperty("request_object_encryption_alg")
    private JWEAlgorithm requestobjEncAlgorithm;
    @JsonProperty("request_object_encryption_enc")
    private EncryptionMethod requestobjEncMethod;

    // token endpoint jwt auth
    @JsonProperty("token_endpoint_auth_signing_alg")
    private JWSAlgorithm tokenEndpointAuthSignAlgorithm;

    @JsonProperty("default_max_age")
    private Integer defaultMaxAge;

    @JsonProperty("initiate_login_uri")
    private String initiateLoginUri;

    @JsonProperty("request_uris")
    private Set<String> requestUris;

    @SuppressWarnings("unchecked")
    public static Map<String, Serializable> read(String additionalConfiguration) {
        try {
            return mapper.readValue(additionalConfiguration, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static OAuth2ClientAdditionalConfig convert(Map<String, Serializable> map) {
        return mapper.convertValue(map, OAuth2ClientAdditionalConfig.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Serializable> toMap() throws IllegalArgumentException {
        try {
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            return mapper.convertValue(this, HashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public Set<ResponseType> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(Set<ResponseType> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public JWSAlgorithm getJwtSignAlgorithm() {
        return jwtSignAlgorithm;
    }

    public void setJwtSignAlgorithm(JWSAlgorithm jwtSignAlgorithm) {
        this.jwtSignAlgorithm = jwtSignAlgorithm;
    }

    public JWEAlgorithm getJwtEncAlgorithm() {
        return jwtEncAlgorithm;
    }

    public void setJwtEncAlgorithm(JWEAlgorithm jwtEncAlgorithm) {
        this.jwtEncAlgorithm = jwtEncAlgorithm;
    }

    public EncryptionMethod getJwtEncMethod() {
        return jwtEncMethod;
    }

    public void setJwtEncMethod(EncryptionMethod jwtEncMethod) {
        this.jwtEncMethod = jwtEncMethod;
    }

    public JWSAlgorithm getIdTokenSignAlgorithm() {
        return idTokenSignAlgorithm;
    }

    public void setIdTokenSignAlgorithm(JWSAlgorithm idTokenSignAlgorithm) {
        this.idTokenSignAlgorithm = idTokenSignAlgorithm;
    }

    public JWEAlgorithm getIdTokenEncAlgorithm() {
        return idTokenEncAlgorithm;
    }

    public void setIdTokenEncAlgorithm(JWEAlgorithm idTokenEncAlgorithm) {
        this.idTokenEncAlgorithm = idTokenEncAlgorithm;
    }

    public EncryptionMethod getIdTokenEncMethod() {
        return idTokenEncMethod;
    }

    public void setIdTokenEncMethod(EncryptionMethod idTokenEncMethod) {
        this.idTokenEncMethod = idTokenEncMethod;
    }

    public JWSAlgorithm getUserinfoSignAlgorithm() {
        return userinfoSignAlgorithm;
    }

    public void setUserinfoSignAlgorithm(JWSAlgorithm userinfoSignAlgorithm) {
        this.userinfoSignAlgorithm = userinfoSignAlgorithm;
    }

    public JWEAlgorithm getUserinfoEncAlgorithm() {
        return userinfoEncAlgorithm;
    }

    public void setUserinfoEncAlgorithm(JWEAlgorithm userinfoEncAlgorithm) {
        this.userinfoEncAlgorithm = userinfoEncAlgorithm;
    }

    public EncryptionMethod getUserinfoEncMethod() {
        return userinfoEncMethod;
    }

    public void setUserinfoEncMethod(EncryptionMethod userinfoEncMethod) {
        this.userinfoEncMethod = userinfoEncMethod;
    }

    public JWSAlgorithm getRequestobjSignAlgorithm() {
        return requestobjSignAlgorithm;
    }

    public void setRequestobjSignAlgorithm(JWSAlgorithm requestobjSignAlgorithm) {
        this.requestobjSignAlgorithm = requestobjSignAlgorithm;
    }

    public JWEAlgorithm getRequestobjEncAlgorithm() {
        return requestobjEncAlgorithm;
    }

    public void setRequestobjEncAlgorithm(JWEAlgorithm requestobjEncAlgorithm) {
        this.requestobjEncAlgorithm = requestobjEncAlgorithm;
    }

    public EncryptionMethod getRequestobjEncMethod() {
        return requestobjEncMethod;
    }

    public void setRequestobjEncMethod(EncryptionMethod requestobjEncMethod) {
        this.requestobjEncMethod = requestobjEncMethod;
    }

    public JWSAlgorithm getTokenEndpointAuthSignAlgorithm() {
        return tokenEndpointAuthSignAlgorithm;
    }

    public void setTokenEndpointAuthSignAlgorithm(JWSAlgorithm tokenEndpointAuthSignAlgorithm) {
        this.tokenEndpointAuthSignAlgorithm = tokenEndpointAuthSignAlgorithm;
    }

    public Integer getDefaultMaxAge() {
        return defaultMaxAge;
    }

    public void setDefaultMaxAge(Integer defaultMaxAge) {
        this.defaultMaxAge = defaultMaxAge;
    }

    public String getInitiateLoginUri() {
        return initiateLoginUri;
    }

    public void setInitiateLoginUri(String initiateLoginUri) {
        this.initiateLoginUri = initiateLoginUri;
    }

    public Set<String> getRequestUris() {
        return requestUris;
    }

    public void setRequestUris(Set<String> requestUris) {
        this.requestUris = requestUris;
    }

}
