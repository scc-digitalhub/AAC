package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

// TODO
public class SpidSubjectResolver extends AbstractProvider<SpidUserAccount> implements SubjectResolver<SpidUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserAccountService<SpidUserAccount> accountService;
    private final SpidIdentityProviderConfig config;
    private String repositoryId;

    public SpidSubjectResolver(
        String authority,
        String providerId,
        UserAccountService<SpidUserAccount>userAccountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.accountService = userAccountService;
        this.config = config;
        this.repositoryId = providerId;
    }

    @Override
    public Subject resolveByAccountId(String accountId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByPrincipalId(String principalId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByUsername(String accountId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByEmailAddress(String accountId) {
        // TODO
        return null;
    }
}
