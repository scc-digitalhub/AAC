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

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.exception.Base64UrlException;

import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class WebAuthnYubicoCredentialsRepository implements CredentialRepository {

    private final String repositoryId;
    private final WebAuthnUserCredentialsRepository credentialsRepository;

    private final WebAuthnUserHandleService userHandleService;

    public WebAuthnYubicoCredentialsRepository(
        String repositoryId,
        UserAccountService<InternalUserAccount> userAccountService,
        WebAuthnUserCredentialsRepository credentialsRepository
    ) {
        Assert.hasText(repositoryId, "repository identifier is required");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");

        this.credentialsRepository = credentialsRepository;
        this.repositoryId = repositoryId;

        // build service
        this.userHandleService = new WebAuthnUserHandleService(userAccountService);
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        // fetch all active credentials for this user
        List<WebAuthnUserCredential> credentials = credentialsRepository
            .findByRepositoryIdAndUsername(repositoryId, username)
            .stream()
            .filter(c -> CredentialsStatus.ACTIVE.getValue().equals(c.getStatus()))
            .collect(Collectors.toList());

        // build descriptors
        Set<PublicKeyCredentialDescriptor> descriptors = new HashSet<>();
        for (WebAuthnUserCredential c : credentials) {
            try {
                Set<AuthenticatorTransport> transports = StringUtils
                    .commaDelimitedListToSet(c.getTransports())
                    .stream()
                    .map(t -> AuthenticatorTransport.of(t))
                    .collect(Collectors.toSet());
                PublicKeyCredentialDescriptor descriptor = PublicKeyCredentialDescriptor
                    .builder()
                    .id(ByteArray.fromBase64Url(c.getCredentialId()))
                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                    .transports(transports)
                    .build();
                descriptors.add(descriptor);
            } catch (Base64UrlException e) {
                // skip
            }
        }

        return descriptors;
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        String userHandle = userHandleService.getUserHandleForUsername(repositoryId, username);
        if (userHandle == null) {
            return Optional.empty();
        }

        return Optional.of(new ByteArray(userHandle.getBytes()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray bytes) {
        if (bytes == null) {
            return Optional.empty();
        }

        String userHandle = new String(bytes.getBytes());
        String userName = userHandleService.getUsernameForUserHandle(repositoryId, userHandle);

        return Optional.of(userName);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        if (userHandle == null || credentialId == null) {
            return Optional.empty();
        }

        // we use yubico ids as base64
        String id = credentialId.getBase64Url();

        WebAuthnUserCredential credential = credentialsRepository.findByRepositoryIdAndUserHandleAndCredentialId(
            repositoryId,
            new String(userHandle.getBytes()),
            id
        );
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
        String id = credentialId.getBase64Url();
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByRepositoryIdAndCredentialId(
            repositoryId,
            id
        );
        return credentials
            .stream()
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

    private RegisteredCredential toRegisteredCredential(WebAuthnUserCredential credential) throws Base64UrlException {
        return RegisteredCredential
            .builder()
            .credentialId(ByteArray.fromBase64Url(credential.getCredentialId()))
            .userHandle(new ByteArray(credential.getUserHandle().getBytes()))
            .publicKeyCose(ByteArray.fromBase64(credential.getPublicKeyCose()))
            .signatureCount(credential.getSignatureCount())
            .build();
    }
}
