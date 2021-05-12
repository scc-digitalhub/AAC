package it.smartcommunitylab.aac.internal;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;

@Service
public class InternalUserManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private InternalUserAccountService accountService;

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
    @Transactional
    public void init() throws Exception {
        // create admin as superuser
        logger.debug("create internal admin user " + adminUsername);
        UserEntity user = null;
        InternalUserAccount account = accountService.findAccountByUsername(SystemKeys.REALM_SYSTEM, adminUsername);
        if (account != null) {
            // check if user exists, recreate if needed
            user = userService.findUser(account.getSubject());
            if (user == null) {
                user = userService.addUser(userService.createUser(SystemKeys.REALM_SYSTEM).getUuid(),
                        SystemKeys.REALM_SYSTEM, adminUsername);
            }
        } else {
            // register as new
            user = userService.addUser(userService.createUser(SystemKeys.REALM_SYSTEM).getUuid(),
                    SystemKeys.REALM_SYSTEM, adminUsername);
            account = new InternalUserAccount();
            account.setSubject(user.getUuid());
            account.setRealm(SystemKeys.REALM_SYSTEM);
            account.setUsername(adminUsername);
            account = accountService.addAccount(account);
        }

        String subjectId = account.getSubject();

        // re-set password
        String hash = PasswordHash.createHash(adminPassword);
        account.setPassword(hash);
        account.setChangeOnFirstAccess(false);

        // ensure account is unlocked
        account.setConfirmed(true);
        account.setConfirmationKey(null);
        account.setConfirmationDeadline(null);
        account.setResetKey(null);
        account.setResetDeadline(null);
        account = accountService.updateAccount(account.getId(), account);

        // assign authorities as roles
        // at minimum we set ADMIN+DEV global, and ADMIN+DEV in SYSTEM realm
        Set<Map.Entry<String, String>> roles = new HashSet<>();
        roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, Config.R_ADMIN));
        roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, Config.R_DEVELOPER));
        roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_SYSTEM, Config.R_ADMIN));
        roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_SYSTEM, Config.R_DEVELOPER));
        // admin roles are global,ie they are valid for any realm
        for (String role : adminRoles) {
            roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, role));

        }

        // merge roles
        List<UserRoleEntity> curRoles = userService.getRoles(subjectId);
        for (UserRoleEntity ur : curRoles) {
            roles.add(new AbstractMap.SimpleEntry<>(ur.getRealm(), ur.getRole()));
        }

        // set
        List<UserRoleEntity> userRoles = userService.updateRoles(subjectId, roles);

        logger.debug("admin user id " + String.valueOf(account.getId()));
        logger.debug("admin user " + user.toString());
        logger.debug("admin user roles " + userRoles.toString());
        logger.debug("admin account " + account.toString());
    }

    /*
     * Account handling
     * 
     * TODO evaluate dropping extra attributes, maybe those belong to a 'user'
     * attributeStore since we can't validate
     */
//    public InternalUserAccount registerAccount(
//            String subject,
//            String realm,
//            String username,
//            String password,
//            String email,
//            String name,
//            String surname,
//            String lang,
//            Set<Map.Entry<String, String>> attributesMap) throws RegistrationException {
//
//        // remediate missing username
//        if (!StringUtils.hasText(username)) {
//            username = email;
//        }
//
//        // validate
//        if (!StringUtils.hasText(username)) {
//            throw new RegistrationException("missing-username");
//        }
//
//        if (confirmationRequired && !StringUtils.hasText(email)) {
//            throw new RegistrationException("missing-email");
//
//        }
//        email = email.trim().toLowerCase();
//        username = username.trim().toLowerCase();
//
//        boolean changeOnFirstAccess = false;
//        if (!StringUtils.hasText(password)) {
//            password = RandomStringUtils.random(passwordMinLength, true, passwordRequireNumber);
//            changeOnFirstAccess = true;
//        } else {
//            validatePassword(password);
//        }
//
//        InternalUserAccount account = accountService.findAccount(realm, username);
//        if (account != null) {
//            throw new AlreadyRegisteredException("duplicate-registration");
//        }
//
//        // check subject, if missing generate
//        UserEntity user = null;
//        if (StringUtils.hasText(subject)) {
//            user = userService.findUser(subject);
//        }
//
//        // TODO resolve subject via attributes
//
//        if (user == null) {
//            subject = userService.createUser(realm).getUuid();
//            user = userService.addUser(subject, realm, username);
//        }
//
//        // add internal account
//        account = accountService.addAccount(
//                subject,
//                realm, username,
//                email, name, surname, lang);
//
//        try {
//
//            // set password
//            if (changeOnFirstAccess) {
//                // we should send password via mail
//                // TODO
//            }
//
//            setPassword(subject, realm, username, password, changeOnFirstAccess);
//
//            // TODO move to caller
////            // check confirmation
////            if (confirmationRequired) {
////                // generate confirmation keys and send mail
////                resetConfirmation(subject, realm, userId, true);
////            } else {
////                // auto approve
////                approveConfirmation(subject, realm, userId);
////            }
//
//            // TODO send registration event
//
//        } catch (NoSuchUserException e) {
//            // something very wrong happend
//            logger.error("no such user during updates " + e.getMessage());
//            throw new SystemException();
//        }
//
//        // persist attributes
//        List<AttributeEntity> attributes = Collections.emptyList();
//
////        if (attributesMap != null) {
////            // note: internal authority has a single provider, internal
////            attributes = attributeService.setAttributes(subject, SystemKeys.AUTHORITY_INTERNAL,
////                    SystemKeys.AUTHORITY_INTERNAL, account.getId(),
////                    attributesMap);
////        }
////
////        return InternalUserIdentity.from(account, attributes);
//
//        // note entity is still attached
//        // TODO evaluate detach
//        return account;
//
//    }

//    public InternalUserAccount updateAccount(
//            String subject,
//            String realm,
//            String username,
//            String email,
//            String name,
//            String surname,
//            String lang,
//            Set<Map.Entry<String, String>> attributesMap) throws NoSuchUserException {
//
//        if (!StringUtils.hasText(username)) {
//            username = email;
//        }
//        email = email.trim().toLowerCase();
//        username = username.trim().toLowerCase();
//
//        InternalUserAccount account = accountService.getAccount(realm, username);
//        account.setSubject(subject);
//        account.setEmail(email);
//        account.setName(name);
//        account.setSurname(surname);
//        account.setLang(lang);
//
//        account = accountService.updateAccount(account);
//
////        // persist attributes
////        List<AttributeEntity> attributes = Collections.emptyList();
////
////        if (attributesMap != null) {
////            // note: internal authority has a single provider, internal
////            attributes = attributeService.setAttributes(subject, SystemKeys.AUTHORITY_INTERNAL,
////                    SystemKeys.AUTHORITY_INTERNAL, account.getId(),
////                    attributesMap);
////        }
////
//        // note entity is still attached
//        // TODO evaluate detach
//        return account;
//    }

//    public InternalUserAccount updateOrCreateAccount(
//            String subject,
//            String realm,
//            String username,
//            String password,
//            String email,
//            String name,
//            String surname,
//            String lang,
//            Set<Map.Entry<String, String>> attributesMap) {
//        try {
//            if (!StringUtils.hasText(username)) {
//                username = email;
//            }
//            username = username.trim().toLowerCase();
//
//            InternalUserAccount existing = accountService.getAccount(realm, username);
//            if (existing == null) {
//                return registerAccount(subject, realm, username, password, email, name, surname, lang, attributesMap);
//            }
//            InternalUserAccount updated = updateAccount(subject, realm, username, email, name, surname, lang,
//                    attributesMap);
//            if (StringUtils.hasText(password)) {
//                updatePassword(subject, realm, updated.getUserId(), password);
//            }
//            return updated;
//        } catch (NoSuchUserException e) {
//            return registerAccount(subject, realm, username, password, email, name, surname, lang, attributesMap);
//        }
//    }

//    public void deleteAccount(
//            String subject,
//            String realm,
//            String username) throws NoSuchUserException {
//
//        accountService.deleteAccount(subject, realm, username);
//
//    }

    /*
     * Password
     */

//    public void validatePassword(String password) throws InvalidPasswordException {
//
//        if (!StringUtils.hasText(password)) {
//            throw new InvalidPasswordException("empty");
//        }
//
//        if (password.length() < passwordMinLength) {
//            throw new InvalidPasswordException("min-length");
//        }
//
//        if (password.length() > passwordMaxLength) {
//            throw new InvalidPasswordException("max-length");
//        }
//
//        if (passwordRequireAlpha) {
//            if (!password.chars().anyMatch(c -> Character.isLetter(c))) {
//                throw new InvalidPasswordException("require-alpha");
//            }
//        }
//
//        if (passwordRequireNumber) {
//            if (!password.chars().anyMatch(c -> Character.isDigit(c))) {
//                throw new InvalidPasswordException("require-number");
//            }
//        }
//
//        if (passwordRequireSpecial) {
//            // we do not count whitespace as special char
//            if (!password.chars().anyMatch(c -> (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)))) {
//                throw new InvalidPasswordException("require-special");
//            }
//        }
//
//        if (!passwordSupportWhitespace) {
//            if (password.chars().anyMatch(c -> Character.isWhitespace(c))) {
//                throw new InvalidPasswordException("contains-whitespace");
//            }
//        }
//
//    }

//    public void updatePassword(
//            String subject,
//            String realm,
//            String username,
//            String password) throws NoSuchUserException, InvalidPasswordException {
//
//        // validate password
//        validatePassword(password);
//
//        setPassword(subject, realm, username, password, false);
//    }
//
//    public InternalUserAccount setPassword(
//            String subject,
//            String realm,
//            String username,
//            String password,
//            boolean changeOnFirstAccess) throws NoSuchUserException {
//
//        try {
//            // encode password
//            String hash = PasswordHash.createHash(password);
//
//            InternalUserAccount account = accountService.updatePassword(subject, realm, username, hash,
//                    changeOnFirstAccess);
//            return account;
//        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
//            throw new SystemException(e.getMessage());
//        }
//
//    }
//
//    public void resetPassword(
//            String subject,
//            String realm,
//            String username,
//            boolean sendMail) throws NoSuchUserException {
//
//        InternalUserAccount account = accountService.getAccount(realm, username);
//
//        try {
//
//            // generate a reset key
//            String resetKey = generateKey();
//
//            account.setResetKey(resetKey);
//
//            // we set deadline as +N seconds
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.SECOND, passwordResetValidity);
//            account.setResetDeadline(calendar.getTime());
//            account = accountService.updateAccount(account);
//
//            sendResetMail(account, resetKey);
//
//        } catch (NoSuchAlgorithmException | InvalidKeySpecException | MessagingException e) {
//            throw new SystemException(e.getMessage());
//        }
//    }
//
//    public InternalUserAccount doPasswordReset(
//            String resetKey) {
//
//        if (!StringUtils.hasText(resetKey)) {
//            throw new InvalidInputException("empty-key");
//        }
//
//        logger.debug("do reset password key " + resetKey);
//        try {
//            InternalUserAccount account = accountService.getAccountByResetKey(resetKey);
//            // validate key, we do it simple
//            boolean isValid = false;
//
//            // validate key match
//            // useless check since we fetch account with key as input..
//            boolean isMatch = resetKey.equals(account.getResetKey());
//
//            if (!isMatch) {
//                logger.error("invalid key, not matching");
//                throw new InvalidInputException("invalid-key");
//            }
//
//            // validate deadline
//            Calendar calendar = Calendar.getInstance();
//            if (account.getResetDeadline() == null) {
//                logger.error("corrupt or used, key missing deadline");
//                // do not leak reason
//                throw new InvalidInputException("invalid-key");
//            }
//
//            boolean isExpired = calendar.after(account.getResetDeadline());
//
//            if (isExpired) {
//                logger.error("expired key on " + String.valueOf(account.getResetDeadline()));
//                // do not leak reason
//                throw new InvalidInputException("invalid-key");
//            }
//
//            isValid = isMatch && !isExpired;
//
//            if (isValid) {
//                // we reset the key, single use
//                account.setResetDeadline(null);
//                account.setResetKey(null);
//                account = accountService.updateAccount(account);
//            }
//
//            // note entity is still attached
//            // TODO evaluate detach
//            return account;
//
//        } catch (NoSuchUserException ne) {
//            logger.error("invalid key, not found in db");
//            throw new InvalidInputException("invalid-key");
//        }
//    }
//
//    /*
//     * Confirmation
//     */
//    public InternalUserAccount doConfirmation(
//            String confirmationKey) {
//
//        if (!StringUtils.hasText(confirmationKey)) {
//            throw new InvalidInputException("empty-key");
//        }
//
//        logger.debug("do confirm key " + confirmationKey);
//        try {
//            InternalUserAccount account = accountService.getAccountByConfirmationKey(confirmationKey);
//            // validate key, we do it simple
//            boolean isValid = false;
//
//            // validate key match
//            // useless check since we fetch account with key as input..
//            boolean isMatch = confirmationKey.equals(account.getConfirmationKey());
//
//            if (!isMatch) {
//                logger.error("invalid key, not matching");
//                throw new InvalidInputException("invalid-key");
//            }
//
//            // validate deadline
//            Calendar calendar = Calendar.getInstance();
//            if (account.getConfirmationDeadline() == null) {
//                logger.error("corrupt or used key, missing deadline");
//                // do not leak reason
//                throw new InvalidInputException("invalid-key");
//            }
//
//            boolean isExpired = calendar.after(account.getConfirmationDeadline());
//
//            if (isExpired) {
//                logger.error("expired key on " + String.valueOf(account.getConfirmationDeadline()));
//                // do not leak reason
//                throw new InvalidInputException("invalid-key");
//            }
//
//            isValid = isMatch && !isExpired;
//
//            if (isValid) {
//                // we set confirm and reset the key, single use
//                account.setConfirmed(true);
//                account.setConfirmationDeadline(null);
//                account.setConfirmationKey(null);
//                account = accountService.updateAccount(account);
//            }
//
//            // note entity is still attached
//            // TODO evaluate detach
//            return account;
//        } catch (NoSuchUserException ne) {
//            logger.error("invalid key, not found in db");
//            throw new InvalidInputException("invalid-key");
//        }
//    }
//
//    public void updateConfirmation(
//            String subject,
//            String realm,
//            String username,
//            boolean confirmed,
//            Date confirmationDeadline,
//            String confirmationKey) throws NoSuchUserException {
//
//        InternalUserAccount account = accountService.getAccount(realm, username);
//        account.setConfirmed(confirmed);
//        account.setConfirmationDeadline(confirmationDeadline);
//        account.setConfirmationKey(confirmationKey);
//
//        account = accountService.updateAccount(account);
//
//    }
//
//    public void approveConfirmation(
//            String subject,
//            String realm,
//            String username) throws NoSuchUserException {
//
//        InternalUserAccount account = accountService.getAccount(realm, username);
//        account.setConfirmed(true);
//
//        account = accountService.updateAccount(account);
//    }
//
//    public void resetConfirmation(
//            String subject,
//            String realm,
//            String username,
//            boolean sendMail) throws NoSuchUserException {
//
//        InternalUserAccount account = accountService.getAccount(realm, username);
//
//        try {
//            // set status to false
//            account.setConfirmed(false);
//
//            // generate a solid key
//            String confirmationKey = generateKey();
//
//            account.setConfirmationKey(confirmationKey);
//
//            // we set deadline as +N seconds
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.SECOND, confirmationValidity);
//            account.setConfirmationDeadline(calendar.getTime());
//            account = accountService.updateAccount(account);
//
//            sendConfirmationMail(account, confirmationKey);
//
//        } catch (NoSuchAlgorithmException | InvalidKeySpecException | MessagingException e) {
//            throw new SystemException(e.getMessage());
//        }
//    }
//
//    /**
//     * @param reg
//     * @param key
//     * @throws RegistrationException
//     */
//    private void sendConfirmationMail(InternalUserAccount account, String key) throws MessagingException {
//        String lang = account.getLang();
//        Map<String, Object> vars = new HashMap<>();
//        vars.put("user", account);
//        vars.put("url", applicationURL + "/internal/confirm?confirmationCode=" + key);
//        String subject = messageSource.getMessage("confirmation.subject", null, Locale.forLanguageTag(lang));
//        mailService.sendEmail(account.getEmail(), "mail/confirmation_" + lang, subject, vars);
//    }
//
//    /**
//     * @param existing
//     * @param key
//     * @throws RegistrationException
//     */
//    private void sendResetMail(InternalUserAccount account, String key) throws MessagingException {
//        String lang = account.getLang();
//        Map<String, Object> vars = new HashMap<String, Object>();
//        vars.put("user", account);
//        vars.put("url", applicationURL + "/internal/confirm?reset=true&confirmationCode=" + key);
//        String subject = messageSource.getMessage("reset.subject", null, Locale.forLanguageTag(lang));
//        mailService.sendEmail(account.getEmail(), "mail/reset_" + lang, subject, vars);
//    }
//
//    /*
//     * Keys
//     */
//
//    private static String generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
//        String rnd = UUID.randomUUID().toString();
//        return rnd;
//    }
//
//    /**
//     * @param key
//     * @return
//     * @throws NoSuchUserException
//     */
//    public InternalUserAccount getAccountByConfirmationKey(String key) throws NoSuchUserException {
//        return accountService.getAccountByConfirmationKey(key);
//    }

}
