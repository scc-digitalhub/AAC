package it.smartcommunitylab.aac.openid.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountId;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

@Transactional
public class OIDCSubjectResolver extends AbstractProvider implements SubjectResolver<OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCUserAccountRepository accountRepository;
    private final OIDCIdentityProviderConfig config;

    public OIDCSubjectResolver(String providerId, OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountRepository, config, realm);
    }

    public OIDCSubjectResolver(String authority, String providerId, OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.accountRepository = accountRepository;
        this.config = config;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Transactional(readOnly = true)
    public Subject resolveBySubject(String sub) {
        logger.debug("resolve by sub " + sub);
        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(getProvider(), sub));
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
        logger.debug("resolve by username " + username);
        OIDCUserAccount account = accountRepository.findByProviderAndUsername(getProvider(), username).stream()
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
        if (!config.isLinkable()) {
            return null;
        }

        logger.debug("resolve by email " + email);
        OIDCUserAccount account = accountRepository.findByProviderAndEmail(getProvider(), email).stream()
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
