package it.smartcommunitylab.aac.internal.provider;

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
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalAccountProvider extends AbstractProvider implements AccountProvider {

    private final InternalUserAccountRepository accountRepository;

    public InternalAccountProvider(String providerId, InternalUserAccountRepository accountRepository, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        this.accountRepository = accountRepository;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    public InternalUserAccount getInternalAccount(String userId) throws NoSuchUserException {
        String username = parseResourceId(userId);
        String realm = getRealm();
        InternalUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            throw new NoSuchUserException(
                    "Internal user with username " + username + " does not exist for realm " + realm);
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);
    }

    @Override
    public UserAccount getAccount(String userId) throws NoSuchUserException {
        InternalUserAccount account = getInternalAccount(userId);

        // make sure we don't leak internal data
        return toDefaultImpl(account);
    }

    @Override
    public UserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
        String realm = getRealm();

        // check if passed map contains at least one valid set and fetch account
        // TODO rewrite less hardcoded
        // note AVOID reflection, we want native image support
        InternalUserAccount account = null;
        if (attributes.containsKey("userId")) {
            String username = parseResourceId(attributes.get("userId"));
            account = accountRepository.findByRealmAndUsername(realm, username);
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "username"))
                && realm.equals((attributes.get("realm")))) {
            account = accountRepository.findByRealmAndUsername(realm, attributes.get("username"));
        }


        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "email"))
                && realm.equals((attributes.get("realm")))) {
            account = accountRepository.findByRealmAndEmail(realm, attributes.get("email"));
        }

        
        if (account == null
                && attributes.keySet().contains("confirmationKey")) {
            account = accountRepository.findByConfirmationKey(attributes.get("confirmationKey"));
            // check realm match
            if (!realm.equals(account.getRealm())) {
                // does not belong here, can't use
                account = null;
            }
        }

        if (account == null
                && attributes.keySet().contains("resetKey")) {
            account = accountRepository.findByConfirmationKey(attributes.get("resetKey"));
            // check realm match
            if (!realm.equals(account.getRealm())) {
                // does not belong here, can't use
                account = null;
            }
        }

        if (account == null) {
            throw new NoSuchUserException("No internal user found matching attributes");
        }

        // make sure we don't leak internal data
        return toDefaultImpl(account);
    }

    @Override
    public Collection<UserAccount> listAccounts(String subject) {
        List<InternalUserAccount> accounts = accountRepository.findBySubjectAndRealm(subject, getRealm());

        // we clear passwords etc by translating to baseImpl
        return accounts.stream().map(a -> toDefaultImpl(a)).collect(Collectors.toList());
    }

    /*
     * helpers
     */

    private DefaultAccountImpl toDefaultImpl(InternalUserAccount account) {
        DefaultAccountImpl base = new DefaultAccountImpl(SystemKeys.AUTHORITY_INTERNAL, account.getProvider(),
                account.getRealm());
        base.setUsername(account.getUsername());

        // userId is globally addressable
        base.setInternalUserId((account.getUsername()));

        return base;
    }

}
