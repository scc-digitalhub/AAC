package it.smartcommunitylab.aac.webauthn.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;

@Service
@Transactional
public class WebAuthnCredentialsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WebAuthnCredentialsRepository credentialsRepository;

    @Transactional(readOnly = true)
    public WebAuthnCredential findCredentialById(String provider, String id) {
        WebAuthnCredential c = credentialsRepository.findOne(id);
        if (c == null) {
            return null;
        }

        if (!c.getProvider().equals(provider)) {
            return null;
        }

        return credentialsRepository.detach(c);
    }

    @Transactional(readOnly = true)
    public List<WebAuthnCredential> findCredentialsByUsername(String provider, String username) {
        List<WebAuthnCredential> credentials = credentialsRepository.findByProviderAndUsername(provider, username);
        return credentials.stream()
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnCredential> findActiveCredentialsByUsername(String provider, String username) {
        List<WebAuthnCredential> credentials = credentialsRepository.findByProviderAndUsername(provider, username);
        return credentials.stream()
                .filter(c -> CredentialsStatus.ACTIVE.getValue().equals(c.getStatus()))
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnCredential> findActiveCredentialsByUserHandle(String provider, String userHandle) {
        List<WebAuthnCredential> credentials = credentialsRepository.findByProviderAndUserHandle(provider, userHandle);
        return credentials.stream()
                .filter(c -> CredentialsStatus.ACTIVE.getValue().equals(c.getStatus()))
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnCredential> findCredentialsByCredentialId(String provider, String credentialId) {
        List<WebAuthnCredential> credentials = credentialsRepository.findByProviderAndCredentialId(provider,
                credentialId);
        return credentials.stream()
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnCredential findCredentialByUserHandleAndCredentialId(String provider, String userHandle,
            String credentialId) {
        WebAuthnCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(provider, userHandle,
                credentialId);
        if (c == null) {
            return null;
        }

        return credentialsRepository.detach(c);
    }

    public WebAuthnCredential addCredential(String provider, String userHandle, String credentialId,
            WebAuthnCredential reg)
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
        WebAuthnCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(provider, userHandle,
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
        c = new WebAuthnCredential();
        c.setId(id);
        c.setProvider(provider);

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

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnCredential updateCredential(String provider, String userHandle, String credentialId,
            WebAuthnCredential reg) throws NoSuchCredentialException, RegistrationException {
        // fetch credential from repo via provided id
        WebAuthnCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(provider, userHandle,
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

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnCredential updateCredentialCounter(String provider, String userHandle, String credentialId,
            long count) throws NoSuchCredentialException, RegistrationException {
        // fetch credential from repo via provided id
        WebAuthnCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(provider, userHandle,
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

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public void deleteCredential(String provider, String userHandle, String credentialId) {
        // fetch credential from repo via provided id
        WebAuthnCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(provider, userHandle,
                credentialId);
        if (c != null) {
            credentialsRepository.delete(c);
        }
    }

    public void validateCredential(WebAuthnCredential reg) {
        // validate credentials
        if (!StringUtils.hasText(reg.getUserHandle())) {
            throw new MissingDataException("user-handle");
        }
        if (!StringUtils.hasText(reg.getCredentialId())) {
            throw new MissingDataException("credentials-id");
        }
        if (!StringUtils.hasText(reg.getPublicKeyCose())) {
            throw new MissingDataException("public-key");
        }
    }

}
