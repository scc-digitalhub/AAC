package it.smartcommunitylab.aac.webauthn.provider;

import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.validation.Valid;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidInputException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.utils.MailService;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnAccountService extends AbstractProvider implements AccountService<WebAuthnUserAccount> {

    private static final String LANG_UNDEFINED = "en";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebAuthnUserAccountService userAccountService;
    private final WebAuthnIdentityProviderConfig config;

    private final SubjectService subjectService;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    private StringKeyGenerator keyGenerator;

    public WebAuthnAccountService(String providerId,
            WebAuthnUserAccountService userAccountService, SubjectService subjectService,
            WebAuthnIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.userAccountService = userAccountService;
        this.subjectService = subjectService;
        this.config = providerConfig;

        // build default keyGen
        keyGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 64);

    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    public void setKeyGenerator(StringKeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnUserAccount> listAccounts(String userId) {
        return userAccountService.listAccountsByUser(userId, getProvider());

    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccount(String username) {
        return findAccountByUsername(username);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByUsername(String username) {
        String provider = getProvider();
        return userAccountService.findAccountByUsername(provider, username);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByUserHandle(String userHandle) {
        String provider = getProvider();
        return userAccountService.findAccountByUserHandle(provider, userHandle);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByUuid(String uuid) {
        String provider = getProvider();
        return userAccountService.findAccountByUuid(provider, uuid);
    }

    @Transactional(readOnly = true)
    public WebAuthnUserAccount findAccountByEmailAddress(String email) {
        String provider = getProvider();
        // pick first result, we enforce single email per provider at registration
        return userAccountService.findAccountByEmailAddress(provider, email).stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public WebAuthnUserAccount getAccount(String username) throws NoSuchUserException {
        String provider = getProvider();
        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Override
    public void deleteAccount(String username) throws NoSuchUserException {
        String provider = getProvider();

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);

        if (account != null) {
            String uuid = account.getUuid();
            if (uuid != null) {
                // remove subject if exists
                subjectService.deleteSubject(uuid);
            }

            // remove account
            userAccountService.deleteAccount(provider, username);
        }
    }

    @Override
    public WebAuthnUserAccount registerAccount(String userId, @Valid WebAuthnUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new RegistrationException("missing-user");
        }

        String provider = getProvider();
        String realm = getRealm();

        // extract base fields
        String username = Jsoup.clean(reg.getUsername(), Safelist.none());
        String userHandle = Jsoup.clean(reg.getUserHandle(), Safelist.none());

        // validate
        if (!StringUtils.hasText(username)) {
            throw new RegistrationException("missing-username");
        }
        if (!StringUtils.hasText(userHandle)) {
            // build a new key as handle
            userHandle = keyGenerator.generateKey();
        }

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        // additional attributes
        String email = reg.getEmailAddress();
        String name = reg.getName();
        String surname = reg.getSurname();
        String lang = reg.getLang();

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

        // we require a unique email
        if (StringUtils.hasText(email) && userAccountService.findAccountByEmailAddress(provider, email).size() > 0) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        // generate uuid and register as subject
        String uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
        Subject s = subjectService.addSubject(uuid, realm, SystemKeys.RESOURCE_ACCOUNT, username);

        account = new WebAuthnUserAccount();
        account.setProvider(provider);
        account.setUsername(username);
        account.setUuid(s.getSubjectId());
        account.setUserId(userId);
        account.setRealm(realm);

        account.setRealm(getRealm());
        account.setUsername(username);
        account.setEmailAddress(email);

        // set account as active
        account.setStatus(UserStatus.ACTIVE.getValue());

        // attributes
        account.setEmailAddress(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);

        // require confirmation
        account.setConfirmed(false);

        if (config.isEnableConfirmation()) {
            // build key
            String confirmationKey = generateKey();

            // we set deadline as +N seconds
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, config.getConfirmationValidity());

            account.setConfirmationDeadline(calendar.getTime());
            account.setConfirmationKey(confirmationKey);
        }

        account = userAccountService.addAccount(provider, account);

        // send mail
        if (config.isEnableConfirmation()) {
            try {
                // send confirmation link
                sendConfirmationMail(account, account.getConfirmationKey());

            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return account;

    }

    @Override
    public WebAuthnUserAccount updateAccount(String username, WebAuthnUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }
        String provider = getProvider();

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        String email = reg.getEmailAddress();
        String name = reg.getName();
        String surname = reg.getSurname();
        String lang = reg.getLang();

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
        // check if email changes
        boolean emailChanged = false;
        boolean emailConfirm = false;

        if ((account.getEmailAddress() != null && email == null)
                || (account.getEmailAddress() == null && email == null)) {
            emailChanged = true;
        } else if (account.getEmailAddress() == null && email != null) {
            // new email, check
            emailChanged = true;
        } else if (account.getEmailAddress() == null && email != null) {
            // check if new
            emailChanged = !account.getEmailAddress().equals(email);
        }

        if (emailChanged) {
            // always reset confirmed flag
            account.setConfirmed(false);

            // if set to value, check if unique
            if (StringUtils.hasText(email)) {
                if (userAccountService.findAccountByEmailAddress(provider, email).size() > 0) {
                    throw new AlreadyRegisteredException("duplicate-registration");
                }

                emailConfirm = true;
            }

        }

        // we update all props, even if empty or null
        account.setEmailAddress(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);

        account = userAccountService.updateAccount(provider, username, account);

        if (emailConfirm) {
            // build confirm key
            String confirmationKey = generateKey();

            // we set deadline as +N seconds
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, config.getConfirmationValidity());

            account.setConfirmationDeadline(calendar.getTime());
            account.setConfirmationKey(confirmationKey);

            // send mail
            if (config.isEnableConfirmation()) {
                try {
                    // send confirmation link
                    sendConfirmationMail(account, account.getConfirmationKey());

                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }

        return account;
    }

    @Override
    public WebAuthnUserAccount linkAccount(String username, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect subject to be valid
        if (!StringUtils.hasText(userId)) {
            throw new RegistrationException("missing-user");
        }

        String provider = getProvider();

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);
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
        account = userAccountService.updateAccount(provider, username, account);
        return account;
    }

    @Override
    public WebAuthnUserAccount verifyAccount(String username)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String provider = getProvider();

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // verify will override confirm
        account.setConfirmed(true);
        account.setConfirmationDeadline(null);
        account.setConfirmationKey(null);

        account = userAccountService.updateAccount(provider, username, account);
        return account;
    }

    @Override
    public WebAuthnUserAccount unverifyAccount(String username)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String provider = getProvider();

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // reset will override confirm
        account.setConfirmed(false);
        account.setConfirmationDeadline(null);
        account.setConfirmationKey(null);

        account = userAccountService.updateAccount(provider, username, account);
        return account;
    }

    @Override
    public WebAuthnUserAccount lockAccount(String username) throws NoSuchUserException, RegistrationException {
        return updateStatus(username, UserStatus.LOCKED);
    }

    @Override
    public WebAuthnUserAccount unlockAccount(String username) throws NoSuchUserException, RegistrationException {
        return updateStatus(username, UserStatus.ACTIVE);
    }

    private WebAuthnUserAccount updateStatus(String username, UserStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(provider, username);
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
        account = userAccountService.updateAccount(provider, username, account);
        return account;
    }

    public WebAuthnUserAccount confirmAccount(String confirmationKey) throws NoSuchUserException {

        if (!StringUtils.hasText(confirmationKey)) {
            throw new IllegalArgumentException("empty-key");
        }
        String provider = getProvider();
        WebAuthnUserAccount account = userAccountService.findAccountByConfirmationKey(provider, confirmationKey);
        if (account == null) {
            throw new NoSuchUserException();
        }

        String username = account.getUsername();

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
                account = userAccountService.updateAccount(provider, username, account);
            }

        }

        return account;

    }

    private void sendConfirmationMail(WebAuthnUserAccount account, String key) throws MessagingException {
        if (mailService != null) {
            // action is handled by global filter
            String realm = null;
            String provider = getProvider();

            String confirmUrl = WebAuthnIdentityAuthority.AUTHORITY_URL + "confirm/" + provider + "?code=" + key;
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

            mailService.sendEmail(account.getEmailAddress(), template, subject, vars);
        }
    }

    private String generateKey() {
        // TODO evaluate usage of a secure key generator
        String rnd = UUID.randomUUID().toString();
        return rnd;
    }

}
