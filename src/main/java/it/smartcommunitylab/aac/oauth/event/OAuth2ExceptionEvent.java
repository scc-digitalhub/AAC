package it.smartcommunitylab.aac.oauth.event;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.Assert;

public class OAuth2ExceptionEvent extends OAuth2Event {

    private final OAuth2Exception exception;
    private String realm;

    public OAuth2ExceptionEvent(OAuth2Exception exception, OAuth2Authentication authentication) {
        super(authentication);
        Assert.notNull(exception, "exception can not be null");
        this.exception = exception;
    }

    public OAuth2Exception getException() {
        return exception;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

}