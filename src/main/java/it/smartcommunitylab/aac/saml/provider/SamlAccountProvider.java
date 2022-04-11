package it.smartcommunitylab.aac.saml.provider;

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
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountId;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

@Transactional
public class SamlAccountProvider extends AbstractProvider implements AccountProvider<SamlUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SamlUserAccountRepository accountRepository;
    private final SamlIdentityProviderConfig config;

    protected SamlAccountProvider(String providerId, SamlUserAccountRepository accountRepository,
            SamlIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
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
    public List<SamlUserAccount> listAccounts(String userId) {
        List<SamlUserAccount> accounts = accountRepository.findByUserIdAndProvider(userId, getProvider());

        // we need to detach
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SamlUserAccount getAccount(String subjectId) throws NoSuchUserException {
        SamlUserAccount account = findAccountBySubjectId(subjectId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccount(String subjectId) {
        return findAccountBySubjectId(subjectId);
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccountBySubjectId(String subjectId) {
        String provider = getProvider();

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(provider, subjectId));
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
//    public SamlUserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
//        String realm = getRealm();
//        String provider = getProvider();
//
//        // check if passed map contains at least one valid set and fetch account
//        // TODO rewrite less hardcoded
//        // note AVOID reflection, we want native image support
//        SamlUserAccount account = null;
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
//        // TODO add find by usernameAttribute as set in providerConfig
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
    public void deleteAccount(String subjectId) throws NoSuchUserException {
        SamlUserAccount account = findAccountBySubjectId(subjectId);

        if (account != null) {
            accountRepository.delete(account);
        }
    }

    @Override
    public SamlUserAccount lockAccount(String sub) throws NoSuchUserException, RegistrationException {
        return updateStatus(sub, UserStatus.LOCKED);
    }

    @Override
    public SamlUserAccount unlockAccount(String sub) throws NoSuchUserException, RegistrationException {
        return updateStatus(sub, UserStatus.ACTIVE);
    }

    @Override
    public SamlUserAccount linkAccount(String sub, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect userId to be valid
        if (!StringUtils.hasText(userId)) {
            throw new RegistrationException("missing-user");
        }

        String provider = getProvider();

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(provider, sub));
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
        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    /*
     * operations
     */

    public SamlUserAccount registerAccount(String userId, SamlUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        // we expect userId to be valid
        if (!StringUtils.hasText(userId)) {
            throw new RegistrationException("missing-user");
        }

        // check if already registered
        String subjectId = clean(reg.getSubjectId());
        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(provider, subjectId));
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        String realm = getRealm();

        // extract id fields
        String email = clean(reg.getEmail());
        String username = clean(reg.getUsername());

        // validate
        if (!StringUtils.hasText(subjectId)) {
            throw new RegistrationException("missing-subject-identifier");
        }
        if (!StringUtils.hasText(email) && config.requireEmailAddress()) {
            throw new RegistrationException("missing-email");
        }

        // extract attributes
        String issuer = reg.getIssuer();
        Boolean emailVerified = reg.getEmailVerified();
        String name = clean(reg.getName());
        String lang = clean(reg.getLang());

        account = new SamlUserAccount();
        account.setProvider(provider);
        account.setSubjectId(subjectId);

        account.setUserId(userId);
        account.setRealm(realm);

        account.setIssuer(issuer);
        account.setUsername(username);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setName(name);
        account.setLang(lang);

        // set account as active
        account.setStatus(UserStatus.ACTIVE.getValue());

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    public SamlUserAccount updateAccount(String sub, SamlUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(provider, sub));
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
        String issuer = reg.getIssuer();
        Boolean emailVerified = reg.getEmailVerified();
        String name = clean(reg.getName());
        String lang = clean(reg.getLang());

        account.setIssuer(issuer);
        account.setUsername(username);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setName(name);
        account.setLang(lang);

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    private SamlUserAccount updateStatus(String sub, UserStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        SamlUserAccount account = accountRepository.findOne(new SamlUserAccountId(provider, sub));
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
