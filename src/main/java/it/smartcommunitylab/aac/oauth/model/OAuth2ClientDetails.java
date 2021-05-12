package it.smartcommunitylab.aac.oauth.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;

import org.springframework.security.oauth2.provider.client.BaseClientDetails;

public class OAuth2ClientDetails extends BaseClientDetails {

    private String realm;

    private String name;

    private String tokenType;

    private Set<String> authenticationMethods = Collections.emptySet();

    private boolean firstParty = false;

    private String jwks;

    private String jwksUri;

    private String jwtSignAlgorithm;

    private String jwtEncAlgorithm;

    private String jwtEncMethod;

    // hooks
    private Map<String, String> hookFunctions;
    private Map<String, String> hookWebUrls;
    private String hookUniqueSpaces;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isFirstParty() {
        return firstParty;
    }

    public void setFirstParty(boolean firstParty) {
        this.firstParty = firstParty;
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

}
