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

package it.smartcommunitylab.aac.credentials.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.model.CredentialsStatus;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.credentials.provider.CredentialsService;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public abstract class AbstractCredentialsService<
    R extends AbstractUserCredentials,
    E extends AbstractEditableUserCredentials,
    M extends AbstractConfigMap,
    C extends AbstractCredentialsServiceConfig<M>
>
    extends AbstractConfigurableResourceProvider<R, C, CredentialsServiceSettingsMap, M>
    implements CredentialsService<R, E, M, C>, InitializingBean {

    protected static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    protected static final String STATUS_INACTIVE = CredentialsStatus.INACTIVE.getValue();
    protected static final String STATUS_REVOKED = CredentialsStatus.REVOKED.getValue();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    protected final UserCredentialsService<R> credentialsService;

    //TODO replace with userService after refactoring userService
    protected UserEntityService userService;
    // protected UserService userService;

    // provider configuration
    protected final String repositoryId;

    protected AbstractCredentialsService(
        String authority,
        String providerId,
        UserCredentialsService<R> credentialsService,
        C providerConfig,
        String realm
    ) {
        super(authority, providerId, realm, providerConfig);
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.repositoryId = config.getRepositoryId();
        logger.debug(
            "create {} credentials service with id {} repository {}",
            String.valueOf(authority),
            String.valueOf(providerId),
            repositoryId
        );

        this.credentialsService = credentialsService;
    }

    public void setUserService(UserEntityService userService) {
        this.userService = userService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {}

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    /*
     * Credentials
     * for credentials API
     */

    @Override
    public Collection<R> listCredentials(String userId) {
        logger.debug("list credentials for user {}", String.valueOf(userId));

        // fetch all
        return credentialsService
            .findCredentialsByUser(repositoryId, userId)
            .stream()
            .map(p -> {
                // map to ourselves
                p.setProvider(getProvider());

                // clear value for extra safety
                p.eraseCredentials();
                return p;
            })
            .collect(Collectors.toList());
    }

    @Override
    public R findCredential(String credentialsId) {
        logger.debug("find credential {}", String.valueOf(credentialsId));

        R cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
        if (cred == null) {
            return null;
        }

        // map to ourselves
        cred.setProvider(getProvider());

        // clear value for extra safety
        cred.eraseCredentials();

        return cred;
    }

    @Override
    public R getCredential(String credentialsId) throws NoSuchCredentialException {
        logger.debug("get credential {}", String.valueOf(credentialsId));

        R cred = findCredential(credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        return cred;
    }

    @Override
    public R addCredential(String userId, String credentialId, UserCredentials uc)
        throws NoSuchUserException, RegistrationException {
        logger.debug("add credential for user {} with id {}", String.valueOf(userId), String.valueOf(credentialId));
        if (logger.isTraceEnabled()) {
            logger.trace("credentials: {}", String.valueOf(uc));
        }

        if (uc == null) {
            throw new RegistrationException();
        }

        // cast and handle errors
        R reg = null;
        try {
            @SuppressWarnings("unchecked")
            R u = (R) uc;
            reg = u;
        } catch (ClassCastException e) {
            logger.error("Wrong credentials class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported credential");
        }

        // fetch user
        if (userService != null) {
            UserEntity u = userService.findUser(userId);
            if (u == null) {
                throw new NoSuchUserException();
            }
        }

        // we expect model to be already validated
        // check only id and regenerate if needed
        String id = credentialId;
        if (!StringUtils.hasText(id)) {
            // generate unique id
            // TODO evaluate secure key generator in place of uuid
            id = UUID.randomUUID().toString();
        }

        R cred = credentialsService.addCredentials(repositoryId, id, reg);

        // map to ourselves
        cred.setProvider(getProvider());

        // clear value for extra safety
        cred.eraseCredentials();

        return cred;
    }

    @Override
    public R setCredential(String credentialsId, UserCredentials uc)
        throws RegistrationException, NoSuchCredentialException {
        logger.debug("set credential {}", String.valueOf(credentialsId));
        if (logger.isTraceEnabled()) {
            logger.trace("credentials: {}", String.valueOf(uc));
        }

        if (uc == null) {
            throw new RegistrationException();
        }

        // cast and handle errors
        R reg = null;
        try {
            @SuppressWarnings("unchecked")
            R u = (R) uc;
            reg = u;
        } catch (ClassCastException e) {
            logger.error("Wrong credentials class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported credential");
        }

        R cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // update registration
        cred = credentialsService.updateCredentials(repositoryId, cred.getId(), reg);

        // map to ourselves
        cred.setProvider(getProvider());

        // clear value for extra safety
        cred.eraseCredentials();

        return cred;
    }

    @Override
    public R revokeCredential(String credentialsId) throws NoSuchCredentialException, RegistrationException {
        logger.debug("revoke credential {}", String.valueOf(credentialsId));

        R cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // we can transition from any status to revoked
        if (!STATUS_REVOKED.equals(cred.getStatus())) {
            // update status
            cred.setStatus(STATUS_REVOKED);
            cred = credentialsService.updateCredentials(repositoryId, credentialsId, cred);
        }

        // map to ourselves
        cred.setProvider(getProvider());

        // clear value for extra safety
        cred.eraseCredentials();

        return cred;
    }

    @Override
    public void deleteCredential(String credentialsId) throws NoSuchCredentialException {
        logger.debug("delete credential {}", String.valueOf(credentialsId));

        R cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // delete
        credentialsService.deleteCredentials(repositoryId, credentialsId);
    }

    @Override
    public void deleteCredentials(String userId) {
        logger.debug("delete all credentials for user {}", String.valueOf(userId));

        // fetch all to collect ids
        Collection<R> credentials = credentialsService.findCredentialsByUser(repositoryId, userId);

        // delete in batch
        Set<String> ids = credentials.stream().map(p -> p.getId()).collect(Collectors.toSet());
        credentialsService.deleteAllCredentials(repositoryId, ids);
    }

    /*
     * Editable
     * Implementations *may* support editable credentials
     * TODO split
     */

    @Override
    public E getEditableCredential(String credentialId) throws NoSuchCredentialException {
        throw new UnsupportedOperationException();
    }

    @Override
    public E registerCredential(String userId, EditableUserCredentials credentials)
        throws RegistrationException, NoSuchUserException {
        throw new UnsupportedOperationException();
    }

    @Override
    public E editCredential(String userId, EditableUserCredentials credentials)
        throws RegistrationException, NoSuchCredentialException {
        throw new UnsupportedOperationException();
    }
}
