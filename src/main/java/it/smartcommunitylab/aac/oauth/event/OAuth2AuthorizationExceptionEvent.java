package it.smartcommunitylab.aac.oauth.event;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.Assert;

public class OAuth2AuthorizationExceptionEvent extends OAuth2Event {

    private final OAuth2Exception exception;
    private final OAuth2Authentication authentication;

    public OAuth2AuthorizationExceptionEvent(AuthorizationRequest request, OAuth2Exception exception,
            OAuth2Authentication authentication) {
        super(request);
        Assert.notNull(exception, "exception can not be null");
        this.exception = exception;
        this.authentication = authentication;
    }

    public OAuth2Exception getException() {
        return exception;
    }

    public AuthorizationRequest getAuthorizationRequest() {
        return (AuthorizationRequest) super.getSource();
    }

    public OAuth2Authentication getAuthentication() {
        return authentication;
    }
}