package it.smartcommunitylab.aac.openid.provider;

import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountService;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCAccountService extends
        AbstractAccountService<OIDCUserAccount, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig> {

    private final OIDCAccountProvider accountProvider;

    public OIDCAccountService(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCAccountServiceConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, config, realm);
    }

    public OIDCAccountService(String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCAccountServiceConfig config,
            String realm) {
        super(authority, providerId, accountService, config, realm);

        // build an account provider
        this.accountProvider = new OIDCAccountProvider(authority, providerId, accountService,
                config.getRepositoryId(), realm);
    }

    @Override
    protected OIDCAccountProvider getAccountProvider() {
        return accountProvider;
    }
}
