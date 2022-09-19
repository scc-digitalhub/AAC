package it.smartcommunitylab.aac.password.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.service.InternalUserPasswordService;
import it.smartcommunitylab.aac.utils.MailService;

public class PasswordCredentialsService extends
        AbstractConfigurableProvider<InternalUserPassword, ConfigurableCredentialsService, PasswordCredentialsServiceConfigMap, PasswordCredentialsServiceConfig>
        implements
        UserCredentialsService<InternalUserPassword, PasswordCredentialsServiceConfigMap, PasswordCredentialsServiceConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalUserPasswordService passwordService;

    // provider configuration
    private final PasswordCredentialsServiceConfig config;
    private final String repositoryId;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public PasswordCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> userAccountService,
            InternalUserPasswordService passwordService,
            PasswordCredentialsServiceConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, realm, providerConfig);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.repositoryId = providerConfig.getRepositoryId();
        logger.debug("create webauthn credentials service with id {} repository {}", String.valueOf(providerId),
                repositoryId);
        this.config = providerConfig;

        this.accountService = userAccountService;
        this.passwordService = passwordService;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    public boolean canSet() {
        return config.isEnablePasswordSet();
    }

    public boolean canReset() {
        return config.isEnablePasswordReset();
    }

    public boolean canRevoke() {
        return true;
    }

    public PasswordPolicy getPasswordPolicy() {
        // return new every time to avoid corruption
        PasswordPolicy policy = new PasswordPolicy();
        policy.setPasswordMinLength(config.getPasswordMinLength());
        policy.setPasswordMaxLength(config.getPasswordMaxLength());
        policy.setPasswordRequireAlpha(config.isPasswordRequireAlpha());
        policy.setPasswordRequireUppercaseAlpha(config.isPasswordRequireUppercaseAlpha());
        policy.setPasswordRequireNumber(config.isPasswordRequireNumber());
        policy.setPasswordRequireSpecial(config.isPasswordRequireSpecial());
        policy.setPasswordSupportWhitespace(config.isPasswordSupportWhitespace());
        return policy;
    }

    public String getPasswordPattern() {
        // translate policy to input pattern
        StringBuilder sb = new StringBuilder();
        if (config.isPasswordRequireAlpha()) {
            // require alpha means any, we add pattern for [a-z]
            // TODO fix pattern
            sb.append("(?=.*[a-z])");
        }
        if (config.isPasswordRequireUppercaseAlpha()) {
            sb.append("(?=.*[A-Z])");
        }
        if (config.isPasswordRequireNumber()) {
            sb.append("(?=.*\\d)");
        }
        if (config.isPasswordRequireSpecial()) {
            // TODO
        }

        // add length
        sb
                .append(".{")
                .append(config.getPasswordMinLength()).append(",").append(config.getPasswordMaxLength())
                .append("}");

        return sb.toString();
    }

    public boolean verifyPassword(String username, String password) throws NoSuchUserException {
        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return passwordService.verifyPassword(repositoryId, username, password);
    }

    public String findUsernameByEmail(String email) {
        InternalUserAccount account = accountService.findAccountByEmail(repositoryId, email).stream()
                .findFirst().orElse(null);
        if (account == null) {
            return null;
        }

        return account.getUsername();
    }
    /*
     * Credentials
     */

    @Override
    @Transactional(readOnly = true)
    public InternalUserPassword getCredentials(String username) throws NoSuchUserException {
        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        InternalUserPassword pass = passwordService.findPassword(repositoryId, username);
        if (pass != null) {
            // password are encrypted, can't read
            // we return a placeholder to describe status
            pass.eraseCredentials();
        }

        return pass;
    }

    @Override
    public InternalUserPassword setCredentials(String username, UserCredentials cred)
            throws NoSuchUserException {
        if (!config.isEnablePasswordSet()) {
            throw new IllegalArgumentException("set is disabled for this provider");
        }

        // we support only password
        if (!(cred instanceof InternalUserPassword)) {
            throw new IllegalArgumentException("invalid credentials");
        }
        InternalUserPassword credentials = (InternalUserPassword) cred;

        if (!username.equals(credentials.getUsername())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check for confirmed
        if (config.isRequireAccountConfirmation() && !account.isConfirmed()) {
            throw new IllegalArgumentException("account-unconfirmed");
        }

        // check if userId matches account
        if (!account.getUserId().equals(credentials.getUserId())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String password = credentials.getPassword();
        validatePassword(password);

        boolean changeOnFirstAccess = credentials.getChangeOnFirstAccess() != null
                ? credentials.getChangeOnFirstAccess().booleanValue()
                : false;

        // set new password as active
        InternalUserPassword pass = passwordService.setPassword(repositoryId, username, password, changeOnFirstAccess,
                config.getPasswordMaxDays(), config.getPasswordKeepNumber());

        // password are encrypted, can't read
        // we return a placeholder to describe status
        pass.eraseCredentials();
        return pass;

    }

    @Override
    public void resetCredentials(String username) throws NoSuchUserException {
        if (!config.isEnablePasswordReset()) {
            throw new IllegalArgumentException("reset is disabled for this provider");
        }

        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check for confirmed
        if (config.isRequireAccountConfirmation() && !account.isConfirmed()) {
            throw new IllegalArgumentException("account-unconfirmed");
        }

        // reset credentials by generating the key
        InternalUserPassword pass = passwordService.resetPassword(repositoryId, username,
                config.getPasswordResetValidity());

        // send mail
        try {
            sendResetMail(account, pass.getResetKey());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    @Override
    public void revokeCredentials(String username) throws NoSuchUserException, NoSuchCredentialException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // fetch current active
        InternalUserPassword password = passwordService.getPassword(repositoryId, username);
        if (password == null) {
            throw new NoSuchUserException();
        }

        // check for confirmed
        if (config.isRequireAccountConfirmation() && !account.isConfirmed()) {
            throw new IllegalArgumentException("account-unconfirmed");
        }

        // revoke this
        passwordService.revokePassword(repositoryId, username, password.getPassword());
    }

    @Override
    public void deleteCredentials(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // delete all passwords
        passwordService.deletePassword(repositoryId, username);
    }

    /*
     * Action urls
     */
    @Override
    public String getResetUrl() {
        // return link for resetting credentials
        return InternalPasswordIdentityAuthority.AUTHORITY_URL + "reset/" + getProvider();
    }

    @Override
    public String getSetUrl() throws NoSuchUserException {
        // internal controller route
        return "/changepwd";
    }
    /*
     * Manage
     */

    public String generatePassword() {
        return RandomStringUtils.random(config.getPasswordMaxLength(), true,
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

        if (config.isPasswordRequireUppercaseAlpha()) {
            if (!password.chars().anyMatch(c -> Character.isUpperCase(c))) {
                throw new InvalidPasswordException("require-capital-alpha");
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

    private void sendResetMail(InternalUserAccount account, String key) throws MessagingException {
        if (mailService != null) {
            // action is handled by global filter
            String provider = getProvider();
            String resetUrl = InternalPasswordIdentityAuthority.AUTHORITY_URL + "doreset/" + provider + "?code=" + key;
            if (uriBuilder != null) {
                resetUrl = uriBuilder.buildUrl(null, resetUrl);
            }

            Map<String, String> action = new HashMap<>();
            action.put("url", resetUrl);
            action.put("text", "action.reset");

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("action", action);
            vars.put("realm", account.getRealm());

            String template = "reset";
            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
        }
    }

    /*
     * Multiple credentials are not supported
     */

    @Override
    public Collection<InternalUserPassword> listCredentials(String accountId) throws NoSuchUserException {
        return Collections.emptyList();
    }

    @Override
    public InternalUserPassword getCredentials(String accountId, String credentialsId) throws NoSuchUserException {
        return null;
    }

    @Override
    public InternalUserPassword setCredentials(String accountId, String credentialsId, UserCredentials credentials)
            throws NoSuchUserException {
        return null;
    }

    @Override
    public void resetCredentials(String accountId, String credentialsId) throws NoSuchUserException {
    }

    @Override
    public void revokeCredentials(String accountId, String credentialsId)
            throws NoSuchUserException {
    }

    @Override
    public void deleteCredentials(String accountId, String credentialsId)
            throws NoSuchUserException {
    }

}
