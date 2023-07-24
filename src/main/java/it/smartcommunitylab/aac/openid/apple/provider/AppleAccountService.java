package it.smartcommunitylab.aac.openid.apple.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountService;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AppleAccountService
    extends AbstractAccountService<OIDCUserAccount, AbstractEditableAccount, AppleIdentityProviderConfigMap, AppleAccountServiceConfig> {

    public AppleAccountService(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        AppleAccountServiceConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_APPLE, providerId, accountService, config, realm);
    }
}
