package it.smartcommunitylab.aac.oauth.event;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.Assert;

public class TokenGrantEvent extends OAuth2Event {

    private final OAuth2AccessToken token;
    private final OAuth2Authentication authentication;

    public TokenGrantEvent(OAuth2AccessToken token, OAuth2Authentication authentication) {
        super(authentication.getOAuth2Request());
        Assert.notNull(token, "token can not be null");
        this.token = token;
        this.authentication = authentication;
    }

    public OAuth2AccessToken getToken() {
        return token;
    }

    public OAuth2Authentication getAuthentication() {
        return authentication;
    }

}
