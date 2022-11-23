package it.smartcommunitylab.aac.password.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.oauth.common.SecureStringKeyGenerator;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;

@Transactional
public class InternalPasswordService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalPasswordUserCredentialsService credentialsService;

    // TODO interface + config for hasher
    private PasswordHash hasher;
    private StringKeyGenerator keyGenerator;

    public InternalPasswordService(InternalPasswordUserCredentialsService credentialsService) {
        Assert.notNull(credentialsService, "password credentials service is mandatory");
        this.credentialsService = credentialsService;

        this.hasher = new PasswordHash();

        // use a secure string generator for keys of length 20
        keyGenerator = new SecureStringKeyGenerator(20);
    }

    public void setKeyGenerator(StringKeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    /*
     * Password handling
     */
    @Transactional(readOnly = true)
    public InternalUserPassword findPasswordById(String repositoryId, String id) {
        return credentialsService.findCredentialsById(repositoryId, id);
    }

    @Transactional(readOnly = true)
    public InternalUserPassword findPassword(String repositoryId, String username) {
        // fetch first active password
        return credentialsService
                .findCredentialsByAccount(repositoryId, username).stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus())).findFirst().orElse(null);
    }

    public InternalUserPassword findByResetKey(String repositoryId, String resetKey) {
        // find via key
        return credentialsService.findCredentialsByResetKey(repositoryId, resetKey);
    }

    @Transactional(readOnly = true)
    public InternalUserPassword getPassword(String repositoryId, String username) throws NoSuchCredentialException {
        // fetch active password
        InternalUserPassword password = findPassword(repositoryId, username);
        if (password == null) {
            throw new NoSuchCredentialException();
        }

        // password are encrypted, return as is
        return password;
    }

    public InternalUserPassword setPassword(String repositoryId, String username, String password,
            boolean changeOnFirstAccess, int expirationDays, int keepNumber)
            throws NoSuchUserException, RegistrationException {
        Date expirationDate = null;
        // expiration date
        if (expirationDays > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, expirationDays);
            expirationDate = cal.getTime();
        }

        return setPassword(repositoryId, username, password, changeOnFirstAccess, expirationDate, keepNumber);
    }

    public InternalUserPassword setPassword(String repositoryId, String username, String password,
            boolean changeOnFirstAccess, Date expirationDate, int keepNumber)
            throws NoSuchUserException, RegistrationException {
        // fetch active password
        try {
            // encode password
            String hash = hasher.createHash(password);

            // TODO add locking for atomic operation

            // invalidate all old active/inactive passwords up to keep number, delete others
            // note: we keep revoked passwords in DB
            List<InternalUserPassword> oldPasswords = passwordRepository
                    .findByRepositoryIdAndUsernameOrderByCreateDateDesc(repositoryId, username).stream()
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

            if (isReuse && keepNumber > 0) {
                throw new InvalidPasswordException("password-reuse");
            }

            List<InternalUserPassword> toUpdate = oldPasswords
                    .stream()
                    .filter(p -> !STATUS_REVOKED.equals(p.getStatus()))
                    .limit(keepNumber)
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

    public boolean verifyPassword(String repositoryId, String username, String password) throws NoSuchUserException {
        // fetch ALL active passwords
        List<InternalUserPassword> credentials = credentialsService
                .findCredentialsByAccount(repositoryId, username).stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()) && !c.isExpired())
                .collect(Collectors.toList());

        // update status on active + expired
        credentials.stream().filter(c -> c.isExpired()).forEach(c -> {
            c.setStatus(STATUS_EXPIRED);
            try {
                credentialsService.updateCredentials(repositoryId, c.getId(), c);
            } catch (RegistrationException | NoSuchCredentialException e) {
                // ignore
            }
        });

        // pick any match on hashed password for non expired credentials
        return credentials.stream()
                .anyMatch(c -> {
                    try {
                        return !c.isExpired() && hasher.validatePassword(password, c.getPassword());
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        throw new SystemException(e.getMessage());
                    }
                });

    }

    public void deletePassword(String repositoryId, String username) {
        // TODO add locking for atomic operation

        // delete all passwords for the given account
        credentialsService.deleteCredentialsByAccount(repositoryId, username);
    }

    public InternalUserPassword revokePassword(String repositoryId, String username, String password)
            throws NoSuchCredentialException {

        try {
            // fetch matching password
            // encode password
            String hash = hasher.createHash(password);

            InternalUserPassword pass = passwordRepository.findByRepositoryIdAndUsernameAndPassword(repositoryId,
                    username,
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

    public InternalUserPassword resetPassword(String repositoryId, String username, int resetValidity)
            throws NoSuchUserException {

        // fetch last active password
        InternalUserPassword password = passwordRepository.findByRepositoryIdAndUsernameAndStatusOrderByCreateDateDesc(
                repositoryId, username, STATUS_ACTIVE);
        if (password == null) {
            // generate and set active a temporary password
            password = setPassword(repositoryId, username, keyGenerator.generateKey(), true, 0, 0);
        }

        // generate and set a reset key
        String resetKey = keyGenerator.generateKey();

        // we set deadline as +N seconds
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, resetValidity);

        password.setResetDeadline(calendar.getTime());
        password.setResetKey(resetKey);

        password = passwordRepository.saveAndFlush(password);

        // password are encrypted, return as is
        password = passwordRepository.detach(password);
        return password;
    }

    public InternalUserPassword confirmReset(String repositoryId, String resetKey) throws NoSuchCredentialException {
        if (!StringUtils.hasText(resetKey)) {
            throw new IllegalArgumentException("empty-key");
        }

        InternalUserPassword password = passwordRepository.findByRepositoryIdAndResetKey(repositoryId, resetKey);
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
     * Status codes
     */
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    private static final String STATUS_INACTIVE = CredentialsStatus.INACTIVE.getValue();
    private static final String STATUS_REVOKED = CredentialsStatus.REVOKED.getValue();
    private static final String STATUS_EXPIRED = CredentialsStatus.EXPIRED.getValue();

}