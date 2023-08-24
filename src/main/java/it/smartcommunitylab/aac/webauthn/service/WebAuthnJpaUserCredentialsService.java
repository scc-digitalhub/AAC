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

package it.smartcommunitylab.aac.webauthn.service;

import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialEntity;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsEntityRepository;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/*
 * An internal service which handles webauthn credential persistence for users, via JPA.
 *
 * We enforce detach on fetch to keep internal datasource isolated.
 */
@Transactional
public class WebAuthnJpaUserCredentialsService implements UserCredentialsService<WebAuthnUserCredential> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebAuthnUserCredentialsEntityRepository credentialRepository;

    public WebAuthnJpaUserCredentialsService(WebAuthnUserCredentialsEntityRepository credentialRepository) {
        Assert.notNull(credentialRepository, "credential repository is mandatory");
        this.credentialRepository = credentialRepository;
    }

    @Override
    public Collection<WebAuthnUserCredential> findCredentials(@NotNull String repositoryId) {
        logger.debug("find credentials for repository {}", String.valueOf(repositoryId));

        List<WebAuthnUserCredentialEntity> credentials = credentialRepository.findByRepositoryId(repositoryId);
        return credentials.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByRealm(@NotNull String realm) {
        logger.debug("find credentials for realm {}", String.valueOf(realm));

        List<WebAuthnUserCredentialEntity> credentials = credentialRepository.findByRealm(realm);
        return credentials.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialsById(@NotNull String repository, @NotNull String id) {
        logger.debug("find credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        WebAuthnUserCredentialEntity credential = credentialRepository.findOne(id);
        if (credential == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return to(credential);
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialsByUuid(@NotNull String uuid) {
        logger.debug("find credentials with uuid {}", String.valueOf(uuid));

        // uuid is id
        WebAuthnUserCredentialEntity credential = credentialRepository.findOne(uuid);
        if (credential == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return to(credential);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug(
            "find credentials for user {} in repository {}",
            String.valueOf(userId),
            String.valueOf(repository)
        );

        List<WebAuthnUserCredentialEntity> credentials = credentialRepository.findByRepositoryIdAndUserId(
            repository,
            userId
        );
        return credentials.stream().map(a -> to(a)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialByUserHandleAndCredentialId(
        String repositoryId,
        String userHandle,
        String credentialId
    ) {
        WebAuthnUserCredentialEntity c = credentialRepository.findByRepositoryIdAndUserHandleAndCredentialId(
            repositoryId,
            userHandle,
            credentialId
        );
        if (c == null) {
            return null;
        }

        return to(c);
    }

    @Override
    public WebAuthnUserCredential addCredentials(
        @NotNull String repository,
        @NotNull String id,
        @NotNull WebAuthnUserCredential reg
    ) throws RegistrationException {
        logger.debug("add credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        try {
            // check if already registered
            WebAuthnUserCredentialEntity credential = credentialRepository.findOne(id);
            if (credential != null) {
                throw new DuplicatedDataException("id");
            }

            // create credential already hashed
            credential = new WebAuthnUserCredentialEntity();
            credential.setId(id);
            credential.setRepositoryId(repository);

            credential.setUsername(reg.getUsername());
            credential.setUserId(reg.getUserId());
            credential.setRealm(reg.getRealm());

            credential.setUserHandle(reg.getUserHandle());
            credential.setDisplayName(reg.getDisplayName());
            credential.setCredentialId(reg.getCredentialId());
            credential.setPublicKeyCose(reg.getPublicKeyCose());
            credential.setSignatureCount(0);
            credential.setTransports(reg.getTransports());
            credential.setDiscoverable(reg.getDiscoverable());
            credential.setAttestationObject(reg.getAttestationObject());
            credential.setClientData(reg.getClientData());
            credential.setLastUsedDate(null);

            // set status as active
            credential.setStatus(CredentialsStatus.ACTIVE.getValue());

            // note: use flush because we detach the entity!
            credential = credentialRepository.saveAndFlush(credential);

            if (logger.isTraceEnabled()) {
                logger.trace("credential: {}", String.valueOf(credential));
            }

            // credential are encrypted, return as is
            WebAuthnUserCredential c = to(credential);
            c.setAuthority(reg.getAuthority());
            c.setProvider(reg.getProvider());

            return c;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public WebAuthnUserCredential updateCredentials(
        @NotNull String repository,
        @NotNull String id,
        @NotNull WebAuthnUserCredential reg
    ) throws NoSuchCredentialException, RegistrationException {
        logger.debug("update credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }
        WebAuthnUserCredentialEntity credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NoSuchCredentialException();
        }

        try {
            // set update fields
            credential.setUsername(reg.getUsername());
            credential.setUserId(reg.getUserId());
            credential.setRealm(reg.getRealm());

            credential.setUserHandle(reg.getUserHandle());
            credential.setDisplayName(reg.getDisplayName());
            credential.setCredentialId(reg.getCredentialId());
            credential.setPublicKeyCose(reg.getPublicKeyCose());
            credential.setSignatureCount(reg.getSignatureCount());
            credential.setTransports(reg.getTransports());
            credential.setDiscoverable(reg.getDiscoverable());
            credential.setAttestationObject(reg.getAttestationObject());
            credential.setClientData(reg.getClientData());
            credential.setLastUsedDate(reg.getLastUsedDate());

            credential.setStatus(reg.getStatus());

            // note: use flush because we detach the entity!
            credential = credentialRepository.saveAndFlush(credential);

            if (logger.isTraceEnabled()) {
                logger.trace("credential: {}", String.valueOf(credential));
            }

            // credential are encrypted, return as is
            WebAuthnUserCredential c = to(credential);
            c.setAuthority(reg.getAuthority());
            c.setProvider(reg.getProvider());

            return c;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public void deleteCredentials(@NotNull String repository, @NotNull String id) {
        WebAuthnUserCredentialEntity credential = credentialRepository.findOne(id);
        if (credential != null) {
            logger.debug("delete credential with id {} repository {}", String.valueOf(id), String.valueOf(repository));
            credentialRepository.delete(credential);
        }
    }

    @Override
    public void deleteAllCredentials(@NotNull String repository, @NotNull Collection<String> ids) {
        logger.debug("delete credentials with id in {} repository {}", String.valueOf(ids), String.valueOf(repository));
        credentialRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    public void deleteAllCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug(
            "delete credentials for user {} in repository {}",
            String.valueOf(userId),
            String.valueOf(repository)
        );

        List<WebAuthnUserCredentialEntity> credentials = credentialRepository.findByRepositoryIdAndUserId(
            repository,
            userId
        );
        credentialRepository.deleteAllInBatch(credentials);
    }

    /*
     * Helpers
     * TODO converters?
     */

    private WebAuthnUserCredential to(WebAuthnUserCredentialEntity e) {
        WebAuthnUserCredential c = new WebAuthnUserCredential(e.getRealm(), e.getId());
        c.setRepositoryId(e.getRepositoryId());
        c.setUsername(e.getUsername());
        c.setUserId(e.getUserId());

        c.setUserHandle(e.getUserHandle());
        c.setDisplayName(e.getDisplayName());
        c.setCredentialId(e.getCredentialId());

        c.setPublicKeyCose(e.getPublicKeyCose());
        c.setSignatureCount(e.getSignatureCount());
        c.setTransports(e.getTransports());
        c.setDiscoverable(e.getDiscoverable());
        c.setAttestationObject(e.getAttestationObject());
        c.setClientData(e.getClientData());

        c.setCreateDate(e.getCreateDate());
        c.setLastUsedDate(e.getLastUsedDate());

        c.setStatus(e.getStatus());

        return c;
    }
}
