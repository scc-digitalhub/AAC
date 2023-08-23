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

package it.smartcommunitylab.aac.credentials.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.persistence.ResourceEntity;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.provider.CredentialsService;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.service.UserEntityService;
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
        CredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
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
        CredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
            .getAuthority(res.getAuthority())
            .getProvider(res.getProvider());

        // find
        return service.getCredential(res.getResourceId());
    }

    //    @Transactional(readOnly = false)
    //    public EditableUserCredentials getEditableUserCredentials(String uuid)
    //            throws NoSuchCredentialException, NoSuchProviderException, NoSuchAuthorityException {
    //        logger.debug("get editable user credentials {}", StringUtils.trimAllWhitespace(uuid));
    //
    //        // resolve resource
    //        ResourceEntity res = getResource(uuid);
    //        String authorityId = res.getAuthority();
    //        String providerId = res.getProvider();
    //        String credentialId = res.getResourceId();
    //
    //        logger.debug("get editable user credentials {} via provider {}:{}", credentialId, authorityId, providerId);
    //
    //        // fetch service
    //        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
    //                .getAuthority(res.getAuthority())
    //                .getProvider(res.getProvider());
    //
    //        // fetch credential
    //        UserCredentials cred = service.getCredential(res.getResourceId());
    //
    //        // fetch editable
    //        return service.getEditableCredential(credentialId);
    //    }

    //    @Transactional(readOnly = false)
    //    public Collection<EditableUserCredentials> listEditableUserCredentials(String userId) throws NoSuchUserException {
    //        logger.debug("get editable user {} credentials", StringUtils.trimAllWhitespace(userId));
    //
    //        // fetch user
    //        UserEntity ue = userService.getUser(userId);
    //        String realm = ue.getRealm();
    //
    //        // collect from all providers for the same realm
    //        List<AccountCredentialsService<?, ?, ?, ?>> services = credentialsServiceAuthorityService.getAuthorities()
    //                .stream()
    //                .flatMap(e -> e.getProvidersByRealm(realm).stream())
    //                .collect(Collectors.toList());
    //
    //        List<EditableUserCredentials> creds = services.stream().flatMap(
    //                s -> s.listCredentialsByUser(userId).stream()
    //                        .filter(c -> c.isActive())
    //                        .map(a -> {
    //                            try {
    //                                return s.getEditableCredential(a.getCredentialsId());
    //                            } catch (NoSuchCredentialException | UnsupportedOperationException e1) {
    //                                return null;
    //                            }
    //                        }).filter(a -> a != null))
    //                .collect(Collectors.toList());
    //
    //        return creds;
    //    }

    //    @Transactional(readOnly = false)
    //    public Collection<EditableUserCredentials> listEditableUserCredentials(String userId, String authorityId)
    //            throws NoSuchUserException, NoSuchAuthorityException {
    //        logger.debug("get editable user {} credentials from authority {}", StringUtils.trimAllWhitespace(userId),
    //                StringUtils.trimAllWhitespace(authorityId));
    //
    //        // fetch user
    //        UserEntity ue = userService.getUser(userId);
    //        String realm = ue.getRealm();
    //
    //        CredentialsServiceAuthority<? extends AccountCredentialsService<?, ?, ?, ?>, ?, ?, ?, ?> authority = credentialsServiceAuthorityService
    //                .getAuthority(authorityId);
    //
    //        // collect from all providers for the same realm
    //        List<? extends AccountCredentialsService<?, ?, ?, ?>> services = authority.getProvidersByRealm(realm);
    //
    //        List<EditableUserCredentials> creds = services.stream().flatMap(
    //                s -> s.listCredentialsByUser(userId).stream()
    //                        .filter(c -> c.isActive())
    //                        .map(a -> {
    //                            try {
    //                                return s.getEditableCredential(a.getCredentialsId());
    //                            } catch (NoSuchCredentialException | UnsupportedOperationException e1) {
    //                                return null;
    //                            }
    //                        }).filter(a -> a != null))
    //                .collect(Collectors.toList());
    //
    //        return creds;
    //    }

    @Transactional(readOnly = false)
    public Collection<UserCredentials> listUserCredentials(String userId) throws NoSuchUserException {
        logger.debug("get user {} credentials", StringUtils.trimAllWhitespace(userId));

        // fetch user
        UserEntity ue = userService.getUser(userId);
        String realm = ue.getRealm();

        // collect from all providers for the same realm
        List<CredentialsService<?, ?, ?, ?>> services = credentialsServiceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(e -> e.getProvidersByRealm(realm).stream())
            .collect(Collectors.toList());
        List<UserCredentials> creds = services
            .stream()
            .flatMap(s -> s.listCredentialsByUser(userId).stream())
            .collect(Collectors.toList());

        return creds;
    }

    //    @Transactional(readOnly = false)
    //    public EditableUserCredentials registerUserCredentials(String authority, String providerId, String accountId,
    //            EditableUserCredentials reg)
    //            throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
    //        logger.debug("register user {} credentials via provider {}",
    //                StringUtils.trimAllWhitespace(String.valueOf(accountId)),
    //                StringUtils.trimAllWhitespace(providerId));
    //
    //        if (reg == null) {
    //            throw new MissingDataException("registration");
    //        }
    //
    //        // fetch service
    //        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authority)
    //                .getProvider(providerId);
    //
    //        // execute
    //        return service.registerCredential(accountId, reg);
    //    }

    //    @Transactional(readOnly = false)
    //    public EditableUserCredentials editUserCredentials(String uuid, EditableUserCredentials reg)
    //            throws NoSuchCredentialException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
    //        logger.debug("edit user credentials {}", StringUtils.trimAllWhitespace(uuid));
    //
    //        if (reg == null) {
    //            throw new MissingDataException("registration");
    //        }
    //
    //        // resolve resource
    //        ResourceEntity res = getResource(uuid);
    //        String authorityId = res.getAuthority();
    //        String providerId = res.getProvider();
    //        String credentialId = res.getResourceId();
    //
    //        logger.debug("edit user credentials {} via provider {}:{}", credentialId, authorityId, providerId);
    //
    //        // fetch service
    //        AccountCredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService.getAuthority(authorityId)
    //                .getProvider(providerId);
    //
    //        // find credentials
    //        UserCredentials cred = service.getCredential(credentialId);
    //
    //        // execute
    //        return service.editEditableCredential(credentialId, reg);
    //    }

    @Transactional(readOnly = false)
    public UserCredentials createUserCredentials(
        String authority,
        String providerId,
        String accountId,
        @Nullable String credentialId,
        UserCredentials reg
    ) throws NoSuchUserException, NoSuchProviderException, RegistrationException, NoSuchAuthorityException {
        logger.debug(
            "create user {} credentials {} via provider {}",
            StringUtils.trimAllWhitespace(String.valueOf(accountId)),
            StringUtils.trimAllWhitespace(String.valueOf(credentialId)),
            StringUtils.trimAllWhitespace(providerId)
        );

        if (reg == null) {
            throw new MissingDataException("registration");
        }

        // fetch service
        CredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
            .getAuthority(authority)
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
        CredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
            .getAuthority(authorityId)
            .getProvider(providerId);

        // execute
        return service.setCredential(credentialId, reg);
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
        CredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
            .getAuthority(authorityId)
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
        CredentialsService<?, ?, ?, ?> service = credentialsServiceAuthorityService
            .getAuthority(authorityId)
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
        List<CredentialsService<?, ?, ?, ?>> services = credentialsServiceAuthorityService
            .getAuthorities()
            .stream()
            .flatMap(e -> e.getProvidersByRealm(realm).stream())
            .collect(Collectors.toList());

        services.forEach(s -> s.deleteCredentialsByUser(userId));
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
