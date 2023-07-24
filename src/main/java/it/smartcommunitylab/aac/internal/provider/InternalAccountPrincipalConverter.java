package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class InternalAccountPrincipalConverter
    extends AbstractProvider<InternalUserAccount>
    implements AccountPrincipalConverter<InternalUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<InternalUserAccount> userAccountService;
    private final String repositoryId;

    public InternalAccountPrincipalConverter(
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_INTERNAL, providerId, accountService, repositoryId, realm);
    }

    public InternalAccountPrincipalConverter(
        String authority,
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        this.userAccountService = accountService;

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
        Assert.isInstanceOf(
            InternalUserAuthenticatedPrincipal.class,
            userPrincipal,
            "principal must be an instance of internal authenticated principal"
        );
        InternalUserAuthenticatedPrincipal principal = (InternalUserAuthenticatedPrincipal) userPrincipal;

        // sanity check for same authority
        if (!getAuthority().equals(principal.getAuthority())) {
            throw new IllegalArgumentException("authority mismatch");
        }

        // username binds all identity pieces together
        String username = principal.getUsername();

        // get from service, account should already exists
        InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
        if (account == null) {
            logger.error("unable to find account for username {}", String.valueOf(username));

            // error, user should already exists for authentication
            // this should be unreachable
            throw new IllegalArgumentException();
        }

        return account;
    }
}
