package it.smartcommunitylab.aac.openid.apple.provider;

import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountService;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountProvider;

@Transactional
public class AppleAccountService extends
        AbstractAccountService<OIDCUserAccount, AppleIdentityProviderConfigMap, AppleAccountServiceConfig> {

    private final OIDCAccountProvider accountProvider;

    public AppleAccountService(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            AppleAccountServiceConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_APPLE, providerId, accountService, config, realm);

        // build an account provider
        this.accountProvider = new OIDCAccountProvider(SystemKeys.AUTHORITY_APPLE, providerId, accountService,
                config.getRepositoryId(), realm);
    }

    @Override
    protected OIDCAccountProvider getAccountProvider() {
        return accountProvider;
    }
}
