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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
public class WebAuthnLoginRpService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${application.url}")
    private String applicationUrl;

    private final UserAccountService<InternalUserAccount> userAccountService;
    private final WebAuthnUserCredentialsRepository credentialsRepository;
    private final ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    // leverage a local cache for fetching rps
    // TODO cache invalidation or check on load or drop cache
    private final LoadingCache<String, RelyingParty> registrations = CacheBuilder
        .newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100)
        .build(
            new CacheLoader<String, RelyingParty>() {
                @Override
                public RelyingParty load(final String providerId) throws Exception {
                    WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);

                    if (config == null) {
                        throw new NoSuchProviderException();
                    }

                    // build RP configuration
                    URL publicAppUrl = new URL(applicationUrl);
                    Set<String> origins = Collections.singleton(applicationUrl);

                    RelyingPartyIdentity rpIdentity = RelyingPartyIdentity
                        .builder()
                        .id(publicAppUrl.getHost())
                        .name(config.getRealm())
                        .build();

                    WebAuthnYubicoCredentialsRepository webauthnRepository = new WebAuthnYubicoCredentialsRepository(
                        config.getRepositoryId(),
                        userAccountService,
                        credentialsRepository
                    );

                    RelyingParty rp = RelyingParty
                        .builder()
                        .identity(rpIdentity)
                        .credentialRepository(webauthnRepository)
                        .allowUntrustedAttestation(config.isAllowedUnstrustedAttestation())
                        .allowOriginPort(false)
                        .allowOriginSubdomain(false)
                        .origins(origins)
                        .build();

                    return rp;
                }
            }
        );

    public WebAuthnLoginRpService(
        UserAccountService<InternalUserAccount> userAccountService,
        WebAuthnUserCredentialsRepository credentialsRepository,
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.credentialsRepository = credentialsRepository;
        this.registrationRepository = registrationRepository;
    }

    public RelyingParty getRelyingParty(String registrationId) {
        Assert.hasText(registrationId, "id can not be null or empty");

        try {
            return registrations.get(registrationId);
        } catch (ExecutionException e) {
            return null;
        }
    }

    /*
     * Login ceremony
     */

    public AssertionRequest startLogin(String registrationId, String username)
        throws NoSuchUserException, NoSuchProviderException {
        logger.debug(
            "start login for {} with provider {}",
            StringUtils.trimAllWhitespace(username),
            StringUtils.trimAllWhitespace(registrationId)
        );

        WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(registrationId);
        RelyingParty rp = getRelyingParty(registrationId);
        if (config == null || rp == null) {
            throw new NoSuchProviderException();
        }

        Optional<ByteArray> userHandle = rp.getCredentialRepository().getUserHandleForUsername(username);
        if (userHandle.isEmpty()) {
            throw new NoSuchUserException();
        }

        int timeout = config.getLoginTimeout() * 1000;

        // build assertion
        StartAssertionOptions startAssertionOptions = StartAssertionOptions
            .builder()
            .userHandle(userHandle)
            .timeout(timeout)
            .userVerification(config.getRequireUserVerification())
            .username(username)
            .build();

        AssertionRequest assertionRequest = rp.startAssertion(startAssertionOptions);
        return assertionRequest;
    }

    public AssertionResult finishLogin(
        String registrationId,
        AssertionRequest assertionRequest,
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc
    ) throws NoSuchUserException, WebAuthnAuthenticationException, NoSuchProviderException, AssertionFailedException {
        WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(registrationId);
        RelyingParty rp = getRelyingParty(registrationId);
        if (config == null || rp == null) {
            throw new NoSuchProviderException();
        }

        logger.debug(
            "finish login for {} with provider {}",
            StringUtils.trimAllWhitespace(assertionRequest.getUsername().orElse(null)),
            StringUtils.trimAllWhitespace(registrationId)
        );

        // build result
        AssertionResult result = rp.finishAssertion(
            FinishAssertionOptions.builder().request(assertionRequest).response(pkc).build()
        );
        logger.debug("login assertion is success: {}", String.valueOf(result.isSuccess()));

        if (!(result.isSuccess() && result.isSignatureCounterValid())) {
            throw new WebAuthnAuthenticationException("", "Untrusted assertion");
        }

        return result;
    }
}
