package it.smartcommunitylab.aac.otp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import org.springframework.security.core.CredentialsContainer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalOtpUserAuthenticatedPrincipal
    extends InternalUserAuthenticatedPrincipal
    implements CredentialsContainer {

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PRINCIPAL + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_OTP;

    public InternalOtpUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        super(SystemKeys.AUTHORITY_OTP, provider, realm, userId, username);
    }

    public InternalOtpUserAuthenticatedPrincipal(InternalUserAccount account) {
        super(
            SystemKeys.AUTHORITY_OTP,
            account.getProvider(),
            account.getRealm(),
            account.getUserId(),
            account.getUsername()
        );
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public void eraseCredentials() {
        throw new UnsupportedOperationException("Unimplemented method 'eraseCredentials'");
    }
}
