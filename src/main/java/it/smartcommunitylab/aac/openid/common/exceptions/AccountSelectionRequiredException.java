package it.smartcommunitylab.aac.openid.common.exceptions;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import it.smartcommunitylab.aac.SystemKeys;

public class AccountSelectionRequiredException extends OAuth2Exception {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    public AccountSelectionRequiredException(String msg) {
        super(msg);
    }

    public AccountSelectionRequiredException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "account_selection_required";
    }

}
