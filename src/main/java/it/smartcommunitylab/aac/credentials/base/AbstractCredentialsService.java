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
import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.credentials.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.credentials.provider.AccountCredentialsService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public abstract class AbstractCredentialsService<
    UC extends AbstractUserCredentials,
    EC extends AbstractEditableUserCredentials,
    U extends AbstractUserAccount,
    M extends AbstractConfigMap,
    C extends AbstractCredentialsServiceConfig<M>
>
    extends AbstractConfigurableResourceProvider<UC, ConfigurableCredentialsProvider, M, C>
    implements AccountCredentialsService<UC, EC, M, C>, InitializingBean {

    protected static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    protected static final String STATUS_INACTIVE = CredentialsStatus.INACTIVE.getValue();
    protected static final String STATUS_REVOKED = CredentialsStatus.REVOKED.getValue();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    protected final UserAccountService<U> accountService;
    protected final UserCredentialsService<UC> credentialsService;
    protected ResourceEntityService resourceService;

    // provider configuration
    protected final String repositoryId;

    protected AbstractCredentialsService(
        String authority,
        String providerId,
        UserAccountService<U> userAccountService,
        UserCredentialsService<UC> credentialsService,
        C providerConfig,
        String realm
    ) {
        super(authority, providerId, realm, providerConfig);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.repositoryId = config.getRepositoryId();
        logger.debug(
            "create {} credentials service with id {} repository {}",
            String.valueOf(authority),
            String.valueOf(providerId),
            repositoryId
        );

        this.accountService = userAccountService;
        this.credentialsService = credentialsService;
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(resourceService, "resource service is mandatory");
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    /*
     * Credentials
     * for credentials API
     */
    @Override
    public Collection<UC> listCredentials(String accountId) {
        logger.debug("list credentials for account {}", String.valueOf(accountId));

        // fetch all
        return credentialsService
            .findCredentialsByAccount(repositoryId, accountId)
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
    public Collection<UC> listCredentialsByUser(String userId) {
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
    public UC findCredential(String credentialsId) {
        logger.debug("find credential {}", String.valueOf(credentialsId));

        UC cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
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
    public UC getCredential(String credentialsId) throws NoSuchCredentialException {
        logger.debug("get credential {}", String.valueOf(credentialsId));

        UC cred = findCredential(credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        return cred;
    }

    @Override
    public UC addCredential(String accountId, String credentialId, UserCredentials uc)
        throws NoSuchUserException, RegistrationException {
        logger.debug(
            "add credential for account {} with id {}",
            String.valueOf(accountId),
            String.valueOf(credentialId)
        );
        if (logger.isTraceEnabled()) {
            logger.trace("credentials: {}", String.valueOf(uc));
        }

        if (uc == null) {
            throw new RegistrationException();
        }

        // cast and handle errors
        UC reg = null;
        try {
            @SuppressWarnings("unchecked")
            UC u = (UC) uc;
            reg = u;
        } catch (ClassCastException e) {
            logger.error("Wrong credentials class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported credential");
        }

        // fetch user
        U account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // we expect model to be already validated
        // check only id and regenerate if needed
        String id = credentialId;
        if (!StringUtils.hasText(id)) {
            // generate unique id
            // TODO evaluate secure key generator in place of uuid
            id = UUID.randomUUID().toString();
        }

        UC cred = credentialsService.addCredentials(repositoryId, id, reg);

        if (resourceService != null) {
            // register
            resourceService.addResourceEntity(
                cred.getUuid(),
                SystemKeys.RESOURCE_CREDENTIALS,
                getAuthority(),
                getProvider(),
                id
            );
        }

        // map to ourselves
        cred.setProvider(getProvider());

        // clear value for extra safety
        cred.eraseCredentials();

        return cred;
    }

    @Override
    public UC setCredential(String credentialsId, UserCredentials uc)
        throws RegistrationException, NoSuchCredentialException {
        logger.debug("set credential {}", String.valueOf(credentialsId));
        if (logger.isTraceEnabled()) {
            logger.trace("credentials: {}", String.valueOf(uc));
        }

        if (uc == null) {
            throw new RegistrationException();
        }

        // cast and handle errors
        UC reg = null;
        try {
            @SuppressWarnings("unchecked")
            UC u = (UC) uc;
            reg = u;
        } catch (ClassCastException e) {
            logger.error("Wrong credentials class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported credential");
        }

        UC cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
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
    public UC revokeCredential(String credentialsId) throws NoSuchCredentialException, RegistrationException {
        logger.debug("revoke credential {}", String.valueOf(credentialsId));

        UC cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
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

        UC cred = credentialsService.findCredentialsById(repositoryId, credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // delete
        credentialsService.deleteCredentials(repositoryId, credentialsId);

        if (resourceService != null) {
            // delete resource
            resourceService.deleteResourceEntity(cred.getUuid());
        }
    }

    @Override
    public void deleteCredentials(String accountId) {
        logger.debug("delete all credentials for account {}", String.valueOf(accountId));

        // fetch all to collect ids
        Collection<UC> credentials = credentialsService.findCredentialsByAccount(repositoryId, accountId);

        // delete in batch
        Set<String> ids = credentials.stream().map(p -> p.getId()).collect(Collectors.toSet());
        credentialsService.deleteAllCredentials(repositoryId, ids);

        if (resourceService != null) {
            // remove resources
            try {
                // delete in batch
                Set<String> uuids = credentials.stream().map(p -> p.getUuid()).collect(Collectors.toSet());
                resourceService.deleteAllResourceEntities(uuids);
            } catch (RuntimeException re) {
                logger.error("error removing resources: {}", re.getMessage());
            }
        }
    }

    @Override
    public void deleteCredentialsByUser(String userId) {
        logger.debug("delete all credentials for user {}", String.valueOf(userId));

        // fetch all to collect ids
        Collection<UC> credentials = credentialsService.findCredentialsByUser(repositoryId, userId);

        // delete in batch
        Set<String> ids = credentials.stream().map(p -> p.getId()).collect(Collectors.toSet());
        credentialsService.deleteAllCredentials(repositoryId, ids);

        if (resourceService != null) {
            // remove resources
            try {
                // delete in batch
                Set<String> uuids = credentials.stream().map(p -> p.getUuid()).collect(Collectors.toSet());
                resourceService.deleteAllResourceEntities(uuids);
            } catch (RuntimeException re) {
                logger.error("error removing resources: {}", re.getMessage());
            }
        }
    }

    /*
     * Editable
     * Implementations *may* support editable credentials
     * TODO split
     */
    public Collection<EC> listEditableCredentials(String accountId) {
        return Collections.emptyList();
    }

    @Override
    public Collection<EC> listEditableCredentialsByUser(String userId) {
        return Collections.emptyList();
    }

    @Override
    public EC getEditableCredential(String credentialId) throws NoSuchCredentialException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EC registerEditableCredential(String accountId, EditableUserCredentials credentials)
        throws RegistrationException, NoSuchUserException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EC editEditableCredential(String credentialId, EditableUserCredentials credentials)
        throws RegistrationException, NoSuchCredentialException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEditableCredential(@NotNull String credentialId) throws NoSuchCredentialException {
        throw new UnsupportedOperationException();
    }
}
