package it.smartcommunitylab.aac.webauthn.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsRepository;

@Transactional
public class WebAuthnCredentialsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebAuthnUserCredentialsRepository credentialsRepository;

    public WebAuthnCredentialsService(WebAuthnUserCredentialsRepository credentialsRepository) {
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");
        this.credentialsRepository = credentialsRepository;
    }

    /*
     * Credentials handling
     */

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialById(String id) {
        WebAuthnUserCredential c = credentialsRepository.findOne(id);
        if (c == null) {
            return null;
        }

        return credentialsRepository.detach(c);
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByUsername(String repositoryId, String username) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndUsername(repositoryId,
                username);
        return credentials.stream()
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findActiveCredentialsByUsername(String repositoryId, String username) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndUsername(repositoryId,
                username);
        return credentials.stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findActiveCredentialsByUserHandle(String repositoryId, String userHandle) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndUserHandle(repositoryId,
                userHandle);
        return credentials.stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByCredentialId(String repositoryId, String credentialId) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndCredentialId(repositoryId,
                credentialId);
        return credentials.stream()
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialByUserHandleAndCredentialId(String repositoryId, String userHandle,
            String credentialId) {
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            return null;
        }

        return credentialsRepository.detach(c);
    }

    public WebAuthnUserCredential addCredential(String repositoryId, String userHandle, String credentialId,
            WebAuthnUserCredential reg)
            throws RegistrationException {

        // extract relevant data
        String username = reg.getUsername();
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }

        String publicKeyCose = reg.getPublicKeyCose();
        if (!StringUtils.hasText(publicKeyCose)) {
            throw new MissingDataException("public-key");
        }

        // check duplicate
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c != null) {
            throw new AlreadyRegisteredException();
        }

        // generate unique id
        // TODO evaluate secure key generator in place of uuid
        String id = UUID.randomUUID().toString();

        String transports = reg.getTransports();
        long signatureCount = reg.getSignatureCount();
        if (signatureCount < 0) {
            throw new InvalidDataException("signature-count");
        }

        String displayName = reg.getDisplayName();
        if (StringUtils.hasText(displayName)) {
            displayName = Jsoup.clean(displayName, Safelist.none());
        }

        // build model
        c = new WebAuthnUserCredential();
        c.setId(id);
        c.setProvider(repositoryId);

        c.setUsername(username);
        c.setUserHandle(userHandle);

        c.setCredentialId(credentialId);
        c.setDisplayName(displayName);
        c.setPublicKeyCose(publicKeyCose);
        c.setTransports(transports);
        c.setSignatureCount(signatureCount);
        c.setDiscoverable(reg.getDiscoverable());

        c.setAttestationObject(reg.getAttestationObject());
        c.setClientData(reg.getClientData());

        c.setStatus(CredentialsStatus.ACTIVE.getValue());

        logger.debug("add credential {} for {}", c.getCredentialId(), String.valueOf(userHandle));
        if (logger.isTraceEnabled()) {
            logger.trace("new credential: {}", String.valueOf(c));
        }

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnUserCredential updateCredential(String repositoryId, String userHandle, String credentialId,
            WebAuthnUserCredential reg) throws NoSuchCredentialException, RegistrationException {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            throw new NoSuchCredentialException();
        }

        // extract data
        String displayName = reg.getDisplayName();
        if (StringUtils.hasText(displayName)) {
            displayName = Jsoup.clean(displayName, Safelist.none());
        }

        long signatureCount = reg.getSignatureCount();
        if (signatureCount < 0) {
            throw new InvalidDataException("signature-count");
        }
        // update allowed fields
        c.setDisplayName(displayName);
        c.setSignatureCount(signatureCount);

        logger.debug("update credential {} displayName {} signature count {}", c.getCredentialId(),
                String.valueOf(displayName), String.valueOf(signatureCount));
        if (logger.isTraceEnabled()) {
            logger.trace("update credential: {}", String.valueOf(c));
        }

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnUserCredential updateCredentialCounter(String repositoryId, String userHandle, String credentialId,
            long count) throws NoSuchCredentialException, RegistrationException {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            throw new NoSuchCredentialException();
        }

        // allow only increment
        long signatureCount = c.getSignatureCount();
        if (count < signatureCount) {
            throw new InvalidDataException("signature-count");
        }

        // update field
        c.setSignatureCount(count);
        logger.debug("update credential {} signature count to {}", c.getCredentialId(), String.valueOf(count));

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public void deleteCredentials(String repositoryId, String userHandle) {
        // fetch all credentials and delete
        List<WebAuthnUserCredential> toDelete = credentialsRepository.findByProviderAndUserHandle(repositoryId,
                userHandle);
        credentialsRepository.deleteAllInBatch(toDelete);
    }

    public void deleteCredential(String repositoryId, String userHandle, String credentialId) {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c != null) {
            logger.debug("delete credential {} with id {}", c.getCredentialId(), c.getId());
            credentialsRepository.delete(c);
        }
    }

    public WebAuthnUserCredential revokeCredential(String repositoryId, String userHandle, String credentialId)
            throws NoSuchUserException {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            throw new NoSuchUserException();
        }

        // we can transition from any status to revoked
        if (!STATUS_REVOKED.equals(c.getStatus())) {
            // update status
            c.setStatus(STATUS_REVOKED);

            logger.debug("revoke credential {}", c.getCredentialId());
            c = credentialsRepository.saveAndFlush(c);
        }

        c = credentialsRepository.detach(c);
        return c;
    }

    /*
     * Status codes
     */
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    private static final String STATUS_REVOKED = CredentialsStatus.REVOKED.getValue();
//    private static final String STATUS_EXPIRED = CredentialsStatus.EXPIRED.getValue();

}
