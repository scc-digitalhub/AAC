package it.smartcommunitylab.aac.saml.provider;

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
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

@Transactional
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

    @Transactional(readOnly = true)
    public SamlUserAccount getAccount(String userId) throws NoSuchUserException {
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
        account = accountRepository.detach(account);

        // rewrite internal userId
        account.setUserId(exportInternalId(id));

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
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
    public Collection<SamlUserAccount> listAccounts(String subject) {
        List<SamlUserAccount> accounts = accountRepository.findBySubjectAndRealmAndProvider(subject, getRealm(),
                getProvider());

        // we need to fix ids and detach
        return accounts.stream().map(a -> {
            a = accountRepository.detach(a);
            a.setUserId(exportInternalId(a.getUserId()));
            return a;
        }).collect(Collectors.toList());
    }

}
