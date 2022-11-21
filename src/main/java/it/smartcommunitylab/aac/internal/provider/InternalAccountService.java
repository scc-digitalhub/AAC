package it.smartcommunitylab.aac.internal.provider;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
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
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.utils.MailService;

@Transactional
public class InternalAccountService
        extends
        AbstractConfigurableProvider<InternalUserAccount, ConfigurableAccountProvider, InternalIdentityProviderConfigMap, InternalAccountServiceConfig>
        implements
        AccountService<InternalUserAccount, InternalEditableUserAccount, InternalIdentityProviderConfigMap, InternalAccountServiceConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final UserEntityService userEntityService;
    private ResourceEntityService resourceService;

    private final UserAccountService<InternalUserAccount> userAccountService;
    private final InternalUserConfirmKeyService confirmKeyService;

    private final String repositoryId;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public InternalAccountService(String providerId,
            UserEntityService userEntityService,
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            InternalAccountServiceConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId,
                realm, providerConfig);
        Assert.notNull(userEntityService, "user entity service is mandatory");
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(confirmKeyService, "user confirm service is mandatory");

        // internal data repositories
        this.userEntityService = userEntityService;

        this.userAccountService = userAccountService;
        this.confirmKeyService = confirmKeyService;

        // config
        this.repositoryId = config.getRepositoryId();
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getRegistrationUrl() {
        // TODO filter
        // TODO build a realm-bound url, need updates on filters
        return "/auth/internal/register/" + getProvider();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalUserAccount> listAccounts(String userId) {
        List<InternalUserAccount> accounts = userAccountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> {
            a.setAuthority(getAuthority());
            a.setProvider(getProvider());
        });
        return accounts;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount getAccount(String username) throws NoSuchUserException {
        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccount(String username) {
        return findAccountByUsername(username);
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUsername(String username) {
        InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByUuid(String uuid) {
        InternalUserAccount account = userAccountService.findAccountByUuid(repositoryId, uuid);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Transactional(readOnly = true)
    public InternalUserAccount findAccountByEmail(String email) {
        // we pick first account matching email, repository should contain unique
        // email+provider
        InternalUserAccount account = userAccountService.findAccountByEmail(repositoryId, email).stream()
                .filter(a -> a.isEmailVerified())
                .findFirst()
                .orElse(null);

        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public InternalUserAccount lockAccount(String username) throws NoSuchUserException, RegistrationException {
        logger.debug("lock account with username {}", String.valueOf(username));
        return updateStatus(username, SubjectStatus.LOCKED);
    }

    @Override
    public InternalUserAccount unlockAccount(String username) throws NoSuchUserException, RegistrationException {
        logger.debug("unlock account with username {}", String.valueOf(username));
        return updateStatus(username, SubjectStatus.ACTIVE);
    }

    @Override
    public InternalUserAccount linkAccount(String username, String userId)
            throws NoSuchUserException, RegistrationException {
        logger.debug("link account with username {} to user {}", String.valueOf(username), String.valueOf(userId));

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // re-link to user
        account.setUserId(userId);
        account = userAccountService.updateAccount(repositoryId, username, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public void deleteAccount(String username) throws NoSuchUserException {
        logger.debug("delete account with username {}", String.valueOf(username));

        InternalUserAccount account = findAccountByUsername(username);
        if (account != null) {
            // remove account
            userAccountService.deleteAccount(repositoryId, username);

            if (resourceService != null) {
                // remove resource
                resourceService.deleteResourceEntity(SystemKeys.RESOURCE_ACCOUNT, SystemKeys.AUTHORITY_INTERNAL,
                        getProvider(), username);
            }
        }
    }

    @Override
    public void deleteAccounts(String userId) {
        logger.debug("delete accounts for user {}", String.valueOf(userId));

        List<InternalUserAccount> accounts = userAccountService.findAccountByUser(repositoryId, userId);
        for (InternalUserAccount a : accounts) {
            // remove account
            userAccountService.deleteAccount(repositoryId, a.getUsername());

            if (resourceService != null) {
                // remove resource
                resourceService.deleteResourceEntity(SystemKeys.RESOURCE_ACCOUNT, SystemKeys.AUTHORITY_INTERNAL,
                        getProvider(), a.getUsername());
            }
        }
    }

    @Override
    public InternalUserAccount registerAccount(@Nullable String userId, EditableUserAccount registration)
            throws RegistrationException, NoSuchUserException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("registration is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalEditableUserAccount.class, registration,
                "registration must be an instance of internal editable user account");
        InternalEditableUserAccount reg = (InternalEditableUserAccount) registration;

        logger.debug("register a new account for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        // check email for confirmation when required
        if (config.isConfirmationRequired()) {
            if (reg.getEmail() == null) {
                throw new MissingDataException("email");
            }

            String email = Jsoup.clean(reg.getEmail(), Safelist.none());
            if (!StringUtils.hasText(email)) {
                throw new MissingDataException("email");
            }
        }

        // registration is create but user-initiated
        // build model
        InternalUserAccount ua = new InternalUserAccount();
        ua.setRepositoryId(repositoryId);
        ua.setUsername(reg.getUsername());
        ua.setEmail(reg.getEmail());
        ua.setName(reg.getName());
        ua.setSurname(reg.getSurname());
        ua.setLang(reg.getLang());

        InternalUserAccount account = createAccount(userId, null, ua);
        String username = account.getUsername();

        if (config.isConfirmationRequired() && !account.isConfirmed()) {
            account = verifyAccount(username);
        }

        return account;
    }

    @Override
    public InternalUserAccount createAccount(@Nullable String userId, @Nullable String accountId,
            UserAccount registration)
            throws RegistrationException, NoSuchUserException {
        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserAccount.class, registration,
                "registration must be an instance of internal user account");
        InternalUserAccount reg = (InternalUserAccount) registration;

        logger.debug("create a new account for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        // validate base params, nothing to do when missing
        String emailAddress = reg.getEmailAddress();
        if (StringUtils.hasText(emailAddress)) {
            emailAddress = Jsoup.clean(emailAddress, Safelist.none());
        }

        String username = StringUtils.hasText(accountId) ? accountId : reg.getUsername();
        if (!StringUtils.hasText(username) && StringUtils.hasText(emailAddress)) {
            username = emailAddress;
        }
        if (StringUtils.hasText(username)) {
            username = Jsoup.clean(username, Safelist.none());
        }
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }

        // check for duplicates
        InternalUserAccount account = findAccountByUsername(username);
        if (account != null) {
            throw new AlreadyRegisteredException();
        }

        // we require unique email
        if (StringUtils.hasText(emailAddress)
                && userAccountService.findAccountByEmail(repositoryId, emailAddress).size() > 0) {
            throw new DuplicatedDataException("email");
        }

        // fetch additional params
        String realm = getRealm();

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

        // get uuid and validate
        String uuid = reg.getUuid();
        if (StringUtils.hasText(uuid)) {
            uuid = Jsoup.clean(uuid, Safelist.none());
        }

        if (!StringUtils.hasText(uuid) || uuid.length() < 5) {
            // reset
            uuid = null;
        }

        // we expect subject to be valid, or null if we need to create
        UserEntity user = null;
        if (!StringUtils.hasText(userId)) {
            userId = userEntityService.createUser(realm).getUuid();
            user = userEntityService.addUser(userId, realm, username, emailAddress);
            userId = user.getUuid();
        } else {
            // check if exists
            userEntityService.getUser(userId);
        }

        // create new account
        account = new InternalUserAccount();
        account.setRepositoryId(repositoryId);
        account.setUsername(username);
        account.setUuid(uuid);

        account.setUserId(userId);
        account.setRealm(realm);

        // set account as active
        account.setStatus(SubjectStatus.ACTIVE.getValue());
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);

        // set confirmed
        account.setConfirmed(confirmed);

        account = userAccountService.addAccount(repositoryId, username, account);

        if (resourceService != null) {
            // register as user resource
            resourceService.addResourceEntity(account.getUuid(), SystemKeys.RESOURCE_ACCOUNT,
                    SystemKeys.AUTHORITY_INTERNAL, getProvider(), username);
        }

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    @Override
    public InternalEditableUserAccount getEditableAccount(String username) throws NoSuchUserException {
        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // build editable model
        InternalEditableUserAccount ea = new InternalEditableUserAccount(
                getProvider(), getRealm(),
                account.getUserId(), account.getUuid());
        ea.setUsername(account.getUsername());
        ea.setEmail(account.getEmail());
        ea.setName(account.getName());
        ea.setSurname(account.getSurname());
        ea.setLang(account.getLang());

        return ea;
    }

    @Override
    public InternalUserAccount editAccount(String userId, String accountId, EditableUserAccount registration)
            throws RegistrationException, NoSuchUserException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("registration is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalEditableUserAccount.class, registration,
                "registration must be an instance of internal editable user account");
        InternalEditableUserAccount reg = (InternalEditableUserAccount) registration;

        logger.debug("edit account for user {}", String.valueOf(userId));
        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        // check email for confirmation when required
        if (config.isConfirmationRequired()) {
            if (reg.getEmail() == null) {
                throw new MissingDataException("email");
            }

            String email = Jsoup.clean(reg.getEmail(), Safelist.none());
            if (!StringUtils.hasText(email)) {
                throw new MissingDataException("email");
            }
        }

        // edit is update but user-initiated
        // build model
        InternalUserAccount ua = new InternalUserAccount();
        ua.setRepositoryId(repositoryId);
        ua.setUsername(reg.getUsername());
        ua.setEmail(reg.getEmail());
        ua.setName(reg.getName());
        ua.setSurname(reg.getSurname());
        ua.setLang(reg.getLang());

        return updateAccount(userId, accountId, ua);
    }

    @Override
    public InternalUserAccount updateAccount(String userId, String username, UserAccount registration)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        if (registration == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserAccount.class, registration,
                "registration must be an instance of internal user account");
        InternalUserAccount reg = (InternalUserAccount) registration;

        logger.debug("update account with username {} for user {}", String.valueOf(username), String.valueOf(userId));
        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // TODO evaluate support for userId change
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus) {
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
        logger.debug("verify account with username {}", String.valueOf(username));

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
        logger.debug("confirm account with username {}", String.valueOf(username));

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
        logger.debug("unconfirm account with username {}", String.valueOf(username));

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
        logger.debug("confirm account via key {}", String.valueOf(confirmationKey));

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

    private InternalUserAccount updateStatus(String username, SubjectStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        logger.debug("update account with username {} to status {}", String.valueOf(username),
                String.valueOf(newStatus));

        InternalUserAccount account = findAccountByUsername(username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        SubjectStatus curStatus = SubjectStatus.parse(account.getStatus());
        if (SubjectStatus.INACTIVE == curStatus && SubjectStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // update status
        account.setStatus(newStatus.getValue());
        account = userAccountService.updateAccount(repositoryId, username, account);

        // map to our authority
        account.setAuthority(getAuthority());
        account.setProvider(getProvider());

        return account;
    }

    /*
     * Mail
     */
//    private void sendPasswordMail(InternalUserAccount account, String password)
//            throws MessagingException {
//        if (mailService != null) {
//            String realm = getRealm();
//            String loginUrl = "/login";
//            if (uriBuilder != null) {
//                loginUrl = uriBuilder.buildUrl(realm, loginUrl);
//            }
//
//            Map<String, String> action = new HashMap<>();
//            action.put("url", loginUrl);
//            action.put("text", "action.login");
//
//            Map<String, Object> vars = new HashMap<>();
//            vars.put("user", account);
//            vars.put("password", password);
//            vars.put("action", action);
//            vars.put("realm", account.getRealm());
//
//            String template = "password";
//            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
//        }
//    }
//
//    private void sendPasswordAndConfirmationMail(InternalUserAccount account, String password, String key)
//            throws MessagingException {
//        if (mailService != null) {
//            // action is handled by global filter
//            String provider = getProvider();
//            String confirmUrl = "/auth/" + getAuthority() + "/confirm/" + provider + "?code="
//                    + key;
//            if (uriBuilder != null) {
//                confirmUrl = uriBuilder.buildUrl(null, confirmUrl);
//            }
//
//            Map<String, String> action = new HashMap<>();
//            action.put("url", confirmUrl);
//            action.put("text", "action.confirm");
//
//            Map<String, Object> vars = new HashMap<>();
//            vars.put("user", account);
//            vars.put("password", password);
//            vars.put("action", action);
//            vars.put("realm", account.getRealm());
//
//            // use confirm template and avoid sending temporary password
//            // TODO evaluate with credentials refactoring
//            String template = "confirmation";
//            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
//        }
//    }

    private void sendConfirmationMail(InternalUserAccount account, String key) throws MessagingException {
        if (mailService != null) {
            logger.debug("send confirmation mail for account with username {}", String.valueOf(account.getUsername()));

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

            logger.debug("send confirmation mail to {}", String.valueOf(account.getEmail()));
            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
        } else {
            logger.debug("can't send confirmation mail for account with username {}: no mail service",
                    String.valueOf(account.getUsername()));
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