package it.smartcommunitylab.aac.saml.auth;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;
import it.smartcommunitylab.aac.SystemKeys;

public class SamlAuthenticationException extends AuthenticationException {
    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private final Saml2Error error;
    private final String saml2Request;
    private final String saml2Response;

    public SamlAuthenticationException(Saml2Error error) {
        this(error, error.getDescription());
    }

    public SamlAuthenticationException(Saml2Error error, String message) {
        super(message);
        this.error = error;
        saml2Request = null;
        saml2Response = null;

    }

    public SamlAuthenticationException(Saml2Error error, String message, String saml2Request, String saml2Response) {
        super(message);
        this.error = error;
        this.saml2Request = saml2Request;
        this.saml2Response = saml2Response;
    }

    public Saml2Error getError() {
        return error;
    }

    public String getErrorMessage() {
        return "saml.error." + getError().getErrorCode();
    }

    public String getSaml2Request() {
        return saml2Request;
    }

    public String getSaml2Response() {
        return saml2Response;
    }

}
