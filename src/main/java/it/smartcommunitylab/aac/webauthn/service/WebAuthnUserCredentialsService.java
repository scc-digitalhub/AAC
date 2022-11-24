package it.smartcommunitylab.aac.webauthn.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsRepository;

/*
 * An internal service which handles credential persistence for internal user accounts, via JPA.
 * 
 * We enforce detach on fetch to keep internal datasource isolated. 
 */
@Transactional
public class WebAuthnUserCredentialsService implements UserCredentialsService<WebAuthnUserCredential> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebAuthnUserCredentialsRepository credentialRepository;

    public WebAuthnUserCredentialsService(WebAuthnUserCredentialsRepository credentialRepository) {
        Assert.notNull(credentialRepository, "credential repository is mandatory");
        this.credentialRepository = credentialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByRealm(@NotNull String realm) {
        logger.debug("find credentials for realm {}", String.valueOf(realm));

        List<WebAuthnUserCredential> credentials = credentialRepository.findByRealm(realm);
        return credentials.stream().map(a -> {
            return credentialRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialsById(@NotNull String repository, @NotNull String id) {
        logger.debug("find credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        WebAuthnUserCredential credential = credentialRepository.findOne(id);
        if (credential == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return credentialRepository.detach(credential);
    }

    @Override
    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialsByUuid(@NotNull String uuid) {
        logger.debug("find credentials with uuid {}", String.valueOf(uuid));

        // uuid is id
        WebAuthnUserCredential credential = credentialRepository.findOne(uuid);
        if (credential == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        return credentialRepository.detach(credential);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByAccount(@NotNull String repository,
            @NotNull String accountId) {
        logger.debug("find credentials for account {} in repository {}", String.valueOf(accountId),
                String.valueOf(repository));

        List<WebAuthnUserCredential> credentials = credentialRepository.findByRepositoryIdAndUsername(repository,
                accountId);
        return credentials.stream().map(a -> {
            return credentialRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug("find credentials for user {} in repository {}", String.valueOf(userId),
                String.valueOf(repository));

        List<WebAuthnUserCredential> credentials = credentialRepository.findByRepositoryIdAndUserId(repository, userId);
        return credentials.stream().map(a -> {
            return credentialRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialByUserHandleAndCredentialId(String repositoryId, String userHandle,
            String credentialId) {
        WebAuthnUserCredential c = credentialRepository.findByRepositoryIdAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            return null;
        }

        return credentialRepository.detach(c);
    }

    @Override
    public WebAuthnUserCredential addCredentials(@NotNull String repository, @NotNull String id,
            @NotNull WebAuthnUserCredential reg) throws RegistrationException {
        logger.debug("add credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }

        try {
            // check if already registered
            WebAuthnUserCredential credential = credentialRepository.findOne(id);
            if (credential != null) {
                throw new DuplicatedDataException("id");
            }

            // create credential already hashed
            credential = new WebAuthnUserCredential();
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

            // credential are encrypted, return as is
            credential = credentialRepository.detach(credential);

            credential.setAuthority(reg.getAuthority());
            credential.setProvider(reg.getProvider());

            if (logger.isTraceEnabled()) {
                logger.trace("credential: {}", String.valueOf(credential));
            }

            return credential;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public WebAuthnUserCredential updateCredentials(@NotNull String repository, @NotNull String id,
            @NotNull WebAuthnUserCredential reg) throws NoSuchCredentialException, RegistrationException {
        logger.debug("update credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("registration: {}", String.valueOf(reg));
        }
        WebAuthnUserCredential credential = credentialRepository.findOne(id);
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

            // credential are encrypted, return as is
            credential = credentialRepository.detach(credential);

            if (logger.isTraceEnabled()) {
                logger.trace("credential: {}", String.valueOf(credential));
            }

            credential.setAuthority(reg.getAuthority());
            credential.setProvider(reg.getProvider());

            return credential;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public void deleteCredentials(@NotNull String repository, @NotNull String id) {
        WebAuthnUserCredential credential = credentialRepository.findOne(id);
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
        logger.debug("delete credentials for user {} in repository {}", String.valueOf(userId),
                String.valueOf(repository));

        List<WebAuthnUserCredential> credentials = credentialRepository.findByRepositoryIdAndUserId(repository, userId);
        credentialRepository.deleteAllInBatch(credentials);
    }

    @Override
    public void deleteAllCredentialsByAccount(@NotNull String repository, @NotNull String username) {
        logger.debug("delete credentials for account {} in repository {}", String.valueOf(username),
                String.valueOf(repository));

        List<WebAuthnUserCredential> credentials = credentialRepository.findByRepositoryIdAndUsername(repository,
                username);
        credentialRepository.deleteAllInBatch(credentials);
    }

}
