package it.smartcommunitylab.aac.spid.service;

import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Transactional
public class SpidJpaUserAccountService implements UserAccountService<SpidUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SpidUserAccountEntityRepository accountRepository;
    private final SubjectService subjectService;

    public SpidJpaUserAccountService(SpidUserAccountEntityRepository accountRepository, SubjectService subjectService) {
        Assert.notNull(accountRepository, "spid account repository is required");
        Assert.notNull(subjectService, "subject service is mandatory");
        this.accountRepository = accountRepository;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccounts(String repositoryId) {
        // TODO
        return null;
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByRealm(String realm) {
        // TODO
        return null;
    }

    @Transactional(readOnly = true)
    public SpidUserAccount findAccountById(String repository, String id) {
        // TODO
        return null;
    }

    @Transactional(readOnly = true)
    public SpidUserAccount findAccountByUuid(String uuid) {
        // TODO
        return null;
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByUsername(String repository, String username) {
        // TODO
        return null;
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByEmail(String repository, String email) {
        // TODO
        return null;
    }

    @Transactional(readOnly = true)
    public List<SpidUserAccount> findAccountsByUser(String repository, String userId) {
        // TODO
        return null;
    }

    @Override
    public SpidUserAccount addAccount(String repository, String id, SpidUserAccount reg) throws RegistrationException {
        // TODO
        return null;
    }

    @Override
    public SpidUserAccount updateAccount(String repository, String id, SpidUserAccount reg) throws NoSuchUserException, RegistrationException {
        // TODO
        return null;
    }

    @Override
    public void deleteAccount(String repository, String id) {
        // TODO
    }

    @Override
    public void deleteAllAccountsByUser(String repository, String userId) {

    }
}
