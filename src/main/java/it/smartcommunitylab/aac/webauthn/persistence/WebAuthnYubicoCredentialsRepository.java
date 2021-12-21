package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * For this class, everytime we should use the 'username', we
 * will instead use a string in the following format:
 * 'realmname{@link #separator}username'. This
 * is due to the fact that yubico's library assumes that two distinct users can
 * not have the same username (https://git.io/JD5Vr).
 */
@Repository
public class WebAuthnYubicoCredentialsRepository implements CredentialRepository {

    public static final String separator = "-";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    WebAuthnUserAccountRepository userAccountRepository;
    @Autowired
    WebAuthnCredentialsRepository webAuthnCredentialsRepository;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String realmNameAndUsername) {
        try {
            WebAuthnUserAccount account = getAccountForRealmAndUsername(realmNameAndUsername);
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

    private WebAuthnUserAccount getAccountForRealmAndUsername(String realmNameAndUsername) {
        final String[] parts = realmNameAndUsername.split(separator);
        if (parts.length != 2) {
            logger.warn("Realm name and username have not been correctly separated: " + realmNameAndUsername);
            throw new IllegalArgumentException();
        }
        WebAuthnUserAccount account = userAccountRepository.findByRealmAndUsername(parts[0], parts[1]);
        return account;
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String realmAndUsername) {
        WebAuthnUserAccount account = getAccountForRealmAndUsername(realmAndUsername);
        if (account != null) {
            return Optional.of(account.getUserHandle());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        try {
            WebAuthnUserAccount account = userAccountRepository.findByUserHandle(userHandle);
            if (account == null) {
                return Optional.empty();
            }
            return Optional.of(account.getRealm() + separator + account.getUsername());
        } catch (Exception e) {
        }
        return Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        WebAuthnUserAccount acc = userAccountRepository.findByUserHandle(userHandle);
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
        WebAuthnCredential cred = webAuthnCredentialsRepository.findByCredentialId(credentialId);
        if (cred != null) {
            s.add(cred.getRegisteredCredential());
        }
        return s;
    }

    public WebAuthnUserAccount save(WebAuthnUserAccount infos) {
        return userAccountRepository.save(infos);
    }
}
