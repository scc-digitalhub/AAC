package it.smartcommunitylab.aac.openid.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountId;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

@Transactional
public class OIDCAccountProvider extends AbstractProvider implements AccountProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCUserAccountRepository accountRepository;
    private final OIDCIdentityProviderConfig config;

    protected OIDCAccountProvider(String providerId, OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountRepository, config, realm);
    }

    protected OIDCAccountProvider(String authority, String providerId, OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountRepository = accountRepository;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OIDCUserAccount> listAccounts(String userId) {
        List<OIDCUserAccount> accounts = accountRepository.findByUserIdAndProvider(userId, getProvider());

        // we need to detach
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount getAccount(String sub) throws NoSuchUserException {
        OIDCUserAccount account = findAccountBySub(sub);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccount(String sub) {
        return findAccountBySub(sub);
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountBySub(String sub) {
        String provider = getProvider();

        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(provider, sub));
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public OIDCUserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
//        String realm = getRealm();
//        String provider = getProvider();
//
//        // check if passed map contains at least one valid set and fetch account
//        // TODO rewrite less hardcoded
//        // note AVOID reflection, we want native image support
//        OIDCUserAccount account = null;
//        if (attributes.containsKey("userId")) {
//            String userId = parseResourceId(attributes.get("userId"));
//            account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);
//        }
//
//        if (account == null
//                && attributes.keySet().containsAll(Arrays.asList("realm", "provider", "userId"))
//                && realm.equals(attributes.get("realm"))
//                && provider.equals(attributes.get("provider"))) {
//            String userId = parseResourceId(attributes.get("userId"));
//            account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);
//        }
//
//        if (account == null
//                && attributes.keySet().containsAll(Arrays.asList("realm", "provider", "email"))
//                && realm.equals(attributes.get("realm"))
//                && provider.equals(attributes.get("provider"))) {
//            account = accountRepository.findByRealmAndProviderAndEmail(realm, provider, attributes.get("email"));
//        }
//
//        if (account == null) {
//            throw new NoSuchUserException("No user found matching attributes");
//        }
//
//        // detach the entity, we don't want modifications to be persisted via a
//        // read-only interface
//        // for example eraseCredentials will reset the password in db
//        account = accountRepository.detach(account);
//
//        // rewrite internal userId
//        account.setUserId(exportInternalId(account.getUserId()));
//
//        return account;
//    }

    @Override
    public void deleteAccount(String sub) throws NoSuchUserException {
        OIDCUserAccount account = findAccountBySub(sub);

        if (account != null) {
            accountRepository.delete(account);
        }
    }

    @Override
    public OIDCUserAccount lockAccount(String sub) throws NoSuchUserException, RegistrationException {
        return updateStatus(sub, UserStatus.LOCKED);
    }

    @Override
    public OIDCUserAccount unlockAccount(String sub) throws NoSuchUserException, RegistrationException {
        return updateStatus(sub, UserStatus.ACTIVE);
    }

    @Override
    public OIDCUserAccount linkAccount(String sub, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect subject to be valid
        if (!StringUtils.hasText(userId)) {
            throw new RegistrationException("missing-user");
        }

        String provider = getProvider();

        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(provider, sub));
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // re-link to user
        account.setUserId(userId);

        // update authority to match ourselves
        account.setAuthority(getAuthority());

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    /*
     * operations
     */

    public OIDCUserAccount registerAccount(String userId, OIDCUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new RegistrationException("missing-user");
        }

        // check if already registered
        String sub = clean(reg.getSubject());
        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(provider, sub));
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        String realm = getRealm();

        // extract id fields
        String email = clean(reg.getEmail());
        String username = clean(reg.getUsername());

        // validate
        if (!StringUtils.hasText(sub)) {
            throw new RegistrationException("missing-sub");
        }
        if (!StringUtils.hasText(email) && config.requireEmailAddress()) {
            throw new RegistrationException("missing-email");
        }

        // extract attributes
        String issuer = clean(reg.getIssuer());
        Boolean emailVerified = reg.getEmailVerified();
        String name = clean(reg.getName());
        String givenName = clean(reg.getGivenName());
        String familyName = clean(reg.getFamilyName());
        String lang = clean(reg.getLang());
        String picture = clean(reg.getPicture());

        account = new OIDCUserAccount(getAuthority());
        account.setProvider(provider);
        account.setSubject(sub);

        account.setUserId(userId);
        account.setRealm(realm);

        account.setIssuer(issuer);
        account.setUsername(username);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setName(name);
        account.setGivenName(givenName);
        account.setFamilyName(familyName);
        account.setLang(lang);
        account.setPicture(picture);

        // set account as active
        account.setStatus(UserStatus.ACTIVE.getValue());

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    public OIDCUserAccount updateAccount(String sub, OIDCUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(provider, sub));
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // id attributes
        String username = clean(reg.getUsername());
        String email = clean(reg.getEmail());

        // validate email
        if (!StringUtils.hasText(email) && config.requireEmailAddress()) {
            throw new RegistrationException("missing-email");
        }

        // extract attributes
        String issuer = clean(reg.getIssuer());
        Boolean emailVerified = reg.getEmailVerified();
        String name = clean(reg.getName());
        String givenName = clean(reg.getGivenName());
        String familyName = clean(reg.getFamilyName());
        String lang = clean(reg.getLang());
        String picture = clean(reg.getPicture());

        account.setIssuer(issuer);
        account.setUsername(username);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setName(name);
        account.setGivenName(givenName);
        account.setFamilyName(familyName);
        account.setLang(lang);
        account.setPicture(picture);

        // update authority to match ourselves
        account.setAuthority(getAuthority());

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    private OIDCUserAccount updateStatus(String sub, UserStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(provider, sub));
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus && UserStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // update status
        account.setStatus(newStatus.getValue());

        // update authority to match ourselves
        account.setAuthority(getAuthority());

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    private String clean(String input) {
        return clean(input, Safelist.none());
    }

    private String clean(String input, Safelist safe) {
        if (StringUtils.hasText(input)) {
            return Jsoup.clean(input, safe);
        }
        return null;

    }

}
