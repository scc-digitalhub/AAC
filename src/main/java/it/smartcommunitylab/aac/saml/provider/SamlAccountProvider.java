package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SamlAccountProvider extends AbstractAccountProvider<SamlUserAccount> {

    public SamlAccountProvider(
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_SAML, providerId, accountService, repositoryId, realm);
    }

    public SamlAccountProvider(
        String authority,
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        super(authority, providerId, accountService, repositoryId, realm);
    }
}
