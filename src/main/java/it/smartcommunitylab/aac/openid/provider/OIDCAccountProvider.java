package it.smartcommunitylab.aac.openid.provider;

import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCAccountProvider extends AbstractAccountProvider<OIDCUserAccount> {

    public OIDCAccountProvider(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            String repositoryId, String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, repositoryId, realm);
    }

    public OIDCAccountProvider(String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            String repositoryId, String realm) {
        super(authority, providerId, accountService, repositoryId, realm);
    }

}
