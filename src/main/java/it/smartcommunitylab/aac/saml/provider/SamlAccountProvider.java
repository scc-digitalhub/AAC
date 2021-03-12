package it.smartcommunitylab.aac.saml.provider;

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
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlAccountProvider extends AbstractProvider implements AccountProvider {

    private final SamlUserAccountRepository accountRepository;

    protected SamlAccountProvider(String providerId, SamlUserAccountRepository accountRepository, String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        this.accountRepository = accountRepository;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    public SamlUserAccount getSamlAccount(String userId) throws NoSuchUserException {
        String id = parseResourceId(userId);
        String realm = getRealm();
        String provider = getProvider();

        SamlUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, id);
        if (account == null) {
            throw new NoSuchUserException(
                    "Saml user with userId " + id + " does not exist for realm " + realm);
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);
    }

    @Override
    public UserAccount getAccount(String userId) throws NoSuchUserException {
        SamlUserAccount account = getSamlAccount(userId);

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
        SamlUserAccount account = null;
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
        List<SamlUserAccount> accounts = accountRepository.findBySubjectAndRealmAndProvider(subject, getRealm(),
                getProvider());

        // we clear passwords etc by translating to baseImpl
        return accounts.stream().map(a -> toDefaultImpl(a)).collect(Collectors.toList());
    }

    /*
     * helpers
     */

    private DefaultAccountImpl toDefaultImpl(SamlUserAccount account) {
        DefaultAccountImpl base = new DefaultAccountImpl(SystemKeys.AUTHORITY_SAML, account.getProvider(),
                account.getRealm());
        base.setUsername(account.getUsername());

        // userId is globally addressable
        base.setInternalUserId((account.getUsername()));

        return base;
    }

}
