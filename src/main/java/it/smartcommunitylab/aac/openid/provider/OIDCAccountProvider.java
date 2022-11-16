package it.smartcommunitylab.aac.openid.provider;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCAccountProvider extends AbstractProvider<OIDCUserAccount>
        implements AccountProvider<OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final UserAccountService<OIDCUserAccount> accountService;
    protected final String repositoryId;

    public OIDCAccountProvider(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            String repositoryId,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, repositoryId, realm);
    }

    public OIDCAccountProvider(String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            String repositoryId, String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        this.accountService = accountService;

        // repositoryId is always providerId, oidc isolates data per provider
        this.repositoryId = providerId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OIDCUserAccount> listAccounts(String userId) {
        List<OIDCUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> {
            a.setAuthority(getAuthority());
            a.setProvider(getProvider());
        });
        return accounts;
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount getAccount(String subject) throws NoSuchUserException {
        OIDCUserAccount account = findAccountBySubject(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccount(String subject) {
        return findAccountBySubject(subject);
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountBySubject(String subject) {
        OIDCUserAccount account = accountService.findAccountById(repositoryId, subject);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountByUuid(String uuid) {
        OIDCUserAccount account = accountService.findAccountByUuid(repositoryId, uuid);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public OIDCUserAccount lockAccount(String subject) throws NoSuchUserException, RegistrationException {
        return updateStatus(subject, SubjectStatus.LOCKED);
    }

    @Override
    public OIDCUserAccount unlockAccount(String subject) throws NoSuchUserException, RegistrationException {
        return updateStatus(subject, SubjectStatus.ACTIVE);
    }

    @Override
    public OIDCUserAccount linkAccount(String subject, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        OIDCUserAccount account = findAccountBySubject(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // re-link to user
        account.setUserId(userId);
        account = accountService.updateAccount(repositoryId, subject, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public void deleteAccount(String subject) throws NoSuchUserException {
        OIDCUserAccount account = findAccountBySubject(subject);

        if (account != null) {
            // remove account
            accountService.deleteAccount(repositoryId, subject);
        }
    }

    @Override
    public void deleteAccounts(String userId) {
        List<OIDCUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);
        for (OIDCUserAccount a : accounts) {
            // remove account
            accountService.deleteAccount(repositoryId, a.getUsername());
        }
    }

    private OIDCUserAccount updateStatus(String subject, SubjectStatus newStatus)
            throws NoSuchUserException, RegistrationException {

        OIDCUserAccount account = findAccountBySubject(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus && SubjectStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // update status
        account.setStatus(newStatus.getValue());
        account = accountService.updateAccount(repositoryId, subject, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

}
