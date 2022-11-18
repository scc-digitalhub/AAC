package it.smartcommunitylab.aac.core.base;

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
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.model.SubjectStatus;

@Transactional
public class AbstractAccountProvider<U extends AbstractAccount> extends AbstractProvider<U>
        implements AccountProvider<U> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    protected final UserAccountService<U> accountService;
    protected final String repositoryId;

    public AbstractAccountProvider(
            String authority, String providerId,
            UserAccountService<U> accountService,
            String repositoryId, String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        logger.debug("create {} account provider for realm {} with id {}", String.valueOf(authority),
                String.valueOf(realm), String.valueOf(providerId));

        this.accountService = accountService;
        this.repositoryId = repositoryId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    @Transactional(readOnly = true)
    public List<U> listAccounts(String userId) {
        List<U> accounts = accountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> {
            a.setAuthority(getAuthority());
            a.setProvider(getProvider());
        });
        return accounts;
    }

    @Transactional(readOnly = true)
    public U getAccount(String accountId) throws NoSuchUserException {
        U account = findAccount(accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public U findAccount(String accountId) {
        U account = accountService.findAccountById(repositoryId, accountId);
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
    public U findAccountByUuid(String uuid) {
        U account = accountService.findAccountByUuid(repositoryId, uuid);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public U lockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return updateStatus(accountId, SubjectStatus.LOCKED);
    }

    @Override
    public U unlockAccount(String accountId) throws NoSuchUserException, RegistrationException {
        return updateStatus(accountId, SubjectStatus.ACTIVE);
    }

    @Override
    public U linkAccount(String accountId, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        U account = findAccount(accountId);
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
        account = accountService.updateAccount(repositoryId, accountId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public void deleteAccount(String accountId) throws NoSuchUserException {
        U account = findAccount(accountId);

        if (account != null) {
            // remove account
            accountService.deleteAccount(repositoryId, accountId);
        }
    }

    @Override
    public void deleteAccounts(String userId) {
        List<U> accounts = accountService.findAccountByUser(repositoryId, userId);
        for (U a : accounts) {
            // remove account
            accountService.deleteAccount(repositoryId, a.getUsername());
        }
    }

    protected U updateStatus(String accountId, SubjectStatus newStatus)
            throws NoSuchUserException, RegistrationException {

        U account = findAccount(accountId);
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
        account = accountService.updateAccount(repositoryId, accountId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

}
