package it.smartcommunitylab.aac.webauthn.service;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class WebAuthnUserHandleService {
    private final UserAccountService<InternalUserAccount> userAccountService;

    public WebAuthnUserHandleService(
            UserAccountService<InternalUserAccount> userAccountService) {
        Assert.notNull(userAccountService, "account service is mandatory");

        this.userAccountService = userAccountService;
    }

    public String getUserHandleForUsername(String repositoryId, String username) {
        InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }

        // use uuid as userHandle
        if (!StringUtils.hasText(account.getUuid())) {
            return null;
        }

        return account.getUuid();
    }

    public String getUsernameForUserHandle(String repositoryId, String userHandle) {
        // userHandle is uuid
        InternalUserAccount account = userAccountService.findAccountByUuid(userHandle);
        if (account == null) {
            return null;
        }

        if (!repositoryId.equals(account.getRepositoryId())) {
            return null;
        }

        return account.getUsername();
    }

}
