package it.smartcommunitylab.aac.internal.provider;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.CredentialsService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.model.UserPasswordCredentials;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalPasswordService extends AbstractProvider implements CredentialsService {

    private final InternalUserAccountRepository accountRepository;

    // provider configuration
    private final InternalIdentityProviderConfigMap config;

    public InternalPasswordService(String providerId, InternalUserAccountRepository accountRepository, String realm,
            InternalIdentityProviderConfigMap configMap) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(configMap, "config map is mandatory");
        this.accountRepository = accountRepository;
        this.config = configMap;
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

    @Override
    public UserCredentials getUserCredentials(String userId) throws NoSuchUserException {
        // fetch user
        InternalUserAccount account = getAccount(userId);

        // password are encrypted, can't read
        // we return a placeholder to describe config
        UserPasswordCredentials credentials = new UserPasswordCredentials();
        credentials.setUserId(userId);
        credentials.setCanReset(canReset());
        credentials.setCanSet(canSet());

        return credentials;
    }

    @Override
    public UserCredentials setUserCredentials(String userId, UserCredentials credentials) throws NoSuchUserException {
        if (!config.isEnablePasswordSet()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // we support only password
        if (!(credentials instanceof UserPasswordCredentials)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String password = ((UserPasswordCredentials) credentials).getPassword();
        validatePassword(password);

        InternalUserAccount account = setPassword(userId, password, false);

        // we return a placeholder to describe config
        UserPasswordCredentials result = new UserPasswordCredentials();
        result.setUserId(userId);
        result.setCanReset(canReset());
        result.setCanSet(canSet());

        return result;
    }

    @Override
    public UserCredentials resetUserCredentials(String userId) throws NoSuchUserException {
        if (!config.isEnablePasswordReset()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // direct reset credentials to a new password, single use
        String password = generatePassword();

        InternalUserAccount account = setPassword(userId, password, true);

        // we return full credentials
        UserPasswordCredentials result = new UserPasswordCredentials();
        result.setUserId(userId);
        result.setCanReset(canReset());
        result.setCanSet(canSet());
        result.setPassword(password);

        return result;

    }

    @Override
    public String getResetUrl(String userId) throws NoSuchUserException {
        // return link for resetting credentials
        // TODO filter, will ask users for confirmation and then build a resetKey to be
        // sent via mail
        // to be implemented outside this component?
        // TODO build a realm-bound url, need updates on filters

        return InternalIdentityAuthority.AUTHORITY_URL + "pwdreset/" + getProvider();
    }

    /*
     * Manage
     */
    private InternalUserAccount getAccount(String userId) throws NoSuchUserException {
        String username = parseResourceId(userId);
        String realm = getRealm();
        InternalUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            throw new NoSuchUserException(
                    "Internal user with username " + username + " does not exist for realm " + realm);
        }

        return account;
    }

    public InternalUserAccount setPassword(
            String userId,
            String password,
            boolean changeOnFirstAccess) throws NoSuchUserException {

        try {
            // encode password
            String hash = PasswordHash.createHash(password);

            InternalUserAccount account = getAccount(userId);
            // set password already hashed
            account.setPassword(hash);
            account.setChangeOnFirstAccess(changeOnFirstAccess);

            account = accountRepository.saveAndFlush(account);
            return account;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SystemException(e.getMessage());
        }

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

}