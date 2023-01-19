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
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.EditableUserCredentials;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.persistence.ResourceEntity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.AccountCredentialsService;

@Service
@Transactional
public class UserCredentialsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserEntityService userService;

    @Autowired
    private ResourceEntityService resourceService;

    @Autowired
    private CredentialsServiceAuthorityService credentialsServiceAuthorityService;

    /*
     * User credentials via UUID
     */

    @Transactional(readOnly = false)
    public UserCredentials findUserCredentials(String uuid) throws NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("find user credentials {}", StringUtils.trimAllWhitespace(uuid));

        // fetch resource registration to resolve
        ResourceEntity res = findResource(uuid);
        if (res == null) {
            return null;
        }

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
                .getAuthority(res.getAuthority())
                .getProvider(res.getProvider());

        // find
        return service.findCredential(res.getResourceId());
    }

    @Transactional(readOnly = false)
    public UserCredentials getUserCredentials(String uuid)
            throws NoSuchCredentialException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("get user credentials {}", StringUtils.trimAllWhitespace(uuid));

        // fetch resource registration to resolve
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String credentialId = res.getResourceId();

        logger.debug("get user credentials {} via provider {}:{}", credentialId, authorityId, providerId);

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
                .getAuthority(res.getAuthority())
                .getProvider(res.getProvider());

        // find
        return service.getCredential(res.getResourceId());
    }

    @Transactional(readOnly = false)
    public EditableUserCredentials getEditableUserCredentials(String uuid)
            throws NoSuchCredentialException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("get editable user credentials {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String credentialId = res.getResourceId();

        logger.debug("get editable user credentials {} via provider {}:{}", credentialId, authorityId, providerId);

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
                .getAuthority(res.getAuthority())
                .getProvider(res.getProvider());

        // fetch credential
        UserCredentials cred = service.getCredential(res.getResourceId());

        // fetch editable
        return service.getEditableCredential(cred.getAccountId(), credentialId);
    }

    @Transactional(readOnly = false)
    public Collection<EditableUserCredentials> listEditableUserCredentials(String userId) throws NoSuchUserException {
        logger.debug("get editable user {} credentials", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.getUser(userId);
        String realm = ue.getRealm();

        // collect from all providers for the same realm
        List<AccountCredentialsService<?, ?, ?, ?>> services = credentialsServiceAuthorityService.getAuthorities()
                .stream()
                .flatMap(e -> e.getProvidersByRealm(realm).stream())
                .collect(Collectors.toList());

        List<EditableUserCredentials> creds = services.stream().flatMap(
                s -> s.listCredentials(userId).stream()
                        .filter(c -> c.isActive())
                        .map(a -> {
                            try {
                                return s.getEditableCredential(a.getAccountId(), a.getCredentialsId());
                            } catch (NoSuchCredentialException | UnsupportedOperationException e1) {
                                return null;
                            }
                        }).filter(a -> a != null))
                .collect(Collectors.toList());

        return creds;
    }

    @Transactional(readOnly = false)
    public Collection<UserCredentials> listUserCredentials(String userId) throws NoSuchUserException {
        logger.debug("get user {} credentials", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.getUser(userId);
        String realm = ue.getRealm();

        // collect from all providers for the same realm
        List<AccountCredentialsService<?, ?, ?, ?>> services = credentialsServiceAuthorityService.getAuthorities()
                .stream()
                .flatMap(e -> e.getProvidersByRealm(realm).stream())
                .collect(Collectors.toList());
        List<UserCredentials> creds = services.stream().flatMap(s -> s.listCredentials(userId).stream())
                .collect(Collectors.toList());

        return creds;
    }

    @Transactional(readOnly = false)
    public EditableUserCredentials registerUserCredentials(String authority, String providerId, String accountId,
            EditableUserCredentials reg)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("register user {} credentials via provider {}",
                StringUtils.trimAllWhitespace(String.valueOf(accountId)),
                StringUtils.trimAllWhitespace(providerId));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authority)
                .getProvider(providerId);

        // execute
        return service.registerCredential(accountId, reg);
    }

    @Transactional(readOnly = false)
    public EditableUserCredentials editUserCredentials(String uuid, EditableUserCredentials reg)
            throws NoSuchCredentialException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("edit user credentials {}", StringUtils.trimAllWhitespace(uuid));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String credentialId = res.getResourceId();

        logger.debug("edit user credentials {} via provider {}:{}", credentialId, authorityId, providerId);

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // find credentials
        UserCredentials cred = service.getCredential(credentialId);

        // execute
        return service.editCredential(cred.getAccountId(), credentialId, reg);
    }

    @Transactional(readOnly = false)
    public UserCredentials createUserCredentials(String authority, String providerId, String accountId,
            @Nullable String credentialId, UserCredentials reg)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("create user {} credentials {} via provider {}",
                StringUtils.trimAllWhitespace(String.valueOf(accountId)),
                StringUtils.trimAllWhitespace(String.valueOf(credentialId)),
                StringUtils.trimAllWhitespace(providerId));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authority)
                .getProvider(providerId);

        // execute
        return service.addCredential(accountId, credentialId, reg);
    }

    @Transactional(readOnly = false)
    public UserCredentials updateUserCredentials(String uuid, UserCredentials reg)
            throws NoSuchCredentialException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("update user credentials {}", StringUtils.trimAllWhitespace(uuid));

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String credentialId = res.getResourceId();

        logger.debug("update user credentials {} via provider {}:{}", credentialId, authorityId, providerId);

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // find credentials
        UserCredentials cred = service.getCredential(credentialId);

        // execute
        return service.setCredential(cred.getAccountId(), credentialId, reg);
    }

    @Transactional(readOnly = false)
    public UserCredentials revokeUserCredentials(String uuid)
            throws NoSuchCredentialException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("revoke user credentials {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String credentialId = res.getResourceId();

        logger.debug("revoke user credentials {} via provider {}:{}", credentialId, authorityId, providerId);

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        // execute
        return service.revokeCredential(credentialId);
    }

    @Transactional(readOnly = false)
    public void deleteUserCredentials(String uuid)
            throws NoSuchCredentialException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("delete user credentials {}", StringUtils.trimAllWhitespace(uuid));

        // resolve resource
        ResourceEntity res = getResource(uuid);
        String authorityId = res.getAuthority();
        String providerId = res.getProvider();
        String credentialId = res.getResourceId();

        logger.debug("delete user credentials {} via provider {}:{}", credentialId, authorityId, providerId);

        // fetch service
        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authorityId)
                .getProvider(providerId);

        service.deleteCredential(credentialId);
    }

    @Transactional(readOnly = false)
    public void deleteAllUserCredentials(String userId)
            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug("delete all user {} credentials", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.getUser(userId);
        String realm = ue.getRealm();

        // collect from all providers for the same realm
        List<AccountCredentialsService<?, ?, ?, ?>> services = credentialsServiceAuthorityService.getAuthorities()
                .stream()
                .flatMap(e -> e.getProvidersByRealm(realm).stream())
                .collect(Collectors.toList());

        services.forEach(s -> s.deleteCredentials(userId));
    }

    /*
     * Resource registrations
     * helpers
     */

    private ResourceEntity findResource(String uuid) {
        return resourceService.findResourceEntity(SystemKeys.RESOURCE_CREDENTIALS, uuid);
    }

    private ResourceEntity getResource(String uuid) throws NoSuchCredentialException {
        ResourceEntity res = resourceService.findResourceEntity(SystemKeys.RESOURCE_CREDENTIALS, uuid);
        if (res == null) {
            throw new NoSuchCredentialException();
        }

        return res;
    }
}
