package it.smartcommunitylab.aac.saml.provider;

import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractAccountService;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

@Transactional
public class SamlAccountService
        extends
        AbstractAccountService<SamlUserAccount, AbstractEditableAccount, SamlIdentityProviderConfigMap, SamlAccountServiceConfig> {

    private final SamlAccountProvider accountProvider;

    public SamlAccountService(String providerId,
            UserAccountService<SamlUserAccount> accountService,
            SamlAccountServiceConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_SAML, providerId, accountService, config, realm);
    }

    public SamlAccountService(String authority, String providerId,
            UserAccountService<SamlUserAccount> accountService,
            SamlAccountServiceConfig config,
            String realm) {
        super(authority, providerId, accountService, config, realm);

        // build an account provider
        this.accountProvider = new SamlAccountProvider(authority, providerId, accountService,
                config.getRepositoryId(), realm);
    }

    @Override
    protected SamlAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public AbstractEditableAccount getEditableAccount(String accountId) throws NoSuchUserException {
        // not supported
        throw new UnsupportedOperationException();
    }
}
