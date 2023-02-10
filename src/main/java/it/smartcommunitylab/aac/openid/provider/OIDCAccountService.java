package it.smartcommunitylab.aac.openid.provider;

import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractAccountService;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.model.OIDCEditableUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCAccountService extends
        AbstractAccountService<OIDCUserAccount, AbstractEditableAccount, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig> {

    public OIDCAccountService(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCAccountServiceConfig config, String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, config, realm);
    }

    public OIDCAccountService(String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCAccountServiceConfig config, String realm) {
        super(authority, providerId, accountService, config, realm);
    }

    @Override
    public OIDCEditableUserAccount getEditableAccount(String userId, String subject) throws NoSuchUserException {
        OIDCUserAccount account = findAccount(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        return toEditableAccount(account);
    }

    private OIDCEditableUserAccount toEditableAccount(OIDCUserAccount account) {
        // build editable model
        OIDCEditableUserAccount ea = new OIDCEditableUserAccount(
                getAuthority(), getProvider(), getRealm(),
                account.getUserId(), account.getUuid());
        ea.setSubject(account.getSubject());
        ea.setUsername(account.getUsername());

        ea.setCreateDate(account.getCreateDate());
        ea.setModifiedDate(account.getModifiedDate());

        ea.setEmail(account.getEmail());
        ea.setName(account.getName());
        ea.setGivenName(account.getGivenName());
        ea.setFamilyName(account.getFamilyName());
        ea.setLang(account.getLang());

        return ea;
    }

}
