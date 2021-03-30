package it.smartcommunitylab.aac.internal;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidInputException;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserService;
import it.smartcommunitylab.aac.model.SpaceRole;

@Service
public class InternalUserManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${authorities.internal.confirmation.required}")
    private boolean confirmationRequired;

    @Value("${authorities.internal.confirmation.validity}")
    private int confirmationValidity;

    @Value("${authorities.internal.password.reset.enabled}")
    private boolean passwordResetEnabled;

    @Value("${authorities.internal.password.reset.validity}")
    private int passwordResetValidity;

    @Value("${authorities.internal.password.minLength}")
    private int passwordMinLength;
    @Value("${authorities.internal.password.maxLength}")
    private int passwordMaxLength;
    @Value("${authorities.internal.password.requireAlpha}")
    private boolean passwordRequireAlpha;
    @Value("${authorities.internal.password.requireNumber}")
    private boolean passwordRequireNumber;
    @Value("${authorities.internal.password.requireSpecial}")
    private boolean passwordRequireSpecial;
    @Value("${authorities.internal.password.supportWhitespace}")
    private boolean passwordSupportWhitespace;

    @Autowired
    private InternalUserService accountService;

    @Autowired
    private UserEntityService userService;

//    @Autowired
//    private AttributeEntityService attributeService;

//    @Autowired
//    private RoleService roleService;

    /*
     * User store init
     */

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.roles}")
    private String[] adminRoles;

    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void init() throws Exception {
        // create admin as superuser
        logger.debug("create internal admin user " + adminUsername);
        UserEntity user = null;
        InternalUserAccount account = accountService.findAccount(SystemKeys.REALM_GLOBAL, adminUsername);
        if (account != null) {
            // check if user exists, recreate if needed
            user = userService.findUser(account.getSubject());
            if (user == null) {
                user = userService.addUser(userService.createUser(SystemKeys.REALM_GLOBAL).getUuid(),
                        SystemKeys.REALM_GLOBAL, adminUsername);
            }
        } else {
            // register as new
            user = userService.addUser(userService.createUser(SystemKeys.REALM_GLOBAL).getUuid(),
                    SystemKeys.REALM_GLOBAL, adminUsername);
            account = accountService.addAccount(user.getUuid(), SystemKeys.REALM_GLOBAL, adminUsername, null, null,
                    null, null);
        }

        String subjectId = account.getSubject();

        // re-set password
        setPassword(subjectId, SystemKeys.REALM_GLOBAL, adminUsername, adminPassword, false);

        // ensure account is unlocked
        this.approveConfirmation(subjectId, SystemKeys.REALM_GLOBAL, adminUsername);

        // assign authorities as roles
        // at minimum we set ADMIN+DEV
        Set<Map.Entry<String, String>> roles = new HashSet<>();
        roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, Config.R_ADMIN));
        roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, Config.R_DEVELOPER));
        for (String role : adminRoles) {
            roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, role));

        }
        List<UserRoleEntity> userRoles = userService.updateRoles(subjectId, roles);

        logger.debug("admin user id " + String.valueOf(account.getId()));
        logger.debug("admin user " + user.toString());
        logger.debug("admin user roles " + userRoles.toString());
        logger.debug("admin account " + account.toString());
    }

    /*
     * Providers
     */
    public String getSubject(String realm, String username) throws NoSuchUserException {
        return accountService.getSubject(realm, username);
    }

    public InternalUserAccount getAccount(String realm, String username) throws NoSuchUserException {
        return accountService.findAccount(realm, username);
    }

    /*
     * Account handling
     * 
     * TODO evaluate dropping extra attributes, maybe those belong to a 'user'
     * attributeStore since we can't validate
     */
    public InternalUserAccount registerAccount(
            String subject,
            String realm,
            String userId,
            String password,
            String email,
            String name,
            String surname,
            String lang,
            Set<Map.Entry<String, String>> attributesMap) throws RegistrationException {

        // remediate missing username
        if (!StringUtils.hasText(userId)) {
            if (StringUtils.hasText(email)) {
                int idx = email.indexOf('@');
                if (idx > 0) {
                    userId = email.substring(0, idx);
                }
            } else if (StringUtils.hasText(name)) {
                userId = StringUtils.trimAllWhitespace(name);
            }

        }

        // validate
        if (!StringUtils.hasText(userId)) {
            throw new RegistrationException("missing-username");
        }

        if (confirmationRequired && !StringUtils.hasText(email)) {
            throw new RegistrationException("missing-email");

        }

        boolean changeOnFirstAccess = false;
        if (!StringUtils.hasText(password)) {
            password = RandomStringUtils.random(passwordMinLength, true, passwordRequireNumber);
            changeOnFirstAccess = true;
        } else {
            validatePassword(password);
        }

        InternalUserAccount account = accountService.findAccount(realm, userId);
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        // check subject, if missing generate
        UserEntity user = null;
        if (StringUtils.hasText(subject)) {
            user = userService.findUser(subject);
        }

        // TODO resolve subject via attributes

        if (user == null) {
            subject = userService.createUser(realm).getUuid();
            user = userService.addUser(subject, realm, userId);
        }

        // add internal account
        account = accountService.addAccount(
                subject,
                realm, userId,
                email, name, surname, lang);

        try {

            // set password
            if (changeOnFirstAccess) {
                // we should send password via mail
                // TODO
            }

            setPassword(subject, realm, userId, password, changeOnFirstAccess);

            // TODO move to caller
//            // check confirmation
//            if (confirmationRequired) {
//                // generate confirmation keys and send mail
//                resetConfirmation(subject, realm, userId, true);
//            } else {
//                // auto approve
//                approveConfirmation(subject, realm, userId);
//            }

            // TODO send registration event

        } catch (NoSuchUserException e) {
            // something very wrong happend
            logger.error("no such user during updates " + e.getMessage());
            throw new SystemException();
        }

        // persist attributes
        List<AttributeEntity> attributes = Collections.emptyList();

//        if (attributesMap != null) {
//            // note: internal authority has a single provider, internal
//            attributes = attributeService.setAttributes(subject, SystemKeys.AUTHORITY_INTERNAL,
//                    SystemKeys.AUTHORITY_INTERNAL, account.getId(),
//                    attributesMap);
//        }
//
//        return InternalUserIdentity.from(account, attributes);

        // note entity is still attached
        // TODO evaluate detach
        return account;

    }

    public InternalUserAccount updateAccount(
            String subject,
            String realm,
            String userId,
            String email,
            String name,
            String surname,
            String lang,
            Set<Map.Entry<String, String>> attributesMap) throws NoSuchUserException {

        InternalUserAccount account = accountService.getAccount(realm, userId);
        account.setSubject(subject);
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);

        account = accountService.updateAccount(account);

//        // persist attributes
//        List<AttributeEntity> attributes = Collections.emptyList();
//
//        if (attributesMap != null) {
//            // note: internal authority has a single provider, internal
//            attributes = attributeService.setAttributes(subject, SystemKeys.AUTHORITY_INTERNAL,
//                    SystemKeys.AUTHORITY_INTERNAL, account.getId(),
//                    attributesMap);
//        }
//
        // note entity is still attached
        // TODO evaluate detach
        return account;
    }

    public void deleteAccount(
            String subject,
            String realm,
            String userId) throws NoSuchUserException {

        accountService.deleteAccount(subject, realm, userId);

    }

    /*
     * Password
     */

    public void validatePassword(String password) throws InvalidPasswordException {

        if (!StringUtils.hasText(password)) {
            throw new InvalidPasswordException("empty");
        }

        if (password.length() < passwordMinLength) {
            throw new InvalidPasswordException("min-length");
        }

        if (password.length() > passwordMaxLength) {
            throw new InvalidPasswordException("max-length");
        }

        if (passwordRequireAlpha) {
            if (!password.chars().anyMatch(c -> Character.isLetter(c))) {
                throw new InvalidPasswordException("require-alpha");
            }
        }

        if (passwordRequireNumber) {
            if (!password.chars().anyMatch(c -> Character.isDigit(c))) {
                throw new InvalidPasswordException("require-number");
            }
        }

        if (passwordRequireSpecial) {
            // we do not count whitespace as special char
            if (!password.chars().anyMatch(c -> (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)))) {
                throw new InvalidPasswordException("require-special");
            }
        }

        if (!passwordSupportWhitespace) {
            if (password.chars().anyMatch(c -> Character.isWhitespace(c))) {
                throw new InvalidPasswordException("contains-whitespace");
            }
        }

    }

    public void updatePassword(
            String subject,
            String realm,
            String userId,
            String password) throws NoSuchUserException, InvalidPasswordException {

        // validate password
        validatePassword(password);

        setPassword(subject, realm, userId, password, false);
    }

    public void setPassword(
            String subject,
            String realm,
            String userId,
            String password,
            boolean changeOnFirstAccess) throws NoSuchUserException {

        try {
            // encode password
            String hash = PasswordHash.createHash(password);

            InternalUserAccount account = accountService.updatePassword(subject, realm, userId, hash,
                    changeOnFirstAccess);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }

    }

    public void resetPassword(
            String subject,
            String realm,
            String userId,
            boolean sendMail) throws NoSuchUserException {

        InternalUserAccount account = accountService.getAccount(realm, userId);

        try {

            // generate a reset key
            String resetKey = generateKey();

            account.setResetKey(resetKey);

            // we set deadline as +N seconds
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, passwordResetValidity);
            account.setResetDeadline(calendar.getTime());
            account = accountService.updateAccount(account);

            // TODO send mail

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }
    }

    public InternalUserAccount doPasswordReset(
            String resetKey) {

        if (!StringUtils.hasText(resetKey)) {
            throw new InvalidInputException("empty-key");
        }

        logger.debug("do reset password key " + resetKey);
        try {
            InternalUserAccount account = accountService.getAccountByResetKey(resetKey);
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
                logger.error("corrupt or used, key missing deadline");
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

            if (isValid) {
                // we reset the key, single use
                account.setResetDeadline(null);
                account.setResetKey(null);
                account = accountService.updateAccount(account);
            }

            // note entity is still attached
            // TODO evaluate detach
            return account;

        } catch (NoSuchUserException ne) {
            logger.error("invalid key, not found in db");
            throw new InvalidInputException("invalid-key");
        }
    }

    /*
     * Confirmation
     */
    public InternalUserAccount doConfirmation(
            String confirmationKey) {

        if (!StringUtils.hasText(confirmationKey)) {
            throw new InvalidInputException("empty-key");
        }

        logger.debug("do confirm key " + confirmationKey);
        try {
            InternalUserAccount account = accountService.getAccountByConfirmationKey(confirmationKey);
            // validate key, we do it simple
            boolean isValid = false;

            // validate key match
            // useless check since we fetch account with key as input..
            boolean isMatch = confirmationKey.equals(account.getConfirmationKey());

            if (!isMatch) {
                logger.error("invalid key, not matching");
                throw new InvalidInputException("invalid-key");
            }

            // validate deadline
            Calendar calendar = Calendar.getInstance();
            if (account.getConfirmationDeadline() == null) {
                logger.error("corrupt or used key, missing deadline");
                // do not leak reason
                throw new InvalidInputException("invalid-key");
            }

            boolean isExpired = calendar.after(account.getConfirmationDeadline());

            if (isExpired) {
                logger.error("expired key on " + String.valueOf(account.getConfirmationDeadline()));
                // do not leak reason
                throw new InvalidInputException("invalid-key");
            }

            isValid = isMatch && !isExpired;

            if (isValid) {
                // we set confirm and reset the key, single use
                account.setConfirmed(true);
                account.setConfirmationDeadline(null);
                account.setConfirmationKey(null);
                account = accountService.updateAccount(account);
            }

            // note entity is still attached
            // TODO evaluate detach
            return account;
        } catch (NoSuchUserException ne) {
            logger.error("invalid key, not found in db");
            throw new InvalidInputException("invalid-key");
        }
    }

    public void updateConfirmation(
            String subject,
            String realm,
            String userId,
            boolean confirmed,
            Date confirmationDeadline,
            String confirmationKey) throws NoSuchUserException {

        InternalUserAccount account = accountService.getAccount(realm, userId);
        account.setConfirmed(confirmed);
        account.setConfirmationDeadline(confirmationDeadline);
        account.setConfirmationKey(confirmationKey);

        account = accountService.updateAccount(account);

    }

    public void approveConfirmation(
            String subject,
            String realm,
            String userId) throws NoSuchUserException {

        InternalUserAccount account = accountService.getAccount(realm, userId);
        account.setConfirmed(true);

        account = accountService.updateAccount(account);
    }

    public void resetConfirmation(
            String subject,
            String realm,
            String userId,
            boolean sendMail) throws NoSuchUserException {

        InternalUserAccount account = accountService.getAccount(realm, userId);

        try {
            // set status to false
            account.setConfirmed(false);

            // generate a solid key
            String confirmationKey = generateKey();

            account.setConfirmationKey(confirmationKey);

            // we set deadline as +N seconds
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, confirmationValidity);
            account.setConfirmationDeadline(calendar.getTime());
            account = accountService.updateAccount(account);

            // TODO send mail

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }
    }

    /*
     * TODO Mail
     */

    /*
     * Keys
     */

    private static String generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String rnd = UUID.randomUUID().toString();
        return rnd;
    }

}
