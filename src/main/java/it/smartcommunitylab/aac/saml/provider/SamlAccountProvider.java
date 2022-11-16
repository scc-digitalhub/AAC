package it.smartcommunitylab.aac.saml.provider;

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
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

@Transactional
public class SamlAccountProvider extends AbstractProvider<SamlUserAccount>
        implements AccountProvider<SamlUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<SamlUserAccount> accountService;
    private final String repositoryId;

    public SamlAccountProvider(String providerId,
            UserAccountService<SamlUserAccount> accountService,
            String repositoryId,
            String realm) {
        this(SystemKeys.AUTHORITY_SAML, providerId, accountService, repositoryId, realm);
    }

    public SamlAccountProvider(String authority, String providerId,
            UserAccountService<SamlUserAccount> accountService,
            String repositoryId,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        this.accountService = accountService;

        // repositoryId is always providerId, saml isolates data per provider
        this.repositoryId = providerId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SamlUserAccount> listAccounts(String userId) {
        List<SamlUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> {
            a.setAuthority(getAuthority());
            a.setProvider(getProvider());
        });
        return accounts;
    }

    @Transactional(readOnly = true)
    public SamlUserAccount getAccount(String subjectId) throws NoSuchUserException {
        SamlUserAccount account = findAccountBySubjectId(subjectId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccount(String subjectId) {
        return findAccountBySubjectId(subjectId);
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccountBySubjectId(String subjectId) {
        SamlUserAccount account = accountService.findAccountById(repositoryId, subjectId);
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
    public SamlUserAccount findAccountByUuid(String uuid) {
        SamlUserAccount account = accountService.findAccountByUuid(repositoryId, uuid);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public SamlUserAccount lockAccount(String subjectId) throws NoSuchUserException, RegistrationException {
        return updateStatus(subjectId, SubjectStatus.LOCKED);
    }

    @Override
    public SamlUserAccount unlockAccount(String subjectId) throws NoSuchUserException, RegistrationException {
        return updateStatus(subjectId, SubjectStatus.ACTIVE);
    }

    @Override
    public SamlUserAccount linkAccount(String subjectId, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect userId to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        SamlUserAccount account = findAccountBySubjectId(subjectId);
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
        account = accountService.updateAccount(repositoryId, subjectId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public void deleteAccount(String subjectId) throws NoSuchUserException {
        SamlUserAccount account = findAccountBySubjectId(subjectId);

        if (account != null) {
            // remove account
            accountService.deleteAccount(repositoryId, subjectId);
        }
    }

    @Override
    public void deleteAccounts(String userId) {
        List<SamlUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);
        for (SamlUserAccount a : accounts) {
            // remove account
            accountService.deleteAccount(repositoryId, a.getUsername());
        }
    }

    private SamlUserAccount updateStatus(String subjectId, SubjectStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        SamlUserAccount account = findAccountBySubjectId(subjectId);
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
        account = accountService.updateAccount(repositoryId, subjectId, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

}
