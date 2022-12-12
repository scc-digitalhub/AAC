package it.smartcommunitylab.aac.openid.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCSubjectResolver extends AbstractProvider<OIDCUserAccount> implements SubjectResolver<OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<OIDCUserAccount> accountService;
    private boolean isLinkable;

    private final String repositoryId;

    public OIDCSubjectResolver(String providerId, UserAccountService<OIDCUserAccount> userAccountService,
            String repositoryId,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, userAccountService, repositoryId, realm);
    }

    public OIDCSubjectResolver(String authority, String providerId,
            UserAccountService<OIDCUserAccount> userAccountService,
            String repositoryId,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        this.accountService = userAccountService;
        this.repositoryId = repositoryId;

        // by default oidc is not linkable via attributes
        this.isLinkable = false;
    }

    public void setLinkable(boolean isLinkable) {
        this.isLinkable = isLinkable;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Transactional(readOnly = true)
    public Subject resolveBySubject(String sub) {
        logger.debug("resolve by sub {}", String.valueOf(sub));
        OIDCUserAccount account = accountService.findAccountById(repositoryId, sub);
        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String accountId) {
        // accountId is sub
        return resolveBySubject(accountId);
    }

    @Override
    public Subject resolveByPrincipalId(String principalId) {
        // principalId is sub
        return resolveBySubject(principalId);
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        // identityId is sub
        return resolveBySubject(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByUsername(String username) {
        logger.debug("resolve by username {}", String.valueOf(username));
        OIDCUserAccount account = accountService.findAccountByUsername(repositoryId, username).stream()
                .findFirst()
                .orElse(null);
        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByEmailAddress(String email) {
        if (!isLinkable) {
            return null;
        }

        logger.debug("resolve by email {}", String.valueOf(email));
        OIDCUserAccount account = accountService.findAccountByEmail(repositoryId, email).stream()
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
