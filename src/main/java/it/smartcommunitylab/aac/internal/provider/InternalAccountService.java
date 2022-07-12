package it.smartcommunitylab.aac.internal.provider;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import javax.validation.Valid;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.utils.MailService;

@Transactional
public class InternalAccountService extends AbstractProvider implements AccountService<InternalUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // use password service to handle password
    private final InternalPasswordService passwordService;

    // provider configuration
    private final InternalIdentityProviderConfig config;

    private final InternalUserAccountService userAccountService;
    private final SubjectService subjectService;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public InternalAccountService(String providerId,
            InternalUserAccountService userAccountService, SubjectService subjectService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.userAccountService = userAccountService;
        this.subjectService = subjectService;
        this.config = providerConfig;

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
    @Transactional(readOnly = true)
    public List<InternalUserAccount> listAccounts(String userId) {
        return userAccountService.findByUser(userId, getProvider());
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserAccount findAccount(String username) {
        return findAccountByUsername(username);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUuid(String uuid) {
        String provider = getProvider();
        return userAccountService.findAccountByUuid(provider, uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserAccount getAccount(String username) throws NoSuchUserException {
        String provider = getProvider();

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUsername(String username) {
        String provider = getProvider();
        return userAccountService.findAccountByUsername(provider, username);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByEmail(String email) {
        String provider = getProvider();

        // pick first result, we enforce single email per provider at registration
        return userAccountService.findAccountByEmail(provider, email).stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public void deleteAccount(String username) throws NoSuchUserException {
        String provider = getProvider();

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);

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
    public InternalUserAccount registerAccount(String userId, @Valid InternalUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        String provider = getProvider();
        String realm = getRealm();

        // extract base fields
        String username = Jsoup.clean(reg.getUsername(), Safelist.none());

        // validate username
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account != null) {
            throw new AlreadyRegisteredException();
        }

        // check type and extract our parameters if present
        String password = null;
        String email = null;
        String name = null;
        String surname = null;
        String lang = null;
        boolean confirmed = !config.isConfirmationRequired();

        password = reg.getPassword();
        email = reg.getEmail();
        name = reg.getName();
        surname = reg.getSurname();
        lang = reg.getLang();

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
            confirmed = reg.isConfirmed();
        }

        if (!confirmed && config.isConfirmationRequired() && !StringUtils.hasText(email)) {
            throw new MissingDataException("email");
        }

        // we require unique email
        if (StringUtils.hasText(email) && userAccountService.findAccountByEmail(provider, email).size() > 0) {
            throw new DuplicatedDataException("email");
        }

        boolean changeOnFirstAccess = false;
        if (!StringUtils.hasText(password)) {
            password = passwordService.generatePassword();
            changeOnFirstAccess = true;
        } else {
            passwordService.validatePassword(password);
        }

        // generate uuid and register as subject
        String uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
        Subject s = subjectService.addSubject(uuid, realm, SystemKeys.RESOURCE_ACCOUNT, username);

        account = new InternalUserAccount();
        account.setProvider(provider);
        account.setUsername(username);
        account.setUuid(s.getSubjectId());

        account.setUserId(userId);
        account.setRealm(realm);
        // by default disable login
        account.setPassword(null);
        // set account as active
        account.setStatus(UserStatus.ACTIVE.getValue());
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

        account = passwordService.setPassword(username, password, changeOnFirstAccess);

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
        return account;

    }

    @Override
    public InternalUserAccount updateAccount(String username, InternalUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String provider = getProvider();

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        String email = reg.getEmail();
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

        if ((account.getEmail() != null && email == null) || (account.getEmail() == null && email == null)) {
            emailChanged = true;
        } else if (account.getEmail() == null && email != null) {
            // new email, check
            emailChanged = true;
        } else if (account.getEmail() == null && email != null) {
            // check if new
            emailChanged = !account.getEmail().equals(email);
        }

        if (emailChanged) {
            // always reset confirmed flag
            account.setConfirmed(false);

            // if set to value, check if unique
            if (StringUtils.hasText(email)) {
                if (userAccountService.findAccountByEmail(provider, email).size() > 0) {
                    throw new DuplicatedDataException("email");
                }

                emailConfirm = true;
            }

        }

        // we update all props, even if empty or null
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);

        account = userAccountService.updateAccount(provider, username, account);

        if (emailConfirm && config.isConfirmationRequired()) {
            // build confirm key
            String confirmationKey = passwordService.generateKey();

            // we set deadline as +N seconds
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, config.getConfirmationValidity());

            account.setConfirmationDeadline(calendar.getTime());
            account.setConfirmationKey(confirmationKey);

            // send mail
            try {
                // send only confirmation link
                sendConfirmationMail(account, account.getConfirmationKey());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        return account;
    }

    @Override
    public InternalUserAccount linkAccount(String username, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect subject to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        String provider = getProvider();

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);
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
    public InternalUserAccount verifyAccount(String username)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String provider = getProvider();

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);
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
    public InternalUserAccount unverifyAccount(String username)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String provider = getProvider();

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);
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

    /*
     * Administrative ops
     */
//    @Override
//    public UserAccount activateAccount(String username) throws NoSuchUserException, RegistrationException {
//        return updateStatus(username, UserStatus.ACTIVE);
//    }
//
//    @Override
//    public UserAccount inactivateAccount(String username) throws NoSuchUserException, RegistrationException {
//        return updateStatus(username, UserStatus.INACTIVE);
//    }

    @Override
    public InternalUserAccount lockAccount(String username) throws NoSuchUserException, RegistrationException {
        return updateStatus(username, UserStatus.LOCKED);
    }

    @Override
    public InternalUserAccount unlockAccount(String username) throws NoSuchUserException, RegistrationException {
        return updateStatus(username, UserStatus.ACTIVE);
    }

//    @Override
//    public UserAccount blockAccount(String username) throws NoSuchUserException, RegistrationException {
//        return updateStatus(username, UserStatus.BLOCKED);
//    }
//
//    @Override
//    public UserAccount unblockAccount(String username) throws NoSuchUserException, RegistrationException {
//        return updateStatus(username, UserStatus.ACTIVE);
//    }

    private InternalUserAccount updateStatus(String username, UserStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        InternalUserAccount account = userAccountService.findAccountByUsername(provider, username);
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

//    public InternalUserAccount resetConfirm(String userId)
//            throws NoSuchUserException {
//        if (!config.isEnableUpdate()) {
//            throw new IllegalArgumentException("delete is disabled for this provider");
//        }
//
//        String username = parseResourceId(userId);
//        String realm = getRealm();
//
//        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
//        if (account == null) {
//            throw new NoSuchUserException();
//        }
//
//        // build key
//        String confirmationKey = passwordService.generateKey();
//
//        // we set deadline as +N seconds
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.SECOND, config.getConfirmationValidity());
//
//        account.setConfirmed(false);
//        account.setConfirmationDeadline(calendar.getTime());
//        account.setConfirmationKey(confirmationKey);
//
//        account = userAccountService.updateAccount(account.getId(), account);
//
//        // set providerId since all internal accounts have the same
//        account.setProvider(getProvider());
//
//        // rewrite internal userId
//        account.setUserId(exportInternalId(username));
//
//        return account;
//    }

    public InternalUserAccount confirmAccount(String confirmationKey) throws NoSuchUserException {

        if (!StringUtils.hasText(confirmationKey)) {
            throw new IllegalArgumentException("empty-key");
        }
        String provider = getProvider();
        InternalUserAccount account = userAccountService.findAccountByConfirmationKey(provider, confirmationKey);
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
                throw new InvalidDataException("key");
            }

            // validate deadline
            Calendar calendar = Calendar.getInstance();
            if (account.getConfirmationDeadline() == null) {
                logger.error("corrupt or used key, missing deadline");
                // do not leak reason
                throw new InvalidDataException("key");
            }

            boolean isExpired = calendar.after(account.getConfirmationDeadline());

            if (isExpired) {
                logger.error("expired key on " + String.valueOf(account.getConfirmationDeadline()));
                // do not leak reason
                throw new InvalidDataException("key");
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

    /*
     * Mail
     */
    private void sendPasswordMail(InternalUserAccount account, String password)
            throws MessagingException {
        if (mailService != null) {
            String realm = getRealm();
            String loginUrl = "/login";
            if (uriBuilder != null) {
                loginUrl = uriBuilder.buildUrl(realm, loginUrl);
            }

            Map<String, String> action = new HashMap<>();
            action.put("url", loginUrl);
            action.put("text", "action.login");

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("password", password);
            vars.put("action", action);
            vars.put("realm", account.getRealm());

            String template = "password";
            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
        }
    }

    private void sendPasswordAndConfirmationMail(InternalUserAccount account, String password, String key)
            throws MessagingException {
        if (mailService != null) {
            // action is handled by global filter
            String provider = getProvider();
            String confirmUrl = InternalIdentityAuthority.AUTHORITY_URL + "confirm/" + provider + "?code="
                    + key;
            if (uriBuilder != null) {
                confirmUrl = uriBuilder.buildUrl(null, confirmUrl);
            }

            Map<String, String> action = new HashMap<>();
            action.put("url", confirmUrl);
            action.put("text", "action.confirm");

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("password", password);
            vars.put("action", action);
            vars.put("realm", account.getRealm());

            // use confirm template and avoid sending temporary password
            // TODO evaluate with credentials refactoring
            String template = "confirmation";
            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
        }
    }

    private void sendConfirmationMail(InternalUserAccount account, String key) throws MessagingException {
        if (mailService != null) {
            // action is handled by global filter
            String provider = getProvider();

            String confirmUrl = InternalIdentityAuthority.AUTHORITY_URL + "confirm/" + provider + "?code=" + key;
            if (uriBuilder != null) {
                confirmUrl = uriBuilder.buildUrl(null, confirmUrl);
            }

            Map<String, String> action = new HashMap<>();
            action.put("url", confirmUrl);
            action.put("text", "action.confirm");

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("action", action);
            vars.put("realm", account.getRealm());

            String template = "confirmation";
            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
        }
    }

}