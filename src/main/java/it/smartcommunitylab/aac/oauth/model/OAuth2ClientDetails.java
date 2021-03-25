package it.smartcommunitylab.aac.oauth.model;

import java.util.Collections;
import java.util.Set;

import javax.persistence.Column;

import org.springframework.security.oauth2.provider.client.BaseClientDetails;

public class OAuth2ClientDetails extends BaseClientDetails {

    private String realm;

    private String tokenType;

    private Set<String> authenticationScheme = Collections.emptySet();

    private String jwks;

    private String jwksUri;

    private String jwtSignAlgorithm;

    private String jwtEncAlgorithm;

    private String jwtEncMethod;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Set<String> getAuthenticationScheme() {
        return authenticationScheme;
    }

    public void setAuthenticationScheme(Set<String> authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
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

}
