package it.smartcommunitylab.aac.webauthn.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

/**
 * For this class, everytime we should use the 'username', we
 * will instead use a string in the following format:
 * 'realmname{@link #separator}username'. This
 * is due to the fact that yubico's library assumes that two distinct users can
 * not have the same username (https://git.io/JD5Vr).
 */
public class WebAuthnYubicoCredentialsRepository implements CredentialRepository {

    private final String providerId;
    private final WebAuthnUserAccountService webAuthnUserAccountService;

    public WebAuthnYubicoCredentialsRepository(String provider,
            WebAuthnUserAccountService webAuthnUserAccountService) {
        Assert.notNull(webAuthnUserAccountService, "WebAuthn account service is mandatory");
        this.webAuthnUserAccountService = webAuthnUserAccountService;
        this.providerId = provider;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        WebAuthnUserAccount account = webAuthnUserAccountService.findByProviderAndUsername(providerId, username);
        if (account == null) {
            return Collections.emptySet();
        }
        final List<WebAuthnCredential> credentials = webAuthnUserAccountService
                .findCredentialsByUserHandle(account.getUserHandle());
        Set<PublicKeyCredentialDescriptor> descriptors = new HashSet<>();
        for (WebAuthnCredential c : credentials) {
            Set<AuthenticatorTransport> transports = StringUtils.commaDelimitedListToSet(c.getTransports())
                    .stream()
                    .map(t -> AuthenticatorTransport.of(t))
                    .collect(Collectors.toSet());
            PublicKeyCredentialDescriptor descriptor = PublicKeyCredentialDescriptor.builder()
                    .id(ByteArray.fromBase64(c.getCredentialId())).type(PublicKeyCredentialType.PUBLIC_KEY)
                    .transports(transports)
                    .build();
            descriptors.add(descriptor);
        }
        return descriptors;
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        WebAuthnUserAccount account = webAuthnUserAccountService.findByProviderAndUsername(providerId, username);
        if (account == null) {
            return Optional.empty();
        }
        return Optional.of(ByteArray.fromBase64(account.getUserHandle()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        WebAuthnUserAccount account = webAuthnUserAccountService.findByUserHandle(userHandle.getBase64());
        if (account == null) {
            return Optional.empty();
        }
        return Optional.of(account.getUsername());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        WebAuthnUserAccount acc = webAuthnUserAccountService.findByUserHandle(userHandle.getBase64());
        if (acc == null) {
            return Optional.empty();
        }
        List<WebAuthnCredential> credentials = webAuthnUserAccountService
                .findCredentialsByUserHandle(acc.getUserHandle());
        return credentials.stream()
                .filter(c -> c.getCredentialId().equals(credentialId.getBase64()))
                .findFirst()
                .map(c -> getRegisteredCredential(c));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        // In our database the credentialID already has a unique constraint
        WebAuthnCredential cred = webAuthnUserAccountService.findCredentialById(credentialId.getBase64());
        if (cred == null) {
            return Collections.emptySet();
        }
        RegisteredCredential credential = getRegisteredCredential(cred);
        if (credential == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(credential);
    }

    private RegisteredCredential getRegisteredCredential(WebAuthnCredential credential) {
        final WebAuthnUserAccount account = webAuthnUserAccountService.findByUserHandle(credential.getUserHandle());
        if (account == null) {
            return null;
        }
        return RegisteredCredential.builder().credentialId(ByteArray.fromBase64(credential.getCredentialId()))
                .userHandle(ByteArray.fromBase64(account.getUserHandle()))
                .publicKeyCose(ByteArray.fromBase64(credential.getPublicKeyCose()))
                .signatureCount(credential.getSignatureCount()).build();
    }
}
