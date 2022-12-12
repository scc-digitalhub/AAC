package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.persistence.ResourceEntity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.AccountService;

@Service
@Transactional
public class UserAccountService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // base services for users
    @Autowired
    private UserEntityService userService;

    @Autowired
    private ResourceEntityService resourceService;

    @Autowired
    private AccountServiceAuthorityService accountServiceAuthorityService;

    /*
     * User account via UUID
     */
    @Transactional(readOnly = false)
    public UserAccount findUserAccount(String uuid) throws NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("find user account {}", StringUtils.trimAllWhitespace(uuid));

        // fetch resource registration to resolve
        ResourceEntity res = findResource(uuid);
        if (res == null) {
            return null;
        }

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(res.getAuthority())
                .getProvider(res.getProvider());

        // find account
        return service.findAccount(res.getResourceId());
    }

    @Transactional(readOnly = false)
    public UserAccount getUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("get user account {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("get user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(res.getProvider());

        // fetch account
        return service.getAccount(accountId);
    }

    @Transactional(readOnly = false)
    public EditableUserAccount getEditableUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("get editable user account {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("get editable user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(res.getProvider());

        // fetch account
        UserAccount account = service.getAccount(accountId);

        // fetch editable
        return service.getEditableAccount(account.getUserId(), accountId);
    }

    @Transactional(readOnly = false)
    public Collection<EditableUserAccount> listEditableUserAccounts(String userId) throws NoSuchUserException {
        logger.debug("get editable user {} accounts", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.getUser(userId);
        String realm = ue.getRealm();

        // collect from all providers for the same realm
        List<AccountService<?, ?, ?, ?>> services = accountServiceAuthorityService.getAuthorities().stream()
                .flatMap(e -> e.getProvidersByRealm(realm).stream())
                .collect(Collectors.toList());
        List<EditableUserAccount> accounts = services.stream().flatMap(
                s -> s.listAccounts(userId).stream().map(a -> {
                    try {
                        return s.getEditableAccount(a.getUserId(), a.getAccountId());
                    } catch (NoSuchUserException e1) {
                        return null;
                    }
                }).filter(a -> a != null))
                .collect(Collectors.toList());

        return accounts;
    }

    @Transactional(readOnly = false)
    public Collection<UserAccount> listUserAccounts(String userId) throws NoSuchUserException {
        logger.debug("get user {} accounts", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.getUser(userId);
        String realm = ue.getRealm();

        // collect from all providers for the same realm
        List<AccountService<?, ?, ?, ?>> services = accountServiceAuthorityService.getAuthorities().stream()
                .flatMap(e -> e.getProvidersByRealm(realm).stream())
                .collect(Collectors.toList());
        List<UserAccount> accounts = services.stream().flatMap(s -> s.listAccounts(userId).stream())
                .collect(Collectors.toList());

        return accounts;
    }

    /*
     * User account via providers
     */
    @Transactional(readOnly = false)
    public EditableUserAccount registerUserAccount(String authority, String providerId, @Nullable String userId,
            EditableUserAccount reg)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("register user {} account via provider {}", StringUtils.trimAllWhitespace(String.valueOf(userId)),
                StringUtils.trimAllWhitespace(providerId));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authority)
                .getProvider(providerId);

        // execute
        return service.registerAccount(userId, reg);
    }

    @Transactional(readOnly = false)
    public EditableUserAccount editUserAccount(String uuid, EditableUserAccount reg)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("edit user account {}", StringUtils.trimAllWhitespace(uuid));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("edit user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // find account
        UserAccount account = service.getAccount(accountId);

        // execute
        return service.editAccount(account.getUserId(), accountId, reg);
    }

    @Transactional(readOnly = false)
    public UserAccount createUserAccount(String authority, String providerId, String userId,
            @Nullable String accountId, UserAccount reg)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("create user {} account {} via provider {}", StringUtils.trimAllWhitespace(String.valueOf(userId)),
                StringUtils.trimAllWhitespace(String.valueOf(accountId)),
                StringUtils.trimAllWhitespace(providerId));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authority)
                .getProvider(providerId);

        // execute
        return service.createAccount(userId, accountId, reg);
    }

    @Transactional(readOnly = false)
    public UserAccount updateUserAccount(String uuid, UserAccount reg)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("update user account {}", StringUtils.trimAllWhitespace(uuid));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("update user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // find account
        UserAccount ua = service.getAccount(accountId);
        String userId = ua.getUserId();

        // execute
        return service.updateAccount(userId, accountId, reg);
    }

    @Transactional(readOnly = false)
    public UserAccount verifyUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("verify user account {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("verify user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // execute
        return service.verifyAccount(accountId);
    }

    @Transactional(readOnly = false)
    public UserAccount confirmUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException,
            NoSuchAuthorityException {
        logger.debug("confirm user account {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("confirm user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // execute
        return service.confirmAccount(accountId);
    }

    @Transactional(readOnly = false)
    public UserAccount unconfirmUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException,
            NoSuchAuthorityException {
        logger.debug("unconfirm user account {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("unconfirm user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // execute
        return service.unconfirmAccount(accountId);
    }

    @Transactional(readOnly = false)
    public void deleteUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("delete user account {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("delete user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // TODO delete via idp provider to also clear attributes
        service.deleteAccount(accountId);
    }

    @Transactional(readOnly = false)
    public void deleteAllUserAccouts(String userId)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("delete all user {} accounts", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.getUser(userId);
        String realm = ue.getRealm();

        // collect from all providers for the same realm
        List<AccountService<?, ?, ?, ?>> services = accountServiceAuthorityService.getAuthorities()
                .stream()
                .flatMap(e -> e.getProvidersByRealm(realm).stream())
                .collect(Collectors.toList());

        services.forEach(s -> s.deleteAccounts(userId));
    }

    @Transactional(readOnly = false)
    public UserAccount lockUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("lock user account {}", StringUtils.trimAllWhitespace(uuid));
        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("delete user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // lock account to disable login
        return service.lockAccount(accountId);
    }

    @Transactional(readOnly = false)
    public UserAccount unlockUserAccount(String uuid)
            throws NoSuchUserException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("unlock user account {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String accountId = res.getResourceId();

        logger.debug("delete user account {} via provider {}:{}", accountId, authorityId, providerId);

        // fetch service
        AccountService<?, ?, ?, ?> service = accountServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // unlock account to enable login
        return service.unlockAccount(accountId);
    }

    /*
     * Resource registrations
     * helpers
     */

    private ResourceEntity findResource(String uuid) {
        return resourceService.findResourceEntity(SystemKeys.RESOURCE_ACCOUNT, uuid);
    }

    private ResourceEntity getResource(String uuid) throws NoSuchUserException {
        ResourceEntity res = resourceService.findResourceEntity(SystemKeys.RESOURCE_ACCOUNT, uuid);
        if (res == null) {
            throw new NoSuchUserException();
        }

        return res;
    }
}
