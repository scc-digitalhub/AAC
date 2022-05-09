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
 * DEPRECATED: username is unique per provider
 * 
 * For this class, everytime we should use the 'username', we will instead use a
 * string in the following format: 'realmname{@link #separator}username'. This
 * is due to the fact that yubico's library assumes that two distinct users can
 * not have the same username (https://git.io/JD5Vr).
 */
public class WebAuthnYubicoCredentialsRepository implements CredentialRepository {

    private final String providerId;
    private final WebAuthnUserAccountService userAccountService;

    public WebAuthnYubicoCredentialsRepository(String provider,
            WebAuthnUserAccountService webAuthnUserAccountService) {
        Assert.hasText(provider, "provider identifier is required");
        Assert.notNull(webAuthnUserAccountService, "WebAuthn account service is mandatory");
        this.userAccountService = webAuthnUserAccountService;
        this.providerId = provider;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        WebAuthnUserAccount account = userAccountService.findAccountByUsername(providerId, username);
        if (account == null) {
            return Collections.emptySet();
        }

        // fetch all credentials for this user
        List<WebAuthnCredential> credentials = userAccountService.findCredentialsByUserHandle(providerId,
                account.getUserHandle());

        // build descriptors
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
        WebAuthnUserAccount account = userAccountService.findAccountByUsername(providerId, username);
        if (account == null) {
            return Optional.empty();
        }

        // yubico userhandle is base64
        return Optional.of(ByteArray.fromBase64(account.getUserHandle()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        if (userHandle == null) {
            return Optional.empty();
        }

        // yubico userhandle is base64
        String uId = userHandle.getBase64();
        WebAuthnUserAccount account = userAccountService.findAccountByUserHandle(providerId, uId);
        if (account == null) {
            return Optional.empty();
        }

        return Optional.of(account.getUsername());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        if (userHandle == null || credentialId == null) {
            return Optional.empty();
        }

        // yubico userhandle is base64
        String uId = userHandle.getBase64();
        WebAuthnUserAccount account = userAccountService.findAccountByUserHandle(providerId, uId);
        if (account == null) {
            return Optional.empty();
        }

        // fetch credentials
        String cId = credentialId.getBase64();
        WebAuthnCredential credential = userAccountService.findByCredentialByUserHandleAndId(providerId,
                account.getUserHandle(), cId);
        if (credential == null) {
            return Optional.empty();
        }

        // convert model to yubico
        return Optional.of(getRegisteredCredential(credential));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        if (credentialId == null) {
            return Collections.emptySet();
        }

        // yubico userhandle is base64
        String cId = credentialId.getBase64();

        // In our database the credentialID already has a unique constraint
        WebAuthnCredential credential = userAccountService.findCredentialById(providerId, cId);
        if (credential == null) {
            return Collections.emptySet();
        }

        // convert model to yubico
        return Collections.singleton(getRegisteredCredential(credential));
    }

    private RegisteredCredential getRegisteredCredential(WebAuthnCredential credential) {
        return RegisteredCredential.builder().credentialId(ByteArray.fromBase64(credential.getCredentialId()))
                .userHandle(ByteArray.fromBase64(credential.getUserHandle()))
                .publicKeyCose(ByteArray.fromBase64(credential.getPublicKeyCose()))
                .signatureCount(credential.getSignatureCount()).build();
    }
}
