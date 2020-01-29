package it.smartcommunitylab.aac.oauth;

import java.util.Date;

import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class AACOAuth2AccessToken extends DefaultOAuth2AccessToken {

    private static final long serialVersionUID = -5481174420095804102L;

    private Date issuedAt;

    private Date notBeforeTime;

    /**
     * Create an access token from the value provided.
     */
    public AACOAuth2AccessToken(String value) {
        super(value);

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
    public AACOAuth2AccessToken(OAuth2AccessToken accessToken) {
        super(accessToken);

        Date now = new Date();
        setIssuedAt(now);
        setNotBeforeTime(now);

        if (accessToken instanceof AACOAuth2AccessToken) {
            // copy dates
            setIssuedAt(((AACOAuth2AccessToken) accessToken).getIssuedAt());
            setNotBeforeTime(((AACOAuth2AccessToken) accessToken).getNotBeforeTime());
        }
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

    @Override
    public String toString() {
        return "AACOAuth2AccessToken [issuedAt=" + issuedAt + ", notBeforeTime=" + notBeforeTime + "]";
    }

}
