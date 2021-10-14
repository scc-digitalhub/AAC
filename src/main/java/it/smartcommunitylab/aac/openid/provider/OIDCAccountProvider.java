package it.smartcommunitylab.aac.openid.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

@Transactional
public class OIDCAccountProvider extends AbstractProvider implements AccountProvider {

    private final OIDCUserAccountRepository accountRepository;
    private final OIDCIdentityProviderConfig providerConfig;

    protected OIDCAccountProvider(String providerId, OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.providerConfig = config;
        this.accountRepository = accountRepository;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccount(String userId) {
        String id = parseResourceId(userId);
        String realm = getRealm();
        String provider = getProvider();

        OIDCUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, id);
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        account = accountRepository.detach(account);

        // rewrite internal userId
        account.setUserId(exportInternalId(id));

        return account;
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount getAccount(String userId) throws NoSuchUserException {
        OIDCUserAccount account = findAccount(userId);
        if (account == null) {
            throw new NoSuchUserException(
                    "OIDC user with userId " + userId + " does not exist for realm " + getRealm());
        }

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
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

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        account = accountRepository.detach(account);

        // rewrite internal userId
        account.setUserId(exportInternalId(account.getUserId()));

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<OIDCUserAccount> listAccounts(String subject) {
        List<OIDCUserAccount> accounts = accountRepository.findBySubjectAndRealmAndProvider(subject, getRealm(),
                getProvider());

        // we need to fix ids and detach
        return accounts.stream().map(a -> {
            a = accountRepository.detach(a);
            a.setUserId(exportInternalId(a.getUserId()));
            return a;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteAccount(String userId) throws NoSuchUserException {
        String id = parseResourceId(userId);
        String realm = getRealm();
        String provider = getProvider();

        // delete account
        OIDCUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, id);
        if (account != null) {
            accountRepository.delete(account);
        }
    }

    /*
     * helpers
     */

//    private DefaultAccountImpl toDefaultImpl(OIDCUserAccount account) {
//        DefaultAccountImpl base = new DefaultAccountImpl(SystemKeys.AUTHORITY_OIDC, account.getProvider(),
//                account.getRealm());
//        base.setUsername(account.getUsername());
//
//        // userId is globally addressable
//        base.setInternalUserId((account.getUsername()));
//
//        return base;
//    }

}
