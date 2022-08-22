package it.smartcommunitylab.aac.internal.provider;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.utils.MailService;

@Transactional
public class InternalAccountService extends InternalAccountProvider
        implements AccountService<InternalUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final InternalIdentityProviderConfig config;

    private final UserAccountService<InternalUserAccount> userAccountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    private final String repositoryId;

    private final SubjectService subjectService;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public InternalAccountService(String providerId,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            SubjectService subjectService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        this(SystemKeys.AUTHORITY_INTERNAL, providerId, userAccountService, confirmKeyService, subjectService,
                providerConfig, realm);
    }

    public InternalAccountService(String authority, String providerId,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            SubjectService subjectService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        super(authority, providerId, userAccountService, providerConfig, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.userAccountService = userAccountService;
        this.confirmKeyService = confirmKeyService;
        this.subjectService = subjectService;
        this.config = providerConfig;

        this.repositoryId = providerConfig.getRepositoryId();
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Override
    public void deleteAccount(String username) throws NoSuchUserException {
        InternalUserAccount account = findAccountByUsername(username);

        if (account != null) {
            String uuid = account.getUuid();
            if (uuid != null) {
                // remove subject if exists
                subjectService.deleteSubject(uuid);
            }

            // remove account
            userAccountService.deleteAccount(repositoryId, username);
        }
    }

    @Override
    public InternalUserAccount createAccount(@Valid InternalUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (reg == null) {
            throw new RegistrationException();
        }

        String userId = reg.getUserId();

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        String realm = getRealm();

        // extract base fields
        String username = Jsoup.clean(reg.getUsername(), Safelist.none());

        // validate username
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }

        InternalUserAccount account = findAccountByUsername(username);
        if (account != null) {
            throw new AlreadyRegisteredException();
        }

        // check type and extract our parameters if present
        String email = null;
        String name = null;
        String surname = null;
        String lang = null;
        boolean confirmed = !config.isConfirmationRequired();

        email = clean(reg.getEmail());
        name = clean(reg.getName());
        surname = clean(reg.getSurname());
        lang = clean(reg.getLang());

        // we accept confirmed accounts
        if (!confirmed) {
            confirmed = reg.isConfirmed();
        }

        // we require unique email
        if (StringUtils.hasText(email) && userAccountService.findAccountByEmail(repositoryId, email).size() > 0) {
            throw new DuplicatedDataException("email");
        }

        // generate uuid and register as subject
        String uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
        Subject s = subjectService.addSubject(uuid, realm, SystemKeys.RESOURCE_ACCOUNT, username);

        account = new InternalUserAccount();
        account.setProvider(repositoryId);
        account.setUsername(username);
        account.setUuid(s.getSubjectId());

        account.setUserId(userId);
        account.setRealm(realm);

        // set account as active
        account.setStatus(UserStatus.ACTIVE.getValue());
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);

        // set confirmed
        account.setConfirmed(confirmed);

        account = userAccountService.addAccount(repositoryId, username, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public InternalUserAccount updateAccount(String username, InternalUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        // TODO remove check here, should be on idp only
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // TODO evaluate username change (will require alignment of related model)

        String email = clean(reg.getEmail());
        String name = clean(reg.getName());
        String surname = clean(reg.getSurname());
        String lang = clean(reg.getLang());

        // check if email changes
        boolean emailChanged = false;
        boolean emailConfirm = false;

        if ((account.getEmail() != null && email == null) || (account.getEmail() == null && email == null)) {
            emailChanged = true;
        } else if (account.getEmail() == null && email != null) {
            // new email, check
            emailChanged = true;
        } else if (account.getEmail() != null && email != null) {
            // check if new
            emailChanged = !account.getEmail().equals(email);
        }

        if (emailChanged) {
            // always reset confirmed flag
            account.setConfirmed(false);

            // if set to value, check if unique
            if (StringUtils.hasText(email)) {
                if (userAccountService.findAccountByEmail(repositoryId, email).size() > 0) {
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

        account = userAccountService.updateAccount(repositoryId, username, account);

        if (emailConfirm && config.isConfirmationRequired()) {
            account = verifyAccount(username);
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public InternalUserAccount verifyAccount(String username)
            throws NoSuchUserException, RegistrationException {

        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // build confirm key
        String confirmationKey = generateKey();

        // we set deadline as +N seconds
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, config.getConfirmationValidity());

        account.setConfirmationDeadline(calendar.getTime());
        account.setConfirmationKey(confirmationKey);

        // override confirm
        account.setConfirmed(false);

        account = userAccountService.updateAccount(repositoryId, username, account);

        // send mail
        try {
            // send only confirmation link
            sendConfirmationMail(account, account.getConfirmationKey());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public InternalUserAccount confirmAccount(String username)
            throws NoSuchUserException, RegistrationException {

        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // override confirm
        account.setConfirmed(true);
        account.setConfirmationDeadline(null);
        account.setConfirmationKey(null);

        account = userAccountService.updateAccount(repositoryId, username, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public InternalUserAccount unconfirmAccount(String username)
            throws NoSuchUserException, RegistrationException {

        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // override confirm
        account.setConfirmed(false);
        account.setConfirmationDeadline(null);
        account.setConfirmationKey(null);

        account = userAccountService.updateAccount(repositoryId, username, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

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

//    @Override
//    public UserAccount blockAccount(String username) throws NoSuchUserException, RegistrationException {
//        return updateStatus(username, UserStatus.BLOCKED);
//    }
//
//    @Override
//    public UserAccount unblockAccount(String username) throws NoSuchUserException, RegistrationException {
//        return updateStatus(username, UserStatus.ACTIVE);
//    }

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

    public InternalUserAccount confirmAccountViaKey(String confirmationKey) throws NoSuchUserException {

        if (!StringUtils.hasText(confirmationKey)) {
            throw new IllegalArgumentException("empty-key");
        }

        InternalUserAccount account = confirmKeyService.findAccountByConfirmationKey(repositoryId, confirmationKey);
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
                account = userAccountService.updateAccount(repositoryId, username, account);
            }

        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;

    }

    public String generateKey() {
        // TODO evaluate usage of a secure key generator
        String rnd = UUID.randomUUID().toString();
        return rnd;
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
            String confirmUrl = "/auth/" + getAuthority() + "/confirm/" + provider + "?code="
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

            String confirmUrl = "/auth/" + getAuthority() + "/confirm/" + provider + "?code="
                    + key;
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

    /*
     * string cleanup
     */

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