package it.smartcommunitylab.aac.saml.auth;

import java.io.Serializable;
import java.util.Map;

import org.springframework.security.core.Authentication;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.UserAuthenticationFailureEvent;

public class SamlUserAuthenticationFailureEvent extends UserAuthenticationFailureEvent {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    // TODO evaluate adding fields for error and message

    public SamlUserAuthenticationFailureEvent(
            String authority, String provider, String realm,
            Authentication authentication, SamlAuthenticationException exception) {
        super(authority, provider, realm, authentication, exception);

    }

    @Override
    public Map<String, Serializable> exportException() {
        Map<String, Serializable> data = super.exportException();

        SamlAuthenticationException ex = (SamlAuthenticationException) getException();
        data.put("error", ex.getError().getDescription());
        data.put("errorCode", ex.getError().getErrorCode());
        data.put("saml2Response", ex.getSaml2Response());

        return data;
    }

}
