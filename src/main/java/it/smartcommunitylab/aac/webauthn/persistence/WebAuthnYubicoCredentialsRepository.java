package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;

/**
 * For this class, everytime we should use the 'username', we
 * will instead use a string in the following format:
 * 'realmname{@link #separator}username'. This
 * is due to the fact that yubico's library assumes that two distinct users can
 * not have the same username (https://git.io/JD5Vr).
 */
public class WebAuthnYubicoCredentialsRepository implements CredentialRepository {

    private final String providerId;
    private WebAuthnUserAccountRepository userAccountRepository;
    private WebAuthnCredentialsRepository webAuthnCredentialsRepository;

    public WebAuthnYubicoCredentialsRepository(String provider,
            WebAuthnUserAccountRepository userAccountRepository,
            WebAuthnCredentialsRepository webAuthnCredentialsRepository) {
        this.userAccountRepository = userAccountRepository;
        this.webAuthnCredentialsRepository = webAuthnCredentialsRepository;
        this.providerId = provider;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        try {
            WebAuthnUserAccount account = userAccountRepository.findByProviderAndUsername(providerId, username);
            final Set<WebAuthnCredential> credentials = account.getCredentials();
            Set<PublicKeyCredentialDescriptor> descriptors = new HashSet<PublicKeyCredentialDescriptor>();
            for (WebAuthnCredential c : credentials) {
                PublicKeyCredentialDescriptor descriptor = PublicKeyCredentialDescriptor.builder()
                        .id(c.getCredentialId()).type(PublicKeyCredentialType.PUBLIC_KEY).transports(c.getTransports())
                        .build();
                descriptors.add(descriptor);
            }
            return descriptors;
        } catch (Exception e) {
        }
        return new HashSet<>();
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        WebAuthnUserAccount account = userAccountRepository.findByProviderAndUsername(providerId, username);
        if (account != null) {
            return Optional.of(account.getUserHandle());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        try {
            WebAuthnUserAccount account = userAccountRepository.findByUserHandle(userHandle.getBase64());
            if (account == null) {
                return Optional.empty();
            }
            return Optional.of(account.getUsername());
        } catch (Exception e) {
        }
        return Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        WebAuthnUserAccount acc = userAccountRepository.findByUserHandle(userHandle.getBase64());
        if (acc == null) {
            return Optional.empty();
        }
        Set<WebAuthnCredential> credentials = acc.getCredentials();
        for (final WebAuthnCredential cred : credentials) {
            if (cred.getCredentialId().equals(credentialId)) {
                return Optional.of(cred.getRegisteredCredential());
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        // In our database the credentialID already has a unique constraint
        Set<RegisteredCredential> s = new HashSet<>();
        WebAuthnCredential cred = webAuthnCredentialsRepository.findByCredentialId(credentialId.getBase64());
        if (cred != null) {
            s.add(cred.getRegisteredCredential());
        }
        return s;
    }
}
