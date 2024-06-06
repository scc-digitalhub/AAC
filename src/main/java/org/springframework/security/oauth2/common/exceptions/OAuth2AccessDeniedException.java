package org.springframework.security.oauth2.common.exceptions;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

/**
 * When access is denied we usually want a 403, but we want the same treatment
 * as all the other OAuth2Exception types, so this is not a Spring Security
 * AccessDeniedException.
 *
 * <p>
 *
 * @author Ryan Heaton
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class OAuth2AccessDeniedException extends OAuth2Exception {

    public OAuth2AccessDeniedException() {
        super("OAuth2 access denied.");
    }

    public OAuth2AccessDeniedException(String msg) {
        super(msg);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "access_denied";
    }

    @Override
    public int getHttpErrorCode() {
        return 403;
    }
}
