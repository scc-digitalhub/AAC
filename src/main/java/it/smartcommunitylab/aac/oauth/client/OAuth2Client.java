package it.smartcommunitylab.aac.oauth.client;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;

public class OAuth2Client extends BaseClient {

    @NotBlank
    private String clientSecret;
    
    private Set<String> authorizedGrantTypes;
    private Set<String> redirectUris;

    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;

    private String jwtSignAlgorithm;
    private String jwtEncAlgorithm;
    private String jwtEncMethod;
    private String jwks;
    private String jwksUri;

    private OAuth2ClientInfo additionalInformation;

    public OAuth2Client(String realm, String clientId) {
        super(realm, clientId);
        authorizedGrantTypes = new HashSet<>();
        redirectUris = new HashSet<>();
    }

    @Override
    public String getType() {
        return SystemKeys.CLIENT_TYPE_OAUTH2;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Set<String> getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(Set<String> authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
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
    public Map<String, Serializable> getConfigurationMap() {
        // use mapper to translate config
        // TODO fix naming to follow RFC standard and return TLD elements
        try {
            String json = toJson();
            return Collections.singletonMap("oauth2", json);
        } catch (JsonProcessingException e) {
            return null;
        }

    }

    private String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        return mapper.writeValueAsString(this);

    }

    /*
     * builder
     */

    public static OAuth2Client from(ClientEntity client, OAuth2ClientEntity oauth) {
        OAuth2Client c = new OAuth2Client(client.getRealm(), client.getClientId());
        c.setName(client.getName());
        c.setDescription(client.getDescription());

        c.setScopes(StringUtils.commaDelimitedListToSet(client.getScopes()));
        c.setProviders(StringUtils.commaDelimitedListToSet(client.getProviders()));

        // map attributes
        c.clientSecret = oauth.getClientSecret();
        c.authorizedGrantTypes.addAll(StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes()));
        c.redirectUris.addAll(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));

        c.accessTokenValidity = oauth.getAccessTokenValidity();
        c.refreshTokenValidity = oauth.getRefreshTokenValidity();

        c.jwtSignAlgorithm = oauth.getJwtSignAlgorithm();
        c.jwtEncAlgorithm = oauth.getJwtEncAlgorithm();
        c.jwtEncMethod = oauth.getJwtEncMethod();
        c.jwks = oauth.getJwks();
        c.jwksUri = oauth.getJwksUri();

        Map<String, Object> map = OAuth2ClientInfo.read(oauth.getAdditionalInformation());
        if (map != null) {
            c.additionalInformation = OAuth2ClientInfo.convert(map);
        }

        return c;
    }

}
