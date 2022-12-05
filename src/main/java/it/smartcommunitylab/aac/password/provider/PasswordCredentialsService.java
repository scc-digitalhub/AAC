package it.smartcommunitylab.aac.password.provider;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsService;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.PasswordCredentialsAuthority;
import it.smartcommunitylab.aac.password.dto.InternalEditableUserPassword;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.service.InternalPasswordUserCredentialsService;
import it.smartcommunitylab.aac.utils.MailService;

public class PasswordCredentialsService extends
        AbstractCredentialsService<InternalUserPassword, InternalEditableUserPassword, InternalUserAccount, PasswordIdentityProviderConfigMap, PasswordCredentialsServiceConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final InternalPasswordUserCredentialsService passwordService;
    private PasswordHash hasher;
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public PasswordCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> userAccountService,
            InternalPasswordUserCredentialsService passwordService,
            PasswordCredentialsServiceConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, userAccountService, passwordService, providerConfig, realm);
        Assert.notNull(passwordService, "password service is mandatory");

        this.passwordService = passwordService;
        this.hasher = new PasswordHash();
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    public void setHasher(PasswordHash hasher) {
        Assert.notNull(hasher, "password hasher can not be null");
        this.hasher = hasher;
    }

    /*
     * Password management
     * for (internal) password API
     */

    public boolean verifyPassword(String username, String password) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("verify password for account {}", String.valueOf(username));
        }

        // fetch ALL active + non expired credentials
        List<InternalUserPassword> credentials = passwordService
                .findCredentialsByAccount(repositoryId, username).stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()) && !c.isExpired())
                .collect(Collectors.toList());

        // pick any match on hashed password
        return credentials.stream()
                .anyMatch(c -> {
                    try {
                        return hasher.validatePassword(password, c.getPassword());
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        throw new SystemException(e.getMessage());
                    }
                });
    }

    public InternalUserPassword setPassword(String username, String password, boolean changeOnFirstAccess)
            throws NoSuchUserException, RegistrationException {
        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // validate against policy
        validatePassword(password);

        logger.debug("set password for account {}", String.valueOf(username));

        // TODO add locking for atomic operation

        // invalidate all old active/inactive passwords up to keep number, delete others
        // note: we keep revoked passwords in DB
        List<InternalUserPassword> oldPasswords = passwordService.findCredentialsByAccount(repositoryId, username);

        // validate new password is NEW
        // TODO move to proper policy service when implemented
        boolean isReuse = oldPasswords.stream().anyMatch(p -> {
            try {
                return hasher.validatePassword(password, p.getPassword());
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                return false;
            }
        });

        if (isReuse && config.getPasswordKeepNumber() > 0) {
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
            // delete in batch
            Set<String> ids = toDelete.stream().map(p -> p.getId()).collect(Collectors.toSet());
            passwordService.deleteAllCredentials(repositoryId, ids);

            if (resourceService != null) {
                // remove resources
                try {
                    // delete in batch
                    Set<String> uuids = toDelete.stream().map(p -> p.getUuid()).collect(Collectors.toSet());
                    resourceService.deleteAllResourceEntities(uuids);
                } catch (RuntimeException re) {
                    logger.error("error removing resources: {}", re.getMessage());
                }
            }
        }

        if (!toUpdate.isEmpty()) {
            toUpdate.forEach(p -> {
                p.setStatus(STATUS_INACTIVE);
                try {
                    passwordService.updateCredentials(repositoryId, p.getId(), p);
                } catch (RegistrationException | NoSuchCredentialException e) {
                    // ignore
                }
            });
        }

        // create password
        InternalUserPassword newPassword = buildPassword(account, password, changeOnFirstAccess);

        // save as new
        InternalUserPassword pass = super.addCredential(username, null, newPassword);

        // map to ourselves
        pass.setProvider(getProvider());

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;

    }

    public InternalUserPassword resetPassword(String username) throws NoSuchUserException, RegistrationException {
        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        logger.debug("reset password for account {}", String.valueOf(username));

        // generate new single-use password
        String password = generatePassword();

        // set as current, with change required
        InternalUserPassword pass = setPassword(username, password, true);

        // send via mail
        try {
            sendPasswordMail(account, password);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return pass;
    }

    /*
     * Password policy
     */

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
     * Credentials
     * for credentials API
     */
    @Override
    public InternalUserPassword addCredential(String accountId, String credentialId, UserCredentials uc)
            throws NoSuchUserException {
        if (uc == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserPassword.class, uc,
                "registration must be an instance of internal user password");
        InternalUserPassword reg = (InternalUserPassword) uc;

        // skip validation of password against policy
        // we only make sure password is usable
        String password = reg.getPassword();
        if (!StringUtils.hasText(password) || password.length() < config.getPasswordMinLength()
                || password.length() > config.getPasswordMaxLength()) {
            throw new RegistrationException("invalid password");
        }

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // build password
        InternalUserPassword newPassword = buildPassword(account, password, reg.getChangeOnFirstAccess());

        // save
        InternalUserPassword pass = super.addCredential(accountId, credentialId, newPassword);

        // map to ourselves
        pass.setProvider(getProvider());

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    @Override
    public InternalUserPassword setCredential(String accountId, String credentialsId, UserCredentials credentials)
            throws RegistrationException, NoSuchCredentialException {
        // set on current password is not allowed
        throw new UnsupportedOperationException();
    }

    /*
     * Editable
     */
    public InternalEditableUserPassword getEditableCredential(String accountId, String credentialId)
            throws NoSuchCredentialException {
        // get as editable
        InternalUserPassword pass = getCredential(credentialId);

        if (!pass.getAccountId().equals(accountId)) {
            throw new IllegalArgumentException("account-mismatch");
        }

        InternalEditableUserPassword ed = new InternalEditableUserPassword(getProvider(), pass.getUuid());
        ed.setCredentialsId(pass.getCredentialsId());
        ed.setUserId(pass.getUserId());
        ed.setUsername(pass.getUsername());

        return ed;
    }

//
//    public InternalEditableUserPassword registerCredential(String accountId, String credentialId,
//            EditableUserCredentials credentials)
//            throws RegistrationException, NoSuchCredentialException {
//        throw new UnsupportedOperationException();
//    }
//
//    public InternalEditableUserPassword editCredential(String accountId, String credentialId,
//            EditableUserCredentials credentials)
//            throws RegistrationException, NoSuchCredentialException {
//        throw new UnsupportedOperationException();
//    }
    @Override
    public String getRegisterUrl() {
        return null;
    }

    @Override
    public String getEditUrl(String credentialsId) throws NoSuchCredentialException {
        // get as editable for user
//        InternalUserPassword pass = getCredential(credentialsId);

        return PasswordCredentialsAuthority.AUTHORITY_URL + "changepwd/" + getProvider();
    }

    /*
     * Mail
     */
    private void sendPasswordMail(InternalUserAccount account, String password) throws MessagingException {
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

    /*
     * Helpers
     */

    private InternalUserPassword buildPassword(InternalUserAccount account, String password,
            boolean changeOnFirstAccess) {
        Date expirationDate = null;
        // expiration date
        if (config.getPasswordMaxDays() > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, config.getPasswordMaxDays());
            expirationDate = cal.getTime();
        }

        try {
            // encode password
            String hash = hasher.createHash(password);

            // create password already hashed
            InternalUserPassword pass = new InternalUserPassword();
            pass.setRepositoryId(repositoryId);

            pass.setAuthority(getAuthority());
            pass.setProvider(getProvider());

            pass.setUsername(account.getUsername());
            pass.setUserId(account.getUserId());
            pass.setRealm(account.getRealm());

            pass.setPassword(hash);
            pass.setChangeOnFirstAccess(changeOnFirstAccess);
            pass.setExpirationDate(expirationDate);

            return pass;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }
    }

    private String generatePassword() {
        return RandomStringUtils.random(config.getPasswordMaxLength(), true,
                config.isPasswordRequireNumber());
    }

}
