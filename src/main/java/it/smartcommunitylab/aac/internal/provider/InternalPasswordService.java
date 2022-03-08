package it.smartcommunitylab.aac.internal.provider;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidInputException;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.dto.PasswordPolicy;
import it.smartcommunitylab.aac.internal.model.UserPasswordCredentials;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalPasswordService extends AbstractProvider implements UserCredentialsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserAccountService accountService;

    // provider configuration
    private final InternalIdentityProviderConfig config;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private PasswordHash hasher;

    public InternalPasswordService(String providerId, InternalUserAccountService userAccountService,
            InternalIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        this.accountService = userAccountService;
        this.config = providerConfig;
        this.hasher = new PasswordHash();
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    @Override
    public boolean canRead() {
        // password are encrypted, can't read
        return false;
    }

    @Override
    public boolean canSet() {
        return config.isEnablePasswordSet();
    }

    @Override
    public boolean canReset() {
        return config.isEnablePasswordReset();
    }

    public PasswordPolicy getPasswordPolicy() {
        // return new every time to avoid corruption
        PasswordPolicy policy = new PasswordPolicy();
        policy.setPasswordMinLength(config.getPasswordMinLength());
        policy.setPasswordMaxLength(config.getPasswordMaxLength());
        policy.setPasswordRequireAlpha(config.isPasswordRequireAlpha());
        policy.setPasswordRequireNumber(config.isPasswordRequireNumber());
        policy.setPasswordRequireSpecial(config.isPasswordRequireSpecial());
        policy.setPasswordSupportWhitespace(config.isPasswordSupportWhitespace());
        return policy;
    }

    @Override
    public UserPasswordCredentials getCredentials(String username) throws NoSuchUserException {
        // fetch user
        String provider = getProvider();
        InternalUserAccount account = accountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // password are encrypted, can't read
        // we return a placeholder to describe config
        UserPasswordCredentials credentials = new UserPasswordCredentials(getAuthority(), provider, getRealm(),
                account.getUserId());
        return credentials;
    }

    @Override
    public UserPasswordCredentials setCredentials(String username, UserCredentials credentials)
            throws NoSuchUserException {
        if (!config.isEnablePasswordSet()) {
            throw new IllegalArgumentException("set is disabled for this provider");
        }

        // we support only password
        if (!(credentials instanceof UserPasswordCredentials)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String provider = getProvider();
        InternalUserAccount account = accountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if userId matches account
        if (!account.getUserId().equals(credentials.getUserId())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String password = ((UserPasswordCredentials) credentials).getPassword();
        validatePassword(password);

        account = setPassword(username, password, false);

        // we return a placeholder to describe config
        UserPasswordCredentials result = new UserPasswordCredentials(getAuthority(), getProvider(), getRealm(),
                account.getUserId());

        return result;
    }

    @Override
    public UserPasswordCredentials resetCredentials(String username) throws NoSuchUserException {
        if (!config.isEnablePasswordReset()) {
            throw new IllegalArgumentException("reset is disabled for this provider");
        }

        // direct reset credentials to a new password, single use
        String password = generatePassword();

        InternalUserAccount account = setPassword(username, password, true);

        // we return a placeholder to describe config
        UserPasswordCredentials result = new UserPasswordCredentials(getAuthority(), getProvider(), getRealm(),
                account.getUserId());

        // send mail
        try {
            sendPasswordMail(account, password);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return result;

    }

    @Override
    public String getResetUrl() {
        // return link for resetting credentials
        return InternalIdentityAuthority.AUTHORITY_URL + "reset/" + getProvider();
    }

    @Override
    public String getSetUrl() throws NoSuchUserException {
        // internal controller route
        return "/changepwd";
    }
    /*
     * Manage
     */

    public boolean verifyPassword(String username, String password) throws NoSuchUserException {
        // fetch user
        String provider = getProvider();

        InternalUserAccount account = accountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // verify match
            return hasher.validatePassword(password, account.getPassword());

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }

    }

    public InternalUserAccount setPassword(
            String username,
            String password,
            boolean changeOnFirstAccess) throws NoSuchUserException {

        // fetch user
        String provider = getProvider();
        InternalUserAccount account = accountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        try {
            // encode password
            String hash = hasher.createHash(password);

            // set password already hashed
            account.setPassword(hash);
            account.setChangeOnFirstAccess(changeOnFirstAccess);

            return accountService.updateAccount(provider, username, account);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }

    }

    public InternalUserAccount resetPassword(String username) throws NoSuchUserException {
        if (!config.isEnablePasswordReset()) {
            throw new IllegalArgumentException("reset is disabled for this provider");
        }

        // fetch user
        String provider = getProvider();
        InternalUserAccount account = accountService.findAccountByUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // generate and set a reset key
        String resetKey = generateKey();

        // we set deadline as +N seconds
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, config.getPasswordResetValidity());

        account.setResetDeadline(calendar.getTime());
        account.setResetKey(resetKey);

        // send mail
        try {
            sendResetMail(account, resetKey);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return accountService.updateAccount(provider, username, account);

    }

    public InternalUserAccount confirmReset(String resetKey) throws NoSuchUserException {
        if (!StringUtils.hasText(resetKey)) {
            throw new IllegalArgumentException("empty-key");
        }

        String provider = getProvider();
        InternalUserAccount account = accountService.findAccountByResetKey(provider, resetKey);
        if (account == null) {
            throw new NoSuchUserException();
        }
        String username = account.getUsername();

        // validate key, we do it simple
        boolean isValid = false;

        // validate key match
        // useless check since we fetch account with key as input..
        boolean isMatch = resetKey.equals(account.getResetKey());

        if (!isMatch) {
            logger.error("invalid key, not matching");
            throw new InvalidInputException("invalid-key");
        }

        // validate deadline
        Calendar calendar = Calendar.getInstance();
        if (account.getResetDeadline() == null) {
            logger.error("corrupt or used key, missing deadline");
            // do not leak reason
            throw new InvalidInputException("invalid-key");
        }

        boolean isExpired = calendar.after(account.getResetDeadline());

        if (isExpired) {
            logger.error("expired key on " + String.valueOf(account.getResetDeadline()));
            // do not leak reason
            throw new InvalidInputException("invalid-key");
        }

        isValid = isMatch && !isExpired;

        if (!isValid) {
            throw new InvalidInputException("invalid-key");
        }

        // we clear keys and reset password to lock login
        account.setResetDeadline(null);
        account.setResetKey(null);

        // users need to change the password during this session or reset again
        // we want to lock login with old password from now on
        String password = null;
        try {
            password = hasher.createHash(generatePassword());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error(e.getMessage());
        }

        account.setPassword(password);
        account.setChangeOnFirstAccess(true);

        account = accountService.updateAccount(provider, username, account);
        return account;

    }

    public String generatePassword() {
        return RandomStringUtils.random(config.getPasswordMinLength(), true,
                config.isPasswordRequireNumber());
    }

    public void validatePassword(String password) throws InvalidPasswordException {

        if (!StringUtils.hasText(password)) {
            throw new InvalidPasswordException("empty");
        }

        if (password.length() < config.getPasswordMinLength()) {
            throw new InvalidPasswordException("min-length");
        }

        if (password.length() > config.getPasswordMaxLength()) {
            throw new InvalidPasswordException("max-length");
        }

        if (config.isPasswordRequireAlpha()) {
            if (!password.chars().anyMatch(c -> Character.isLetter(c))) {
                throw new InvalidPasswordException("require-alpha");
            }
        }

        if (config.isPasswordRequireNumber()) {
            if (!password.chars().anyMatch(c -> Character.isDigit(c))) {
                throw new InvalidPasswordException("require-number");
            }
        }

        if (config.isPasswordRequireSpecial()) {
            // we do not count whitespace as special char
            if (!password.chars().anyMatch(c -> (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)))) {
                throw new InvalidPasswordException("require-special");
            }
        }

        if (!config.isPasswordSupportWhitespace()) {
            if (password.chars().anyMatch(c -> Character.isWhitespace(c))) {
                throw new InvalidPasswordException("contains-whitespace");
            }
        }

    }

    /*
     * Keys
     */

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
            String lang = (account.getLang() != null ? account.getLang() : "en");

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

    private void sendResetMail(InternalUserAccount account, String key) throws MessagingException {
        if (mailService != null) {
            // action is handled by global filter
            String realm = null;
            String provider = getProvider();
            String confirmUrl = InternalIdentityAuthority.AUTHORITY_URL + "doreset/" + provider + "?code=" + key;
            if (uriBuilder != null) {
                confirmUrl = uriBuilder.buildUrl(realm, confirmUrl);
            }
            String lang = (account.getLang() != null ? account.getLang() : "en");

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("url", confirmUrl);

            String template = "mail/reset";
            if (StringUtils.hasText(lang)) {
                template = template + "_" + lang;
            }

            String subject = mailService.getMessageSource().getMessage(
                    "mail.reset_subject", null,
                    Locale.forLanguageTag(lang));

            mailService.sendEmail(account.getEmail(), template, subject, vars);
        }
    }

}