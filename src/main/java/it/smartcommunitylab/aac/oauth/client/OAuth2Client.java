package it.smartcommunitylab.aac.oauth.client;

import java.io.Serializable;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientSecret;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;

public class OAuth2Client extends BaseClient {
    public static final String CLIENT_TYPE = SystemKeys.CLIENT_TYPE_OAUTH2;

    @JsonIgnore
    private ClientSecret clientSecret;

    private Set<AuthorizationGrantType> authorizedGrantTypes;
    private Set<String> redirectUris;

    private TokenType tokenType;

    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;

    private JWSAlgorithm jwtSignAlgorithm;
    private JWEAlgorithm jwtEncAlgorithm;
    private EncryptionMethod jwtEncMethod;
    private String jwks;
    private String jwksUri;

    @JsonUnwrapped
    private OAuth2ClientInfo additionalInformation;

    public OAuth2Client(@JsonProperty("realm") String realm, @JsonProperty("clientId") String clientId) {
        super(realm, clientId);
        authorizedGrantTypes = new HashSet<>();
        redirectUris = new HashSet<>();
    }

    @Override
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

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
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

    public OAuth2ClientInfo getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(OAuth2ClientInfo additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfigurationMap() {
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

            map.put("tokenType", tokenType);
            map.put("accessTokenValidity", accessTokenValidity);
            map.put("refreshTokenValidity", refreshTokenValidity);

            if (jwtSignAlgorithm != null) {
                map.put("jwtSignAlgorithm", jwtSignAlgorithm.getName());
            }

            if (jwtEncMethod != null) {
                map.put("jwtEncMethod", jwtEncMethod.getName());
            }

            if (jwtEncAlgorithm != null) {
                map.put("jwtEncAlgorithm", jwtEncAlgorithm.getName());
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

    private static ObjectMapper mapper = new ObjectMapper();

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
        c.tokenType = (oauth.getTokenType() != null ? TokenType.valueOf(oauth.getTokenType()) : null);

        Set<String> authorizedGrantTypes = StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes());
        c.authorizedGrantTypes = authorizedGrantTypes.stream()
                .map(a -> AuthorizationGrantType.parse(a)).collect(Collectors.toSet());

        c.redirectUris.addAll(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));

        c.accessTokenValidity = oauth.getAccessTokenValidity();
        c.refreshTokenValidity = oauth.getRefreshTokenValidity();

        c.jwtSignAlgorithm = (oauth.getJwtSignAlgorithm() != null ? JWSAlgorithm.parse(oauth.getJwtSignAlgorithm())
                : null);
        c.jwtEncAlgorithm = (oauth.getJwtEncAlgorithm() != null ? JWEAlgorithm.parse(oauth.getJwtEncAlgorithm())
                : null);
        c.jwtEncMethod = (oauth.getJwtEncMethod() != null ? EncryptionMethod.parse(oauth.getJwtEncMethod()) : null);
        c.jwks = oauth.getJwks();
        c.jwksUri = oauth.getJwksUri();

        Map<String, String> map = oauth.getAdditionalInformation();
        if (map != null) {
            c.additionalInformation = OAuth2ClientInfo.convert(map);
        }

        return c;
    }

}
