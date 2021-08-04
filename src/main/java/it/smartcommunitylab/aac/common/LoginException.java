package it.smartcommunitylab.aac.common;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationException;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticationException;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;

public class LoginException extends AuthenticationException {
    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private final String error;

    public LoginException(AuthenticationException e) {
        super(e.getMessage(), e.getCause());
        this.error = e.getClass().getSimpleName();
    }

    public LoginException(String error, AuthenticationException e) {
        super(e.getMessage(), e.getCause());
        Assert.hasText(error, "error can not be null");
        this.error = error;
    }

    public String getError() {
        return error;
    }

    /*
     * Static builder TODO move
     */

    public static LoginException translate(SpidAuthenticationException e) {
        return new LoginException(e.getError().getErrorCode(), e);
    }

    public static LoginException translate(SamlAuthenticationException e) {
        return new LoginException(e.getError().getErrorCode(), e);
    }
    
    public static LoginException translate(OIDCAuthenticationException e) {
        return new LoginException(e.getError().getErrorCode(), e);
    }
    
    public static LoginException translate(InternalAuthenticationException e) {
        return new LoginException(e.getError(), e);
    }

    public static LoginException translate(AuthenticationException e) {

        if (e instanceof SpidAuthenticationException) {
            return translate((SpidAuthenticationException) e);
        }

        if (e instanceof SamlAuthenticationException) {
            return translate((SamlAuthenticationException) e);
        }
        
        if (e instanceof OIDCAuthenticationException) {
            return translate((OIDCAuthenticationException) e);
        }

        
        if (e instanceof InternalAuthenticationException) {
            return translate((InternalAuthenticationException) e);
        }


        return new LoginException(e.getClass().getSimpleName(), e);

    }
}
