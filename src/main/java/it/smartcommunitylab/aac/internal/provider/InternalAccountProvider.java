package it.smartcommunitylab.aac.internal.provider;

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
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.SubjectStatus;

@Transactional
public class InternalAccountProvider extends AbstractProvider<InternalUserAccount>
        implements AccountProvider<InternalUserAccount>, AccountPrincipalConverter<InternalUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<InternalUserAccount> accountService;
    private final String repositoryId;

    public InternalAccountProvider(String providerId,
            UserAccountService<InternalUserAccount> accountService,
            String repositoryId, String realm) {
        this(SystemKeys.AUTHORITY_INTERNAL, providerId, accountService, repositoryId, realm);
    }

    public InternalAccountProvider(String authority, String providerId,
            UserAccountService<InternalUserAccount> accountService,
            String repositoryId, String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        this.accountService = accountService;

        // repositoryId from config
        this.repositoryId = repositoryId;

    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public InternalUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        // we expect an instance of our model
        Assert.isInstanceOf(InternalUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of internal authenticated principal");
        InternalUserAuthenticatedPrincipal principal = (InternalUserAuthenticatedPrincipal) userPrincipal;

        // sanity check for same authority
        if (!getAuthority().equals(principal.getAuthority())) {
            throw new IllegalArgumentException("authority mismatch");
        }

        // username binds all identity pieces together
        String username = principal.getUsername();

        // get from service, account should already exists
        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            // error, user should already exists for authentication
            // this should be unreachable
            throw new IllegalArgumentException();
        }

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalUserAccount> listAccounts(String userId) {
        List<InternalUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> {
            a.setAuthority(getAuthority());
            a.setProvider(getProvider());
        });
        return accounts;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount getAccount(String username) throws NoSuchUserException {
        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccount(String username) {
        return findAccountByUsername(username);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUsername(String username) {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
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
    public InternalUserAccount findAccountByUuid(String uuid) {
        InternalUserAccount account = accountService.findAccountByUuid(repositoryId, uuid);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByEmail(String email) {
        // we pick first account matching email, repository should contain unique
        // email+provider
        InternalUserAccount account = accountService.findAccountByEmail(repositoryId, email).stream()
                .filter(a -> a.isEmailVerified())
                .findFirst()
                .orElse(null);

        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public InternalUserAccount lockAccount(String username) throws NoSuchUserException, RegistrationException {
        return updateStatus(username, SubjectStatus.LOCKED);
    }

    @Override
    public InternalUserAccount unlockAccount(String username) throws NoSuchUserException, RegistrationException {
        return updateStatus(username, SubjectStatus.ACTIVE);
    }

    @Override
    public InternalUserAccount linkAccount(String username, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        InternalUserAccount account = findAccountByUsername(username);
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
        account = accountService.updateAccount(repositoryId, username, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public void deleteAccount(String username) throws NoSuchUserException {
        InternalUserAccount account = findAccountByUsername(username);

        if (account != null) {
            // remove account
            accountService.deleteAccount(repositoryId, username);
        }
    }

    @Override
    public void deleteAccounts(String userId) {
        List<InternalUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);
        for (InternalUserAccount a : accounts) {
            // remove account
            accountService.deleteAccount(repositoryId, a.getUsername());
        }
    }

    private InternalUserAccount updateStatus(String username, SubjectStatus newStatus)
            throws NoSuchUserException, RegistrationException {

        InternalUserAccount account = findAccountByUsername(username);
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
        account = accountService.updateAccount(repositoryId, username, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

}
