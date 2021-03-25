package it.smartcommunitylab.aac.oauth.persistence;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.smartcommunitylab.aac.repository.HashMapConverter;

@Entity
@Table(name = "oauth2_clients")
@EntityListeners(AuditingEntityListener.class)
public class OAuth2ClientEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "client_id", unique = true)
    private String clientId;

    /*
     * client metadata as of RFC 7591
     */
    // - client_id -> client_id
    // - client_secret -> client_secret
    // - redirect_uris -> redirectUri (array of strings)
    // - token_endpoint_auth_method -> authenticationScheme (string/array)
    // - grant_types -> authorizedGrantTypes (array of strings)
    // - response_types -> TODO (array of strings)
    // - client_name -> name (string)
    // - client_uri -> TODO (string)
    // - logo_uri -> TODO (string)
    // - scope -> scope (string space separated)
    // - contacts -> TODO (array of strings, e.g., emails)
    // - tos_uri -> TODO (string)
    // - policy_uri -> TODO (string)
    // - jwks_uri -> jwksUri
    // - jwks -> jwks
    // - software_id -> TODO (string)
    // - software_version -> TODO (string)
    // - client_id_issued_at -> TODO (seconds)
    // - client_secret_expires_at -> TODO (seconds)

    // client details
    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Column(name = "grant_types")
    private String authorizedGrantTypes;

    @Column(name = "redirect_uris", columnDefinition = "LONGTEXT")
    private String redirectUris;

    @Column(name = "authentication_scheme")
    private String authenticationScheme;

    /*
     * OAuth2 flows - related info
     * 
     * we persist these here to feed the clientDetailsService without the need to
     * access additionalInfo
     */
    @Column(name = "access_token_validity")
    private Integer accessTokenValidity;

    @Column(name = "refresh_token_validity")
    private Integer refreshTokenValidity;

    @Column(name = "token_type")
    private String tokenType;

    @Column(name = "jwt_sign_algo")
    private String jwtSignAlgorithm;

    @Column(name = "jwt_enc_algo")
    private String jwtEncAlgorithm;

    @Column(name = "jwt_enc_method")
    private String jwtEncMethod;

    @Column(name = "jwks")
    private String jwks;

    @Column(name = "jwks_uri")
    private String jwksUri;

    @Lob
    @Column(name = "configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, String> additionalInformation;

    /*
     * first party clients won't require approval for in-realm resource consumption
     * (realm idp, realm identity, realm service etc)
     */
    @Column(name = "first_party")
    private boolean firstParty;

    // TODO evaluate registering resourceIds to be able to validate requests
    @Column(name = "resource_ids")
    private String resourceIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(String authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
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

    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    public void setAuthenticationScheme(String authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

    public String getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(String resourceIds) {
        this.resourceIds = resourceIds;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
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

    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, String> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public boolean isFirstParty() {
        return firstParty;
    }

    public void setFirstParty(boolean firstParty) {
        this.firstParty = firstParty;
    }

}
