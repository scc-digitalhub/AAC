package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.events.UserAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Map;

public class SpidUserAuthenticationFailureEvent extends UserAuthenticationFailureEvent {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    // TODO evaluate adding fields for error and message

    public SpidUserAuthenticationFailureEvent(
        String authority,
        String provider,
        String realm,
        Authentication authentication,
        SpidAuthenticationException exception
    ) {
        super(authority, provider, realm, null, authentication, exception);

    }

    @Override
    public Map<String, Serializable> exportException() {
        Map<String, Serializable> data = super.exportException();

        SpidAuthenticationException ex = (SpidAuthenticationException) getException();
        data.put("error", ex.getError().name());
        data.put("errorCode", ex.getError().getValue());
        data.put("saml2Response", ex.getSaml2Response());

        return data;
    }
}
