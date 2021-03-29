package it.smartcommunitylab.aac.oauth.client;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.nimbusds.jose.jwk.JWKSet;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientSecret;
import it.smartcommunitylab.aac.oauth.model.EncryptionMethod;
import it.smartcommunitylab.aac.oauth.model.JWEAlgorithm;
import it.smartcommunitylab.aac.oauth.model.JWSAlgorithm;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;

public class OAuth2Client extends BaseClient {
    public static final String CLIENT_TYPE = SystemKeys.CLIENT_TYPE_OAUTH2;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    @JsonIgnore
    private ClientSecret clientSecret;

    private Set<AuthorizationGrantType> authorizedGrantTypes;
    private Set<String> redirectUris;

    private TokenType tokenType;
    private Set<AuthenticationMethod> authenticationMethods;

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

    public OAuth2Client(@JsonProperty("realm") String realm, @JsonProperty("clientId") String clientId) {
        super(realm, clientId);
        authorizedGrantTypes = new HashSet<>();
        redirectUris = new HashSet<>();
        authenticationMethods = new HashSet<>();
        autoApproveScopes = new HashSet<>();
    }

    @Override
    @JsonIgnore
    public String getType() {
        return SystemKeys.CLIENT_TYPE_OAUTH2;
    }

    @JsonProperty("clientSecret")
    public String getSecret() {
        return (clientSecret != null ? clientSecret.getCredentials() : null);
    }

    @JsonProperty("clientSecret")
    public void setSecret(String secret) {
        this.clientSecret = new ClientSecret(secret);
    }

    public ClientSecret getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(ClientSecret clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Set<AuthorizationGrantType> getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(Set<AuthorizationGrantType> authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
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

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
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

    public OAuth2ClientInfo getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(OAuth2ClientInfo additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfigurationMap() {
        return getJacksonConfigurationMap();
    }

    @JsonIgnore
    public Map<String, Serializable> getJacksonConfigurationMap() {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @JsonIgnore
    public Map<String, Serializable> getStaticConfigurationMap() {
        // TODO fix naming to follow RFC standard and return TLD elements
        // will break converter
        try {
            Map<String, Serializable> map = new HashMap<>();
            // expose only specific properties

            if (clientSecret != null) {
                map.put("clientSecret", clientSecret.getCredentials());
            }

            // TODO rewrite properly
            if (authorizedGrantTypes != null) {
                map.put("authorizedGrantTypes", authorizedGrantTypes.toArray(new AuthorizationGrantType[0]));
            } else {
                map.put("authorizedGrantTypes", new AuthorizationGrantType[0]);
            }

            if (redirectUris != null) {
                map.put("redirectUris", redirectUris.toArray(new String[0]));
            } else {
                map.put("redirectUris", new String[0]);
            }

            if (authenticationMethods != null) {
                map.put("authenticationMethods", authenticationMethods.toArray(new AuthenticationMethod[0]));
            } else {
                map.put("authenticationMethods", new AuthenticationMethod[0]);
            }

            map.put("tokenType", tokenType);

            map.put("firstParty", firstParty);
            if (autoApproveScopes != null) {
                map.put("autoApproveScopes", autoApproveScopes.toArray(new String[0]));
            } else {
                map.put("autoApproveScopes", new String[0]);
            }

            map.put("accessTokenValidity", accessTokenValidity);
            map.put("refreshTokenValidity", refreshTokenValidity);

            if (jwtSignAlgorithm != null) {
                map.put("jwtSignAlgorithm", jwtSignAlgorithm.getValue());
            }

            if (jwtEncMethod != null) {
                map.put("jwtEncMethod", jwtEncMethod.getValue());
            }

            if (jwtEncAlgorithm != null) {
                map.put("jwtEncAlgorithm", jwtEncAlgorithm.getValue());
            }

            map.put("jwks", jwks);
            map.put("jwksUri", jwksUri);

            // filter null
            return map.entrySet().stream().filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        } catch (Exception e) {
            return null;
        }

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(OAuth2Client.class);
    }

    private String toJson() throws JsonProcessingException {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.writeValueAsString(this);
    }

    /*
     * builder
     * 
     * TODO add a proper builder to use with jackson
     */

    public static OAuth2Client convert(Map<String, Serializable> map) {
        // use mapper
        // TODO custom mapping, see getConfigurationMap
        return mapper.convertValue(map, OAuth2Client.class);
    }

    public static OAuth2Client from(ClientEntity client, OAuth2ClientEntity oauth) {
        OAuth2Client c = new OAuth2Client(client.getRealm(), client.getClientId());
        c.setName(client.getName());
        c.setDescription(client.getDescription());

        c.setScopes(StringUtils.commaDelimitedListToSet(client.getScopes()));
        c.setProviders(StringUtils.commaDelimitedListToSet(client.getProviders()));
        c.setHookFunctions(client.getHookFunctions());

        // map attributes
        c.clientSecret = (oauth.getClientSecret() != null ? new ClientSecret(oauth.getClientSecret()) : null);
        c.tokenType = (oauth.getTokenType() != null ? TokenType.parse(oauth.getTokenType()) : null);

        Set<String> authorizedGrantTypes = StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes());
        c.authorizedGrantTypes = authorizedGrantTypes.stream()
                .map(a -> AuthorizationGrantType.parse(a))
                .filter(a -> (a != null))
                .collect(Collectors.toSet());

        Set<String> authenticationMethods = StringUtils.commaDelimitedListToSet(oauth.getAuthenticationMethods());
        c.authenticationMethods = authenticationMethods.stream()
                .map(a -> AuthenticationMethod.parse(a))
                .filter(a -> (a != null))
                .collect(Collectors.toSet());

        c.redirectUris.addAll(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));
        c.setResourceIds(StringUtils.commaDelimitedListToSet(oauth.getResourceIds()));

        c.setFirstParty(oauth.isFirstParty());
        c.setAutoApproveScopes(StringUtils.commaDelimitedListToSet(oauth.getAutoApproveScopes()));

        c.accessTokenValidity = oauth.getAccessTokenValidity();
        c.refreshTokenValidity = oauth.getRefreshTokenValidity();

        c.jwtSignAlgorithm = (oauth.getJwtSignAlgorithm() != null ? JWSAlgorithm.parse(oauth.getJwtSignAlgorithm())
                : null);
        c.jwtEncAlgorithm = (oauth.getJwtEncAlgorithm() != null ? JWEAlgorithm.parse(oauth.getJwtEncAlgorithm())
                : null);
        c.jwtEncMethod = (oauth.getJwtEncMethod() != null ? EncryptionMethod.parse(oauth.getJwtEncMethod()) : null);
        try {
            c.jwks = (StringUtils.hasText(oauth.getJwks()) ? JWKSet.parse(oauth.getJwks()) : null);
        } catch (ParseException e) {
            c.jwks = null;
        }
        c.jwksUri = oauth.getJwksUri();

        Map<String, String> map = oauth.getAdditionalInformation();
        if (map != null) {
            c.additionalInformation = OAuth2ClientInfo.convert(map);
        }

        return c;
    }

}
