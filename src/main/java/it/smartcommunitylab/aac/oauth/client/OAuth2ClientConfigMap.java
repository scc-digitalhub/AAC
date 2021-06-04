package it.smartcommunitylab.aac.oauth.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.nimbusds.jose.jwk.JWKSet;

import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.EncryptionMethod;
import it.smartcommunitylab.aac.oauth.model.JWEAlgorithm;
import it.smartcommunitylab.aac.oauth.model.JWSAlgorithm;
import it.smartcommunitylab.aac.oauth.model.TokenType;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2ClientConfigMap implements ConfigurableProperties {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private Set<AuthorizationGrantType> authorizedGrantTypes;
    private Set<String> redirectUris;

    private TokenType tokenType;
    private Set<AuthenticationMethod> authenticationMethods;

    private Boolean idTokenClaims;
    private Boolean firstParty;
    private Set<String> autoApproveScopes;

    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;

    private JWSAlgorithm jwtSignAlgorithm;
    private JWEAlgorithm jwtEncAlgorithm;
    private EncryptionMethod jwtEncMethod;
    private JWKSet jwks;
    private String jwksUri;

    @JsonUnwrapped
    private OAuth2ClientInfo additionalInformation;

    public OAuth2ClientConfigMap() {
        authorizedGrantTypes = new HashSet<>();
        redirectUris = new HashSet<>();
        authenticationMethods = new HashSet<>();
        autoApproveScopes = new HashSet<>();
        tokenType = TokenType.TOKEN_TYPE_JWT;

    }

    public OAuth2ClientConfigMap(Map<String, Serializable> props) {
        authorizedGrantTypes = new HashSet<>();
        redirectUris = new HashSet<>();
        authenticationMethods = new HashSet<>();
        autoApproveScopes = new HashSet<>();
        tokenType = TokenType.TOKEN_TYPE_JWT;

        setConfiguration(props);
    }

    public Set<AuthorizationGrantType> getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(Set<AuthorizationGrantType> authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public Set<AuthenticationMethod> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(Set<AuthenticationMethod> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public Boolean getIdTokenClaims() {
        return idTokenClaims;
    }

    public void setIdTokenClaims(Boolean idTokenClaims) {
        this.idTokenClaims = idTokenClaims;
    }

    public Boolean getFirstParty() {
        return firstParty;
    }

    public void setFirstParty(Boolean firstParty) {
        this.firstParty = firstParty;
    }

    public Set<String> getAutoApproveScopes() {
        return autoApproveScopes;
    }

    public void setAutoApproveScopes(Set<String> autoApproveScopes) {
        this.autoApproveScopes = autoApproveScopes;
    }

    public Integer getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(Integer accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public Integer getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    public void setRefreshTokenValidity(Integer refreshTokenValidity) {
        this.refreshTokenValidity = refreshTokenValidity;
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

    public JWKSet getJwks() {
        return jwks;
    }

    public void setJwks(JWKSet jwks) {
        this.jwks = jwks;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @JsonIgnore
    public OAuth2ClientInfo getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(OAuth2ClientInfo additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.ALWAYS);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OAuth2ClientConfigMap map = mapper.convertValue(props, OAuth2ClientConfigMap.class);
        this.authorizedGrantTypes = map.getAuthorizedGrantTypes();
        this.redirectUris = map.getRedirectUris();

        this.tokenType = map.getTokenType();
        this.authenticationMethods = map.getAuthenticationMethods();

        this.idTokenClaims = map.getIdTokenClaims();
        this.firstParty = map.getFirstParty();
        this.autoApproveScopes = map.getAutoApproveScopes();

        this.accessTokenValidity = map.getAccessTokenValidity();
        this.refreshTokenValidity = map.getRefreshTokenValidity();

        this.jwtSignAlgorithm = map.getJwtSignAlgorithm();
        this.jwtEncAlgorithm = map.getJwtEncAlgorithm();
        this.jwtEncMethod = map.getJwtEncMethod();
        this.jwks = map.getJwks();
        this.jwksUri = map.getJwksUri();

        // handle additional props
        if (map.getAdditionalInformation() != null) {
            this.additionalInformation = map.getAdditionalInformation();
        }
//        if(props != null && props.containsKey("additionalInformation")) {
//            this.additionalInformation = OAuth2ClientInfo.convert(props.get("additionalInformation"));
//        }

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(OAuth2ClientConfigMap.class);
    }
}
