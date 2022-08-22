package it.smartcommunitylab.aac.password.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.oauth.common.SecureStringKeyGenerator;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.utils.MailService;

@Transactional
public class InternalPasswordService extends AbstractProvider
        implements UserCredentialsService<InternalUserPassword> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<InternalUserAccount> accountService;

    // TODO replace with service
    private final InternalUserPasswordRepository passwordRepository;

    // provider configuration
    private final InternalPasswordIdentityProviderConfig config;
    private final String repositoryId;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private PasswordHash hasher;
    private StringKeyGenerator keyGenerator;

    public InternalPasswordService(String providerId,
            UserAccountService<InternalUserAccount> userAccountService,
            InternalUserPasswordRepository passwordRepository,
            InternalPasswordIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(passwordRepository, "password repository is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        this.accountService = userAccountService;
        this.passwordRepository = passwordRepository;
        this.config = providerConfig;
        this.repositoryId = config.getRepositoryId();

        this.hasher = new PasswordHash();

        // use a secure string generator for keys of length 20
        keyGenerator = new SecureStringKeyGenerator(20);
    }

    public void setKeyGenerator(StringKeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
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

    /*
     * Password handling
     */
    @Transactional(readOnly = true)
    public InternalUserPassword findPassword(String username) {
        // fetch active password
        InternalUserPassword password = passwordRepository.findByProviderAndUsernameAndStatusOrderByCreateDateDesc(
                repositoryId, username,
                CredentialsStatus.ACTIVE.getValue());
        if (password != null) {
            // password are encrypted, return as is
            password = passwordRepository.detach(password);
        }

        return password;
    }

    @Transactional(readOnly = true)
    public InternalUserPassword getPassword(String username) throws NoSuchCredentialException {
        // fetch active password
        InternalUserPassword password = passwordRepository.findByProviderAndUsernameAndStatusOrderByCreateDateDesc(
                repositoryId, username,
                CredentialsStatus.ACTIVE.getValue());
        if (password == null) {
            throw new NoSuchCredentialException();
        }

        // password are encrypted, return as is
        password = passwordRepository.detach(password);
        return password;
    }

    public InternalUserPassword setPassword(String username, String password, boolean changeOnFirstAccess)
            throws NoSuchUserException, RegistrationException {
        Date expirationDate = null;
        // expiration date
        if (config.getPasswordMaxDays() > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, config.getPasswordMaxDays());
            expirationDate = cal.getTime();
        }

        return setPassword(username, password, changeOnFirstAccess, expirationDate);
    }

    public InternalUserPassword setPassword(String username, String password, boolean changeOnFirstAccess,
            Date expirationDate)
            throws NoSuchUserException, RegistrationException {
        // fetch active password
        try {
            // encode password
            String hash = hasher.createHash(password);

            // TODO add locking for atomic operation

            // invalidate all old active/inactive passwords up to keep number, delete others
            // note: we keep revoked passwords in DB
            List<InternalUserPassword> oldPasswords = passwordRepository
                    .findByProviderAndUsernameOrderByCreateDateDesc(repositoryId, username).stream()
                    .collect(Collectors.toList());

            // validate new password is NEW
            // TODO move to proper policy service when implemented
            boolean isReuse = oldPasswords.stream().anyMatch(p -> {
                try {
                    return hasher.validatePassword(password, p.getPassword());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    return false;
                }
            });

            if (isReuse) {
                throw new InvalidPasswordException("password-reuse");
            }

            List<InternalUserPassword> toUpdate = oldPasswords
                    .stream()
                    .filter(p -> !STATUS_REVOKED.equals(p.getStatus()))
                    .limit(config.getPasswordKeepNumber())
                    .collect(Collectors.toList());

            List<InternalUserPassword> toDelete = oldPasswords.stream()
                    .filter(p -> !STATUS_REVOKED.equals(p.getStatus()))
                    .filter(p -> !toUpdate.contains(p))
                    .collect(Collectors.toList());
            if (!toDelete.isEmpty()) {
                passwordRepository.deleteAllInBatch(toDelete);
            }

            if (!toUpdate.isEmpty()) {
                toUpdate.forEach(p -> p.setStatus(STATUS_INACTIVE));
                passwordRepository.saveAllAndFlush(toUpdate);
            }

            // create password already hashed
            InternalUserPassword newPassword = new InternalUserPassword();
            newPassword.setId(UUID.randomUUID().toString());
            newPassword.setProvider(repositoryId);
            newPassword.setUsername(username);
            newPassword.setPassword(hash);
            newPassword.setStatus(STATUS_ACTIVE);
            newPassword.setChangeOnFirstAccess(changeOnFirstAccess);
            newPassword.setExpirationDate(expirationDate);

            newPassword = passwordRepository.saveAndFlush(newPassword);

            // password are encrypted, return as is
            newPassword = passwordRepository.detach(newPassword);
            return newPassword;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }

    }

    public boolean verifyPassword(String username, String password) throws NoSuchUserException {
        // fetch active password
        InternalUserPassword pass = passwordRepository.findByProviderAndUsernameAndStatusOrderByCreateDateDesc(
                repositoryId,
                username, STATUS_ACTIVE);
        if (pass == null) {
            return false;
        }

        try {
            // verify expiration
            if (pass.isExpired()) {
                // set expired
                pass.setStatus(STATUS_EXPIRED);
                pass = passwordRepository.saveAndFlush(pass);

                return false;
            }

            // verify match
            return hasher.validatePassword(password, pass.getPassword());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }

    }

    public void deletePassword(String username)
            throws NoSuchUserException {
        // TODO add locking for atomic operation

        // delete all passwords
        List<InternalUserPassword> toDelete = passwordRepository
                .findByProviderAndUsernameOrderByCreateDateDesc(repositoryId, username);
        passwordRepository.deleteAllInBatch(toDelete);
    }

    public InternalUserPassword revokePassword(String username, String password) throws NoSuchCredentialException {

        try {
            // fetch matching password
            // encode password
            String hash = hasher.createHash(password);

            InternalUserPassword pass = passwordRepository.findByProviderAndUsernameAndPassword(repositoryId, username,
                    hash);
            if (pass == null) {
                throw new NoSuchCredentialException();
            }

            // we can transition from any status to revoked
            if (!STATUS_REVOKED.equals(pass.getStatus())) {
                // update status
                pass.setStatus(STATUS_REVOKED);
                pass = passwordRepository.saveAndFlush(pass);
            }

            pass = passwordRepository.detach(pass);
            return pass;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }
    }

    public InternalUserPassword resetPassword(String username) throws NoSuchUserException {
        if (!config.isEnablePasswordReset()) {
            throw new IllegalArgumentException("reset is disabled for this provider");
        }

        // fetch last active password
        InternalUserPassword password = passwordRepository.findByProviderAndUsernameAndStatusOrderByCreateDateDesc(
                repositoryId, username, STATUS_ACTIVE);
        if (password == null) {
            // generate and set active a temporary password
            password = setPassword(username, generatePassword(), true);
        }

        // generate and set a reset key
        String resetKey = keyGenerator.generateKey();

        // we set deadline as +N seconds
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, config.getPasswordResetValidity());

        password.setResetDeadline(calendar.getTime());
        password.setResetKey(resetKey);

        password = passwordRepository.saveAndFlush(password);

        // password are encrypted, return as is
        password = passwordRepository.detach(password);
        return password;
    }

    public InternalUserPassword confirmReset(String resetKey) throws NoSuchCredentialException {
        if (!StringUtils.hasText(resetKey)) {
            throw new IllegalArgumentException("empty-key");
        }

        InternalUserPassword password = passwordRepository.findByProviderAndResetKey(repositoryId, resetKey);
        if (password == null) {
            throw new NoSuchCredentialException();
        }

        // validate key, we do it simple
        boolean isValid = false;

        // password must be active, can't reset inactive
        boolean isActive = STATUS_ACTIVE.equals(password.getStatus());
        if (!isActive) {
            logger.error("invalid key, inactive");
            throw new InvalidDataException("key");
        }

        // validate key match
        // useless check since we fetch account with key as input..
        boolean isMatch = resetKey.equals(password.getResetKey());

        if (!isMatch) {
            logger.error("invalid key, not matching");
            throw new InvalidDataException("key");
        }

        // validate deadline
        Calendar calendar = Calendar.getInstance();
        if (password.getResetDeadline() == null) {
            logger.error("corrupt or used key, missing deadline");
            // do not leak reason
            throw new InvalidDataException("key");
        }

        boolean isExpired = calendar.after(password.getResetDeadline());

        if (isExpired) {
            logger.error("expired key on " + String.valueOf(password.getResetDeadline()));
            // do not leak reason
            throw new InvalidDataException("key");
        }

        isValid = isActive && isMatch && !isExpired;

        if (!isValid) {
            throw new InvalidDataException("key");
        }

        // we clear keys and reset password to lock login
        password.setResetDeadline(null);
        password.setResetKey(null);

        // users need to change the password during this session or reset again
        // we want to lock login with old password from now on
        password.setStatus(STATUS_INACTIVE);

        password = passwordRepository.saveAndFlush(password);

        // password are encrypted, return as is
        password = passwordRepository.detach(password);
        return password;
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

        InternalUserPassword pass = findPassword(username);
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
        InternalUserPassword pass = setPassword(username, password, changeOnFirstAccess);

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

        // reset credentials by generating the key
        InternalUserPassword pass = resetPassword(username);

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
        InternalUserPassword password = getPassword(username);
        if (password == null) {
            throw new NoSuchUserException();
        }

        // revoke this
        revokePassword(username, password.getPassword());
    }

    @Override
    public void deleteCredentials(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // delete all passwords
        deletePassword(username);
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

    @Override
    public AbstractProviderConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    /*
     * Status codes
     */
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    private static final String STATUS_INACTIVE = CredentialsStatus.INACTIVE.getValue();
    private static final String STATUS_REVOKED = CredentialsStatus.REVOKED.getValue();
    private static final String STATUS_EXPIRED = CredentialsStatus.EXPIRED.getValue();

}