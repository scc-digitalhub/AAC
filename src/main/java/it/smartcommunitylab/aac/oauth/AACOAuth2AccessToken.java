package it.smartcommunitylab.aac.oauth;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.util.Assert;

public class AACOAuth2AccessToken implements OAuth2AccessToken, Serializable {

    private static final long serialVersionUID = -5481174420095804102L;

    private final String value;

    // TODO evaluate if needed
    private String realm;

    private Date expiration;

    private String tokenType = BEARER_TYPE.toLowerCase();

    private OAuth2RefreshToken refreshToken;

    private Set<String> scope;

    private Map<String, Object> additionalInformation = Collections.emptyMap();

    private Date issuedAt;

    private Date notBeforeTime;

    private Map<String, Serializable> claims;

    /**
     * Create an access token from the value provided.
     */
    public AACOAuth2AccessToken(String value) {
        Assert.hasText(value, "token can not be empty or null");
        this.value = value;

        Date now = new Date();
        setIssuedAt(now);
        setNotBeforeTime(now);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     */
    @SuppressWarnings("unused")
    private AACOAuth2AccessToken() {
        this((String) null);
    }

    /**
     * Copy constructor for access token.
     * 
     * @param accessToken
     */
    public AACOAuth2AccessToken(AACOAuth2AccessToken accessToken) {
        this(accessToken.getValue());

        setRefreshToken(accessToken.getRefreshToken());
        setExpiration(accessToken.getExpiration());
        setScope(accessToken.getScope());
        setTokenType(accessToken.getTokenType());

        // copy dates
        setIssuedAt(accessToken.getIssuedAt());
        setNotBeforeTime(accessToken.getNotBeforeTime());

        // copy claims
        setClaims(accessToken.getClaims());

        // additional info
        setAdditionalInformation(accessToken.getAdditionalInformation());

    }

    public AACOAuth2AccessToken(OAuth2AccessToken accessToken) {
        this(accessToken.getValue());

        setRefreshToken(accessToken.getRefreshToken());
        setExpiration(accessToken.getExpiration());
        setScope(accessToken.getScope());
        setTokenType(accessToken.getTokenType());

        if (accessToken instanceof AACOAuth2AccessToken) {
            AACOAuth2AccessToken token = (AACOAuth2AccessToken) accessToken;
            // copy dates
            setIssuedAt(token.getIssuedAt());
            setNotBeforeTime(token.getNotBeforeTime());

            // copy claims
            setClaims(token.getClaims());
        }
        // additional info
        setAdditionalInformation(accessToken.getAdditionalInformation());

    }

    public String getValue() {
        return value;
    }

    public int getExpiresIn() {
        return expiration != null ? Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L)
                .intValue() : 0;
    }

    protected void setExpiresIn(int delta) {
        setExpiration(new Date(System.currentTimeMillis() + delta));
    }

    public boolean isExpired() {
        return expiration != null && expiration.before(new Date());
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public OAuth2RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(OAuth2RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.scope = scope;
    }

    public Map<String, Object> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, Object> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public Map<String, Serializable> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Serializable> claims) {
        this.claims = claims;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Date getNotBeforeTime() {
        return notBeforeTime;
    }

    public void setNotBeforeTime(Date notBeforeTime) {
        this.notBeforeTime = notBeforeTime;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public String toString() {
        return "AACOAuth2AccessToken [value=" + value + ", expiration=" + expiration + ", tokenType=" + tokenType
                + ", scope=" + scope + ", issuedAt=" + issuedAt + "]";
    }

}
