package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractAccountService;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.spid.model.SpidEditableUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SpidAccountService extends AbstractAccountService<SpidUserAccount, SpidEditableUserAccount, SpidAccountServiceConfig, SpidIdentityProviderConfigMap> {

    public SpidAccountService(String providerId, UserAccountService<SpidUserAccount> accountService, SpidAccountServiceConfig config, String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, accountService, config, realm);
    }

    public SpidAccountService(String authority, String providerId, UserAccountService<SpidUserAccount> accountService, SpidAccountServiceConfig config, String realm) {
        super(authority, providerId, accountService, config, realm);
    }

    @Override
    public SpidEditableUserAccount getEditableAccount(String userId, String subject) throws NoSuchUserException {
        SpidUserAccount account = findAccount(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }
        return toEditableAccount(account);
    }

    public SpidEditableUserAccount toEditableAccount(SpidUserAccount account) {
        SpidEditableUserAccount ea = new SpidEditableUserAccount(
            getAuthority(),
            getProvider(),
            getRealm(),
            account.getUserId(),
            account.getUuid()
        );
        // TODO: review setters: I have no idea where this is used and what should or should not contains
        ea.setSubjectId(account.getSubjectId());
        ea.setUsername(account.getUsername());

        ea.setCreateDate(account.getCreateDate());
        ea.setModifiedDate(account.getModifiedDate());

        ea.setEmail(account.getEmail());
        ea.setName(account.getName());
        ea.setSurname(account.getSurname());

        return null;
    }
}
