package it.smartcommunitylab.aac.password.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import org.springframework.security.core.CredentialsContainer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalPasswordUserAuthenticatedPrincipal
    extends InternalUserAuthenticatedPrincipal
    implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PRINCIPAL + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_PASSWORD;

    public InternalPasswordUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, userId, username);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }
}
