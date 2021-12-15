package it.smartcommunitylab.aac.internal.provider;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidInputException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalAccountService extends AbstractProvider implements AccountService {

    private static final String LANG_UNDEFINED = "en";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // use password service to handle password
    private final InternalPasswordService passwordService;

    // provider configuration
    private final InternalIdentityProviderConfig providerConfig;
    private final InternalIdentityProviderConfigMap config;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    protected final InternalUserAccountService userAccountService;

    public InternalAccountService(String providerId,
            InternalUserAccountService userAccountService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");
        Assert.notNull(userAccountService, "user account service is mandatory");
        this.userAccountService = userAccountService;
        this.providerConfig = providerConfig;
        this.config = providerConfig.getConfigMap();

        this.passwordService = new InternalPasswordService(providerId, userAccountService, providerConfig, realm);
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;

        // also assign to passwordService to be consistent
        this.passwordService.setMailService(mailService);
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;

        // also assign to passwordService to be consistent
        this.passwordService.setUriBuilder(uriBuilder);
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

    @Override
    public void deleteAccount(String userId) throws NoSuchUserException {
        String username = parseResourceId(userId);
        String realm = getRealm();

        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);

        if (account != null) {
            // remove account
            userAccountService.deleteAccount(account.getId());
        }
    }

    @Override
    public boolean canRegister() {
        return config.isEnableRegistration();
    }

    @Override
    public boolean canUpdate() {
        return config.isEnableUpdate();
    }

    public InternalUserAccount findAccountByUsername(String username) {
        String realm = getRealm();
        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account == null) {
            return null;
        }

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        return account;
    }

    @Override
    public InternalUserAccount registerAccount(String subjectId, UserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // we expect subject to be valid
        if (!StringUtils.hasText(subjectId)) {
            throw new RegistrationException("missing-subject");

        }

        String realm = getRealm();

        // extract base fields
        String username = Jsoup.clean(reg.getUsername(), Safelist.none());

        // validate username
        if (!StringUtils.hasText(username)) {
            throw new RegistrationException("missing-username");
        }

        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        // check type and extract our parameters if present
        String password = null;
        String email = null;
        String name = null;
        String surname = null;
        String lang = null;
        boolean confirmed = !config.isConfirmationRequired();

        if (reg instanceof InternalUserAccount) {
            InternalUserAccount ireg = (InternalUserAccount) reg;
            password = ireg.getPassword();
            email = ireg.getEmail();
            name = ireg.getName();
            surname = ireg.getSurname();
            lang = ireg.getLang();

            if (StringUtils.hasText(password)) {
                password = Jsoup.clean(password, Safelist.none());
            }
            if (StringUtils.hasText(email)) {
                email = Jsoup.clean(email, Safelist.none());
            }
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            if (StringUtils.hasText(surname)) {
                surname = Jsoup.clean(surname, Safelist.none());
            }
            if (StringUtils.hasText(lang)) {
                lang = Jsoup.clean(lang, Safelist.none());
            }

            // we accept confirmed accounts
            if (!confirmed) {
                confirmed = ireg.isConfirmed();
            }
        }

        if (!confirmed && config.isConfirmationRequired() && !StringUtils.hasText(email)) {
            throw new IllegalArgumentException("missing-email");
        }

        boolean changeOnFirstAccess = false;
        if (!StringUtils.hasText(password)) {
            password = passwordService.generatePassword();
            changeOnFirstAccess = true;
        } else {
            passwordService.validatePassword(password);
        }

        account = new InternalUserAccount();
        account.setSubject(subjectId);
        account.setRealm(realm);
        account.setUsername(username);
        // by default disable login
        account.setPassword(null);
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);
        // set confirmed
        account.setConfirmed(confirmed);

        if (!confirmed) {
            // build key
            String confirmationKey = passwordService.generateKey();

            // we set deadline as +N seconds
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, config.getConfirmationValidity());

            account.setConfirmationDeadline(calendar.getTime());
            account.setConfirmationKey(confirmationKey);
        }

//        account.setResetDeadline(null);
//        account.setResetKey(null);
        account.setChangeOnFirstAccess(changeOnFirstAccess);

        account = userAccountService.addAccount(account);

        String userId = this.exportInternalId(username);

        account = passwordService.setPassword(userId, password, changeOnFirstAccess);

        // send mail
        try {
            if (changeOnFirstAccess) {
                // we need to send single-use password
                if (account.isConfirmed()) {
                    sendPasswordMail(account, password);
                } else {
                    // also send confirmation link
                    sendPasswordAndConfirmationMail(account, password, account.getConfirmationKey());
                }
            } else if (!account.isConfirmed()) {
                // send only confirmation link
                sendConfirmationMail(account, account.getConfirmationKey());
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        // TODO evaluate returning cleartext password after creation

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        return account;

    }

    @Override
    public InternalUserAccount updateAccount(String userId, UserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String username = parseResourceId(userId);
        String realm = getRealm();

        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // can update only from our model
        if (reg instanceof InternalUserAccount) {
            InternalUserAccount ireg = (InternalUserAccount) reg;
            String email = ireg.getEmail();
            String name = ireg.getName();
            String surname = ireg.getSurname();
            String lang = ireg.getLang();

            if (StringUtils.hasText(email)) {
                email = Jsoup.clean(email, Safelist.none());
            }
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            if (StringUtils.hasText(surname)) {
                surname = Jsoup.clean(surname, Safelist.none());
            }
            if (StringUtils.hasText(lang)) {
                lang = Jsoup.clean(lang, Safelist.none());
            }

            // we update all props, even if empty or null
            account.setEmail(email);
            account.setName(name);
            account.setSurname(surname);
            account.setLang(lang);

            account = userAccountService.updateAccount(account.getId(), account);
        }

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        return account;
    }

    public InternalUserAccount resetConfirm(String userId)
            throws NoSuchUserException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        String username = parseResourceId(userId);
        String realm = getRealm();

        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // build key
        String confirmationKey = passwordService.generateKey();

        // we set deadline as +N seconds
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, config.getConfirmationValidity());

        account.setConfirmed(false);
        account.setConfirmationDeadline(calendar.getTime());
        account.setConfirmationKey(confirmationKey);

        account = userAccountService.updateAccount(account.getId(), account);

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        return account;
    }

    public InternalUserAccount confirmAccount(String confirmationKey) throws NoSuchUserException {

        if (!StringUtils.hasText(confirmationKey)) {
            throw new IllegalArgumentException("empty-key");
        }
        String realm = getRealm();
        InternalUserAccount account = userAccountService.findAccountByConfirmationKey(realm, confirmationKey);
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (!account.getRealm().equals(realm)) {
            throw new IllegalArgumentException("realm mismatch");
        }

        if (!account.isConfirmed()) {

            // validate key, we do it simple
            boolean isValid = false;

            // validate key match
            // useless check since we fetch account with key as input..
            boolean isMatch = confirmationKey.equals(account.getConfirmationKey());

            if (!isMatch) {
                logger.error("invalid key, not matching");
                throw new InvalidInputException("invalid-key");
            }

            // validate deadline
            Calendar calendar = Calendar.getInstance();
            if (account.getConfirmationDeadline() == null) {
                logger.error("corrupt or used key, missing deadline");
                // do not leak reason
                throw new InvalidInputException("invalid-key");
            }

            boolean isExpired = calendar.after(account.getConfirmationDeadline());

            if (isExpired) {
                logger.error("expired key on " + String.valueOf(account.getConfirmationDeadline()));
                // do not leak reason
                throw new InvalidInputException("invalid-key");
            }

            isValid = isMatch && !isExpired;

            if (isValid) {
                // we set confirm and reset the key, single use
                account.setConfirmed(true);
                account.setConfirmationDeadline(null);
                account.setConfirmationKey(null);
                account = userAccountService.updateAccount(account.getId(), account);
            }

        }

        String username = account.getUsername();

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        return account;

    }

    /*
     * Mail
     */
    private void sendPasswordMail(InternalUserAccount account, String password)
            throws MessagingException {
        if (mailService != null) {
            String realm = getRealm();
            String provider = getProvider();
            String loginUrl = "/login";
            if (uriBuilder != null) {
                loginUrl = uriBuilder.buildUrl(realm, loginUrl);
            }
            String lang = (account.getLang() != null ? account.getLang() : LANG_UNDEFINED);

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("password", password);
            vars.put("url", loginUrl);

            String template = "mail/password";
            if (StringUtils.hasText(lang)) {
                template = template + "_" + lang;
            }

            String subject = mailService.getMessageSource().getMessage(
                    "mail.password_subject", null,
                    Locale.forLanguageTag(lang));

            mailService.sendEmail(account.getEmail(), template, subject, vars);
        }
    }

    private void sendPasswordAndConfirmationMail(InternalUserAccount account, String password, String key)
            throws MessagingException {
        if (mailService != null) {
            String realm = getRealm();
            String provider = getProvider();

            String confirmUrl = InternalIdentityAuthority.AUTHORITY_URL + "confirm/" + provider + "?code="
                    + key;
            if (uriBuilder != null) {
                confirmUrl = uriBuilder.buildUrl(realm, confirmUrl);
            }
            String lang = (account.getLang() != null ? account.getLang() : LANG_UNDEFINED);

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("password", password);
            vars.put("url", confirmUrl);

            String template = "mail/passwordconfirmation";
            if (StringUtils.hasText(lang)) {
                template = template + "_" + lang;
            }

            String subject = mailService.getMessageSource().getMessage(
                    "mail.confirmation_subject", null,
                    Locale.forLanguageTag(lang));

            mailService.sendEmail(account.getEmail(), template, subject, vars);
        }
    }

    private void sendConfirmationMail(InternalUserAccount account, String key) throws MessagingException {
        if (mailService != null) {
            String realm = getRealm();
            String provider = getProvider();

            String confirmUrl = InternalIdentityAuthority.AUTHORITY_URL + "confirm/" + provider + "?code=" + key;
            if (uriBuilder != null) {
                confirmUrl = uriBuilder.buildUrl(realm, confirmUrl);
            }

            String lang = (account.getLang() != null ? account.getLang() : LANG_UNDEFINED);

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("url", confirmUrl);

            String template = "mail/confirmation";
            if (StringUtils.hasText(lang)) {
                template = template + "_" + lang;
            }

            String subject = mailService.getMessageSource().getMessage(
                    "mail.confirmation_subject", null,
                    Locale.forLanguageTag(lang));

            mailService.sendEmail(account.getEmail(), template, subject, vars);
        }
    }

}