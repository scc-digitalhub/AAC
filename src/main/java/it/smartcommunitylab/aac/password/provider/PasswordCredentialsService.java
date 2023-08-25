/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.credentials.base.AbstractCredentialsService;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.password.PasswordCredentialsAuthority;
import it.smartcommunitylab.aac.password.model.InternalEditableUserPassword;
import it.smartcommunitylab.aac.password.model.InternalUserPassword;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.service.InternalPasswordJpaUserCredentialsService;
import it.smartcommunitylab.aac.utils.MailService;
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
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class PasswordCredentialsService
    extends AbstractCredentialsService<InternalUserPassword, InternalEditableUserPassword, InternalUserAccount, PasswordIdentityProviderConfigMap, PasswordCredentialsServiceConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final InternalPasswordJpaUserCredentialsService passwordService;
    private PasswordHash hasher;
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public PasswordCredentialsService(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordJpaUserCredentialsService passwordService,
        PasswordCredentialsServiceConfig providerConfig,
        String realm
    ) {
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

        // fetch ALL active + non expired credentials from same user
        //NOTE: username in password should be dropped
        String userId = account.getUserId();
        List<InternalUserPassword> credentials = passwordService
            .findCredentialsByUser(repositoryId, userId)
            .stream()
            .filter(c -> STATUS_ACTIVE.equals(c.getStatus()) && !c.isExpired())
            .collect(Collectors.toList());

        // pick any match on hashed password
        return credentials
            .stream()
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
        String userId = account.getUserId();
        List<InternalUserPassword> oldPasswords = passwordService.findCredentialsByUser(repositoryId, userId);

        // validate new password is NEW
        // TODO move to proper policy service when implemented
        boolean isReuse = oldPasswords
            .stream()
            .anyMatch(p -> {
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

        List<InternalUserPassword> toDelete = oldPasswords
            .stream()
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
    public InternalUserPassword addCredential(String userId, String credentialId, UserCredentials uc)
        throws NoSuchUserException, RegistrationException {
        if (uc == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            InternalUserPassword.class,
            uc,
            "registration must be an instance of internal user password"
        );
        InternalUserPassword reg = (InternalUserPassword) uc;

        // skip validation of password against policy
        // we only make sure password is usable
        String password = reg.getPassword();
        if (
            !StringUtils.hasText(password) ||
            password.length() < config.getPasswordMinLength() ||
            password.length() > config.getPasswordMaxLength()
        ) {
            throw new RegistrationException("invalid password");
        }

        // // fetch user
        // InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        // if (account == null) {
        //     throw new NoSuchUserException();
        // }

        // build password
        InternalUserPassword newPassword = buildPassword(account, password, reg.isChangeOnFirstAccess());

        // save
        InternalUserPassword pass = super.addCredential(accountId, credentialId, newPassword);

        // map to ourselves
        pass.setProvider(getProvider());

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    @Override
    public InternalUserPassword setCredential(String credentialId, UserCredentials uc)
        throws RegistrationException, NoSuchCredentialException {
        if (uc == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            InternalUserPassword.class,
            uc,
            "registration must be an instance of internal user password"
        );
        InternalUserPassword reg = (InternalUserPassword) uc;

        // skip validation of password against policy
        // we only make sure password is usable
        String password = reg.getPassword();
        if (
            !StringUtils.hasText(password) ||
            password.length() < config.getPasswordMinLength() ||
            password.length() > config.getPasswordMaxLength()
        ) {
            throw new RegistrationException("invalid password");
        }

        // fetch password
        InternalUserPassword cred = credentialsService.findCredentialsById(repositoryId, credentialId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // fetch user
        String accountId = cred.getUsername();
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchCredentialException();
        }

        // update password
        InternalUserPassword newPassword = buildPassword(account, password, reg.isChangeOnFirstAccess());
        cred.setPassword(newPassword.getPassword());

        // save
        InternalUserPassword pass = super.setCredential(credentialId, cred);

        // map to ourselves
        pass.setProvider(getProvider());

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    /*
     * Editable
     * TODO represent a single logical "ediablePassword" backed by different
     * passwords to expose a single, constant-id "credential" to console
     */

    private InternalEditableUserPassword toEditable(InternalUserPassword pass) {
        InternalEditableUserPassword ed = new InternalEditableUserPassword(getProvider(), pass.getUuid());
        ed.setCredentialsId(pass.getCredentialsId());
        ed.setUserId(pass.getUserId());
        ed.setUsername(pass.getUsername());
        ed.setCreateDate(pass.getCreateDate());
        ed.setModifiedDate(pass.getCreateDate());
        ed.setExpireDate(pass.getExpirationDate());

        // load policy
        PasswordPolicy policy = getPasswordPolicy();
        ed.setPolicy(policy);

        return ed;
    }

    // // @Override
    // public Collection<InternalEditableUserPassword> listEditableCredentials(String accountId) {
    //     // TODO map MANY pass to ONE editable per user
    //     // fetch ALL active
    //     List<InternalUserPassword> credentials = passwordService
    //         .findCredentialsByAccount(repositoryId, accountId)
    //         .stream()
    //         .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
    //         .collect(Collectors.toList());

    //     return credentials.stream().map(c -> toEditable(c)).collect(Collectors.toList());
    // }

    // @Override
    public Collection<InternalEditableUserPassword> listEditableCredentialsByUser(String userId) {
        // TODO map MANY pass to ONE editable per user
        // fetch ALL active
        List<InternalUserPassword> credentials = passwordService
            .findCredentialsByUser(repositoryId, userId)
            .stream()
            .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
            .collect(Collectors.toList());

        return credentials.stream().map(c -> toEditable(c)).collect(Collectors.toList());
    }

    @Override
    public InternalEditableUserPassword getEditableCredential(String credentialId) throws NoSuchCredentialException {
        // get as editable
        // TODO map MANY pass to ONE editable per user
        InternalUserPassword pass = getCredential(credentialId);
        return toEditable(pass);
    }

    @Override
    public InternalEditableUserPassword registerCredential(String username, EditableUserCredentials uc)
        throws RegistrationException, NoSuchUserException {
        if (uc == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            InternalEditableUserPassword.class,
            uc,
            "registration must be an instance of internal user password"
        );
        InternalEditableUserPassword reg = (InternalEditableUserPassword) uc;

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if one password is already set
        // we require NO password for registration, otherwise we should check against
        // current for proper authorization (same as edit)
        String userId = account.getUserId();
        List<InternalUserPassword> list = passwordService.findCredentialsByUser(repositoryId, userId);
        if (!list.isEmpty()) {
            throw new AlreadyRegisteredException();
        }
        // skip validation of password against policy, will be done later
        // we only make sure password is usable
        String password = reg.getPassword();
        if (
            !StringUtils.hasText(password) ||
            password.length() < config.getPasswordMinLength() ||
            password.length() > config.getPasswordMaxLength()
        ) {
            throw new RegistrationException("invalid password");
        }

        // update password via set to keep password history
        InternalUserPassword cred = this.setPassword(account.getUsername(), password, false);

        // TODO map MANY pass to ONE editable per user
        return toEditable(cred);
    }

    // @Override
    public void deleteEditableCredential(@NotNull String credentialId) throws NoSuchCredentialException {
        // TODO map MANY pass to ONE editable per user
        String id = credentialId;
        deleteCredential(id);
    }

    @Override
    public InternalEditableUserPassword editCredential(String credentialId, EditableUserCredentials uc)
        throws RegistrationException, NoSuchCredentialException {
        if (uc == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            InternalEditableUserPassword.class,
            uc,
            "registration must be an instance of internal user password"
        );
        InternalEditableUserPassword reg = (InternalEditableUserPassword) uc;

        // skip validation of password against policy, will be done later
        // we only make sure password is usable
        String password = reg.getPassword();
        if (
            !StringUtils.hasText(password) ||
            password.length() < config.getPasswordMinLength() ||
            password.length() > config.getPasswordMaxLength()
        ) {
            throw new RegistrationException("invalid password");
        }

        // fetch password
        InternalUserPassword cred = credentialsService.findCredentialsById(repositoryId, credentialId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // fetch user
        String accountId = cred.getUsername();
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchCredentialException();
        }

        // only active credentials can be used for edit
        if (!cred.isActive()) {
            throw new NoSuchCredentialException();
        }

        try {
            // validate current password for authorization
            boolean isValid = hasher.validatePassword(reg.getCurPassword(), cred.getPassword());
            if (!isValid) {
                throw new RegistrationException("invalid_password");
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }

        try {
            // update password via set to keep password history
            this.setPassword(account.getUsername(), password, false);

            // TODO map MANY pass to ONE editable per user
            // TODO evaluate how to handle id change (now)
            // for now return same editable blanked
            // note that this will be inactive so NOT editable again
            return toEditable(cred);
        } catch (NoSuchUserException e) {
            throw new NoSuchCredentialException();
        }
    }

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

    private InternalUserPassword buildPassword(
        InternalUserAccount account,
        String password,
        boolean changeOnFirstAccess
    ) {
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
            String id = UUID.randomUUID().toString();
            InternalUserPassword pass = new InternalUserPassword(getRealm(), id);
            pass.setRepositoryId(repositoryId);
            pass.setProvider(getProvider());

            pass.setUsername(account.getUsername());
            pass.setUserId(account.getUserId());

            pass.setPassword(hash);
            pass.setChangeOnFirstAccess(changeOnFirstAccess);
            pass.setExpirationDate(expirationDate);

            return pass;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }
    }

    private String generatePassword() {
        return RandomStringUtils.random(config.getPasswordMaxLength(), true, config.isPasswordRequireNumber());
    }
}
