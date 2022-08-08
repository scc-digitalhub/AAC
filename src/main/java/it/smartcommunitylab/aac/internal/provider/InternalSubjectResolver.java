package it.smartcommunitylab.aac.internal.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.model.Subject;

public class InternalSubjectResolver extends AbstractProvider
        implements SubjectResolver<InternalUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final static String[] ATTRIBUTES = { "email" };

    private final InternalUserAccountService accountService;
    private final InternalIdentityProviderConfig config;

    private final String repositoryId;

    public InternalSubjectResolver(String providerId, InternalUserAccountService userAccountService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        this.accountService = userAccountService;
        this.config = providerConfig;

        this.repositoryId = config.getRepositoryId();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    public Subject resolveByUsername(String username) {
        logger.debug("resolve by username " + username);
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }
        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String username) {
        // accountId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByPrincipalId(String username) {
        // principalId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByIdentityId(String username) {
        // identityId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByEmailAddress(String email) {
        if (!config.isLinkable()) {
            return null;
        }

        logger.debug("resolve by email " + email);
        InternalUserAccount account = accountService.findAccountByEmail(repositoryId, email).stream()
                .filter(a -> a.isEmailVerified())
                .findFirst()
                .orElse(null);

        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

}
