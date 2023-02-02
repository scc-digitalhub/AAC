package it.smartcommunitylab.aac.webauthn.model;

import java.io.Serializable;
import java.util.Map;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class WebAuthnUserAuthenticatedPrincipal extends InternalUserAuthenticatedPrincipal
        implements CredentialsContainer {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PRINCIPAL + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_WEBAUTHN;

    private String userHandle;

    public WebAuthnUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm, userId, username);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> map = super.getAttributes();
        if (StringUtils.hasText(userHandle)) {
            map.put("userHandle", userHandle);
        }

        return map;
    }

    @Override
    public void setAccountAttributes(InternalUserAccount account) {
        if (account != null) {
            super.setAccountAttributes(account);

            userHandle = account.getUuid();
        }
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }

}
