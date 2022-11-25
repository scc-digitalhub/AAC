package it.smartcommunitylab.aac.password.provider;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.core.provider.AccountCredentialsService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.service.InternalPasswordUserCredentialsService;
import it.smartcommunitylab.aac.utils.MailService;

public class PasswordCredentialsService extends
        AbstractConfigurableProvider<InternalUserPassword, ConfigurableCredentialsProvider, PasswordIdentityProviderConfigMap, PasswordCredentialsServiceConfig>
        implements
        AccountCredentialsService<InternalUserPassword, PasswordIdentityProviderConfigMap, PasswordCredentialsServiceConfig> {
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    private static final String STATUS_INACTIVE = CredentialsStatus.INACTIVE.getValue();
    private static final String STATUS_REVOKED = CredentialsStatus.REVOKED.getValue();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final UserAccountService<InternalUserAccount> accountService;
    private final InternalPasswordUserCredentialsService passwordService;
    private ResourceEntityService resourceService;

    // provider configuration
    private final PasswordCredentialsServiceConfig config;
    private final String repositoryId;

    private PasswordHash hasher;
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public PasswordCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> userAccountService,
            InternalPasswordUserCredentialsService passwordService,
            PasswordCredentialsServiceConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, realm, providerConfig);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.config = providerConfig;
        this.repositoryId = config.getRepositoryId();
        logger.debug("create password credentials service with id {} repository {}", String.valueOf(providerId),
                repositoryId);

        this.accountService = userAccountService;
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

    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
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
                    passwordService.deleteAllCredentials(repositoryId, uuids);
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
        InternalUserPassword newPassword = addPassword(account, password, changeOnFirstAccess);

        // password are encrypted, but clear value for extra safety
        newPassword.eraseCredentials();

        return newPassword;

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
    public Collection<InternalUserPassword> listCredentials(String accountId) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        logger.debug("list credentials for account {}", String.valueOf(accountId));

        // fetch all passwords
        return passwordService.findCredentialsByAccount(repositoryId, accountId).stream()
                .map(p -> {
                    // password are encrypted, but clear value for extra safety
                    p.eraseCredentials();
                    return p;
                }).collect(Collectors.toList());
    }

    @Override
    public InternalUserPassword addCredentials(String accountId, UserCredentials uc) throws NoSuchUserException {
        if (uc == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(InternalUserPassword.class, uc,
                "registration must be an instance of internal user password");
        InternalUserPassword reg = (InternalUserPassword) uc;

        logger.debug("add credential for account {}", String.valueOf(accountId));
        if (logger.isTraceEnabled()) {
            logger.trace("credentials: {}", String.valueOf(reg));
        }

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // skip validation of password against policy
        // we only make sure password is usable
        String password = reg.getPassword();
        if (!StringUtils.hasText(password) || password.length() < config.getPasswordMinLength()
                || password.length() > config.getPasswordMaxLength()) {
            throw new RegistrationException("invalid password");
        }

        InternalUserPassword pass = addPassword(account, password, reg.getChangeOnFirstAccess());

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    @Override
    public InternalUserPassword getCredentials(String accountId, String credentialsId)
            throws NoSuchUserException, NoSuchCredentialException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        logger.debug("get credential {} for account {}", String.valueOf(credentialsId), String.valueOf(accountId));

        InternalUserPassword pass = passwordService.findCredentialsById(repositoryId, credentialsId);
        if (pass == null) {
            throw new NoSuchCredentialException();
        }

        if (!accountId.equals(pass.getAccountId())) {
            // credential is associated to a different account
            throw new NoSuchCredentialException();
        }

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    @Override
    public InternalUserPassword setCredentials(String accountId, String credentialsId, UserCredentials credentials)
            throws NoSuchUserException, RegistrationException, NoSuchCredentialException {
        // set on current password is not allowed
        throw new UnsupportedOperationException();
    }

    @Override
    public InternalUserPassword revokeCredentials(String accountId, String credentialsId)
            throws NoSuchUserException, NoSuchCredentialException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        logger.debug("revoke credential {} for account {}", String.valueOf(credentialsId), String.valueOf(accountId));

        InternalUserPassword pass = passwordService.findCredentialsById(repositoryId, credentialsId);
        if (pass == null) {
            throw new NoSuchCredentialException();
        }

        if (!accountId.equals(pass.getAccountId())) {
            // credential is associated to a different account
            throw new NoSuchCredentialException();
        }

        // we can transition from any status to revoked
        if (!STATUS_REVOKED.equals(pass.getStatus())) {
            // update status
            pass.setStatus(STATUS_REVOKED);
            pass = passwordService.updateCredentials(repositoryId, credentialsId, pass);
        }

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    @Override
    public void deleteCredentials(String accountId, String credentialsId)
            throws NoSuchUserException, NoSuchCredentialException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        logger.debug("delete credential {} for account {}", String.valueOf(credentialsId), String.valueOf(accountId));

        InternalUserPassword pass = passwordService.findCredentialsById(repositoryId, credentialsId);
        if (pass == null) {
            throw new NoSuchCredentialException();
        }

        if (!accountId.equals(pass.getAccountId())) {
            // credential is associated to a different account
            throw new NoSuchCredentialException();
        }

        // delete
        passwordService.deleteCredentials(repositoryId, credentialsId);

        if (resourceService != null) {
            // delete resource
            resourceService.deleteResourceEntity(pass.getUuid());
        }
    }

    @Override
    public void deleteCredentials(String accountId) {
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            return;
        }

        logger.debug("delete all credentials for account {}", String.valueOf(accountId));

        // fetch all to collect ids
        List<InternalUserPassword> passwords = passwordService.findCredentialsByAccount(repositoryId, accountId);

        // delete in batch
        Set<String> ids = passwords.stream().map(p -> p.getId()).collect(Collectors.toSet());
        passwordService.deleteAllCredentials(repositoryId, ids);

        if (resourceService != null) {
            // remove resources
            try {
                // delete in batch
                Set<String> uuids = passwords.stream().map(p -> p.getUuid()).collect(Collectors.toSet());
                passwordService.deleteAllCredentials(repositoryId, uuids);
            } catch (RuntimeException re) {
                logger.error("error removing resources: {}", re.getMessage());
            }
        }
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

    private InternalUserPassword addPassword(InternalUserAccount account, String password,
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
            InternalUserPassword newPassword = new InternalUserPassword();
            newPassword.setId(UUID.randomUUID().toString());
            newPassword.setProvider(repositoryId);

            newPassword.setUsername(account.getUsername());
            newPassword.setUserId(account.getUserId());
            newPassword.setRealm(account.getRealm());

            newPassword.setPassword(hash);
            newPassword.setChangeOnFirstAccess(changeOnFirstAccess);
            newPassword.setExpirationDate(expirationDate);

            newPassword = passwordService.addCredentials(repositoryId, newPassword.getId(), newPassword);

            if (resourceService != null) {
                // register
                resourceService.addResourceEntity(newPassword.getUuid(), SystemKeys.RESOURCE_CREDENTIALS,
                        getAuthority(), getProvider(), newPassword.getResourceId());
            }

            return newPassword;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }
    }

    private String generatePassword() {
        return RandomStringUtils.random(config.getPasswordMaxLength(), true,
                config.isPasswordRequireNumber());
    }

}
