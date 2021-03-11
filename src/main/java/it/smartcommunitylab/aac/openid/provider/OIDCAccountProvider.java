package it.smartcommunitylab.aac.openid.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.DefaultAccountImpl;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCAccountProvider extends AbstractProvider implements AccountProvider {

    private final OIDCUserAccountRepository accountRepository;

    protected OIDCAccountProvider(String providerId, OIDCUserAccountRepository accountRepository, String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        this.accountRepository = accountRepository;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    public OIDCUserAccount getOIDCAccount(String userId) throws NoSuchUserException {
        String id = parseResourceId(userId);
        String realm = getRealm();
        String provider = getProvider();

        OIDCUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, id);
        if (account == null) {
            throw new NoSuchUserException(
                    "OIDC user with userId " + id + " does not exist for realm " + realm);
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);
    }

    @Override
    public UserAccount getAccount(String userId) throws NoSuchUserException {
        OIDCUserAccount account = getOIDCAccount(userId);

        // make sure we don't leak internal data
        return toDefaultImpl(account);
    }

    @Override
    public UserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
        String realm = getRealm();
        String provider = getProvider();

        // check if passed map contains at least one valid set and fetch account
        // TODO rewrite less hardcoded
        // note AVOID reflection, we want native image support
        OIDCUserAccount account = null;
        if (attributes.containsKey("userId")) {
            String userId = parseResourceId(attributes.get("userId"));
            account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "provider", "userId"))
                && realm.equals(attributes.get("realm"))
                && provider.equals(attributes.get("provider"))) {
            String userId = parseResourceId(attributes.get("userId"));
            account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "provider", "email"))
                && realm.equals(attributes.get("realm"))
                && provider.equals(attributes.get("provider"))) {
            account = accountRepository.findByRealmAndProviderAndEmail(realm, provider, attributes.get("email"));
        }

        if (account == null) {
            throw new NoSuchUserException("No user found matching attributes");
        }

        // make sure we don't leak internal data
        return toDefaultImpl(account);
    }

    @Override
    public Collection<UserAccount> listAccounts(String subject) {
        List<OIDCUserAccount> accounts = accountRepository.findBySubjectAndRealmAndProvider(subject, getRealm(),
                getProvider());

        // we clear passwords etc by translating to baseImpl
        return accounts.stream().map(a -> toDefaultImpl(a)).collect(Collectors.toList());
    }

    /*
     * helpers
     */

    private DefaultAccountImpl toDefaultImpl(OIDCUserAccount account) {
        DefaultAccountImpl base = new DefaultAccountImpl(SystemKeys.AUTHORITY_OIDC, account.getProvider(),
                account.getRealm());
        base.setUsername(account.getUsername());

        // userId is globally addressable
        base.setInternalUserId((account.getUsername()));

        return base;
    }

}
