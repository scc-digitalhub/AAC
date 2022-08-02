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
import com.yubico.webauthn.data.exception.Base64UrlException;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;

public class WebAuthnYubicoCredentialsRepository implements CredentialRepository {

    private final String repositoryId;
    private final InternalUserAccountService userAccountService;
    private final WebAuthnCredentialsRepository credentialsRepository;

    public WebAuthnYubicoCredentialsRepository(String repositoryId,
            InternalUserAccountService userAccountService,
            WebAuthnCredentialsRepository credentialsRepository) {
        Assert.hasText(repositoryId, "repository identifier is required");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");

        this.userAccountService = userAccountService;
        this.credentialsRepository = credentialsRepository;
        this.repositoryId = repositoryId;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        // fetch all active credentials for this user
        List<WebAuthnCredential> credentials = credentialsRepository.findByProviderAndUsername(repositoryId, username)
                .stream()
                .filter(c -> CredentialsStatus.ACTIVE.getValue().equals(c.getStatus()))
                .collect(Collectors.toList());

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

    public ByteArray extractUserHandleFromAccount(InternalUserAccount account) {

//      yubico userhandle is uuid as base64
//     String userHandle = Base64.getUrlEncoder().withoutPadding().encodeToString(account.getUuid().getBytes());
//     return Optional.of(ByteArray.fromBase64(userHandle));

        // yubico userhandle is uuid
        return new ByteArray(account.getUuid().getBytes());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
        if (account == null) {
            return Optional.empty();
        }

        return Optional.of(extractUserHandleFromAccount(account));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray bytes) {
        if (bytes == null) {
            return Optional.empty();
        }

//        // yubico userhandle is base64
//        String userHandle = bytes.getBase64Url();
//        String uuid = new String(Base64.getUrlDecoder().decode(userHandle.getBytes()));

        // yubico userhandle is uuid
        String uuid = new String(bytes.getBytes());
        InternalUserAccount account = userAccountService.findAccountByUuid(repositoryId, uuid);
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

        // we use yubico ids as base64
        WebAuthnCredential credential = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                credentialId.getBase64Url(), userHandle.getBase64Url());
        if (credential == null) {
            return Optional.empty();
        }

        // convert model to yubico
        try {
            return Optional.of(toRegisteredCredential(credential));
        } catch (Base64UrlException e) {
            return Optional.empty();
        }
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        if (credentialId == null) {
            return Collections.emptySet();
        }

        // we use yubico ids as base64
        List<WebAuthnCredential> credentials = credentialsRepository.findByProviderAndCredentialId(repositoryId,
                credentialId.getBase64Url());
        return credentials.stream()
                .map(c -> {
                    try {
                        return toRegisteredCredential(c);
                    } catch (Base64UrlException e) {
                        return null;
                    }
                })
                .filter(c -> c != null)
                .collect(Collectors.toSet());

    }

    private RegisteredCredential toRegisteredCredential(WebAuthnCredential credential) throws Base64UrlException {
        return RegisteredCredential.builder()
                .credentialId(ByteArray.fromBase64Url(credential.getCredentialId()))
                .userHandle(ByteArray.fromBase64Url(credential.getUserHandle()))
                .publicKeyCose(ByteArray.fromBase64(credential.getPublicKeyCose()))
                .signatureCount(credential.getSignatureCount())
                .build();
    }
}
