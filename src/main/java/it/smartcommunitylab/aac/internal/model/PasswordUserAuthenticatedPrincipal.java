package it.smartcommunitylab.aac.internal.model;

import org.springframework.security.core.CredentialsContainer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.SystemKeys;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordUserAuthenticatedPrincipal extends InternalUserAuthenticatedPrincipal
        implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    public PasswordUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, userId, username);
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }

}
