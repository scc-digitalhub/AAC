package it.smartcommunitylab.aac.password.model;

import org.springframework.security.core.CredentialsContainer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalPasswordUserAuthenticatedPrincipal extends InternalUserAuthenticatedPrincipal
        implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    public InternalPasswordUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, userId, username);
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }

}
