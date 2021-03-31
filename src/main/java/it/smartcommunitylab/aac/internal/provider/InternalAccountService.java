package it.smartcommunitylab.aac.internal.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalAccountService extends AbstractProvider implements AccountService {

    // TODO use a service to support different repositories, avoid repo+passw
    // service
    private final InternalUserAccountRepository accountRepository;
    // use password service to handle password
    private final InternalPasswordService passwordService;

    // provider configuration
    private final InternalIdentityProviderConfigMap config;

    public InternalAccountService(String providerId, InternalUserAccountRepository accountRepository, String realm,
            InternalIdentityProviderConfigMap configMap) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(configMap, "config map is mandatory");
        this.accountRepository = accountRepository;
        this.config = configMap;
        this.passwordService = new InternalPasswordService(providerId, accountRepository, realm,
                config);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public boolean canRegister() {
        return config.isEnableRegistration();
    }

    @Override
    public boolean canUpdate() {
        return config.isEnableUpdate();
    }

    @Override
    public boolean canDelete() {
        return config.isEnableDelete();
    }

    @Override
    public InternalUserAccount registerAccount(String subjectId, Collection<Entry<String, String>> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // fetch params
        String realm = getRealm();
        // TODO use a proper model for attributes
        Map<String, String> map = new HashMap<>();
        attributes.forEach(e -> map.put(e.getKey(), e.getValue()));

        // validate
        validateAttributes(map);

        // fetch
        String username = map.get("username");
        String password = map.get("password");
        String email = map.get("email");
        String name = map.get("name");
        String surname = map.get("surname");
        String lang = map.get("lang");

        // remediate missing username
        if (!StringUtils.hasText(username)) {
            if (StringUtils.hasText(email)) {
                int idx = email.indexOf('@');
                if (idx > 0) {
                    username = email.substring(0, idx);
                }
            } else if (StringUtils.hasText(name)) {
                username = StringUtils.trimAllWhitespace(name);
            }
        }

        // we expect subject to be valid
        if (!StringUtils.hasText(subjectId)) {
            throw new RegistrationException("missing-subject");

        }

        boolean changeOnFirstAccess = false;
        if (!StringUtils.hasText(password)) {
            password = passwordService.generatePassword();
            changeOnFirstAccess = true;
        } else {
            passwordService.validatePassword(password);
        }

        InternalUserAccount account = accountRepository.findByRealmAndUsername(realm, username);
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        account = new InternalUserAccount();
        account.setSubject(subjectId);
        account.setRealm(realm);
        account.setUsername(username);
        // by default disable login
        account.setPassword(null);
        account.setEmail(email);
        account.setName(name);
        account.setSurname(surname);
        account.setLang(lang);
        account.setConfirmed(false);
        account.setConfirmationDeadline(null);
        account.setConfirmationKey(null);
        account.setResetDeadline(null);
        account.setResetKey(null);
        account.setChangeOnFirstAccess(false);

        account = accountRepository.save(account);

        String userId = this.exportInternalId(username);

        // set password
        // set password
        if (changeOnFirstAccess) {
            // we should send password via mail
            // TODO
        }

        account = passwordService.setPassword(userId, password, changeOnFirstAccess);

        // TODO evaluate returning cleartext password after creation

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);

    }

    @Override
    public InternalUserAccount updateAccount(String subjectId, String userId, Collection<Entry<String, String>> attributes)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        InternalUserAccount account = getAccount(userId);
        if (!account.getSubject().equals(subjectId)) {
            throw new IllegalArgumentException("subject mismatch");
        }

        // fetch params
        Map<String, String> map = new HashMap<>();
        attributes.forEach(e -> map.put(e.getKey(), e.getValue()));

        // selective update, accept empty/null
        if (map.containsKey("name")) {
            account.setName(map.get("name"));
        }
        if (map.containsKey("surname")) {
            account.setSurname(map.get("surname"));
        }
        if (map.containsKey("email")) {
            account.setEmail(map.get("email"));
        }
        if (map.containsKey("lang")) {
            account.setLang(map.get("lang"));
        }

        account = accountRepository.saveAndFlush(account);

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);
    }

    @Override
    public void deleteAccount(String subjectId, String userId) throws NoSuchUserException {
        if (!config.isEnableDelete()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }
        InternalUserAccount account = getAccount(userId);
        if (!account.getSubject().equals(subjectId)) {
            throw new IllegalArgumentException("subject mismatch");
        }

        // remove account
        accountRepository.delete(account);
    }

    public void validateAttributes(Map<String, String> map) {
        String username = map.get("username");
        String email = map.get("email");
        String name = map.get("name");

        // remediate missing username
        if (!StringUtils.hasText(username)) {
            if (StringUtils.hasText(email)) {
                int idx = email.indexOf('@');
                if (idx > 0) {
                    username = email.substring(0, idx);
                }
            } else if (StringUtils.hasText(name)) {
                username = StringUtils.trimAllWhitespace(name);
            }
        }

        // validate
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("missing-username");
        }

        if (config.isConfirmationRequired() && !StringUtils.hasText(email)) {
            throw new IllegalArgumentException("missing-email");
        }
    }

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
}