package it.smartcommunitylab.aac.internal.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;

public class InternalAccountProvider extends AbstractProvider implements AccountProvider {

    protected final InternalUserAccountService userAccountService;

    public InternalAccountProvider(String providerId, InternalUserAccountService userAccountService, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        this.userAccountService = userAccountService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public List<InternalUserAccount> listAccounts(String subject) {
        List<InternalUserAccount> accounts = userAccountService.findBySubject(subject, getRealm());

        // we need to fix ids
        return accounts.stream().map(a -> {
            a.setProvider(getProvider());
            a.setUserId(exportInternalId(a.getUsername()));
            return a;
        }).collect(Collectors.toList());
    }

    @Override
    public InternalUserAccount getAccount(String userId) throws NoSuchUserException {
        String username = parseResourceId(userId);
        String realm = getRealm();
        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account == null) {
            throw new NoSuchUserException(
                    "Internal user with username " + username + " does not exist for realm " + realm);
        }

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        return account;
    }

    @Override
    public InternalUserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
        String realm = getRealm();

        // check if passed map contains at least one valid set and fetch account
        // TODO rewrite less hardcoded
        // note AVOID reflection, we want native image support
        InternalUserAccount account = null;
        if (attributes.containsKey("userId")) {
            String username = parseResourceId(attributes.get("userId"));
            account = userAccountService.findAccountByUsername(realm, username);
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "username"))
                && realm.equals((attributes.get("realm")))) {
            account = userAccountService.findAccountByUsername(realm, attributes.get("username"));
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "email"))
                && realm.equals((attributes.get("realm")))) {
            account = userAccountService.findAccountByEmail(realm, attributes.get("email"));
        }

        if (account == null
                && attributes.keySet().contains("confirmationKey")) {
            account = userAccountService.findAccountByConfirmationKey(realm, attributes.get("confirmationKey"));  
        }

        if (account == null
                && attributes.keySet().contains("resetKey")) {
            account = userAccountService.findAccountByResetKey(realm, attributes.get("resetKey"));          
        }

        if (account == null) {
            throw new NoSuchUserException("No internal user found matching attributes");
        }

        String username = account.getUsername();

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        return account;
    }

    /*
     * helpers
     */
//
//    private DefaultAccountImpl toDefaultImpl(InternalUserAccount account) {
//        DefaultAccountImpl base = new DefaultAccountImpl(SystemKeys.AUTHORITY_INTERNAL, account.getProvider(),
//                account.getRealm());
//        base.setUsername(account.getUsername());
//
//        // userId is globally addressable
//        base.setInternalUserId((account.getUsername()));
//
//        return base;
//    }

}
