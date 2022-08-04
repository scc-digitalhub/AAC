package it.smartcommunitylab.aac.webauthn.service;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnCredentialCreationInfo;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;

@Service
public class WebAuthnRpService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Long TIMEOUT = 9000L;

    @Value("${application.url}")
    private String applicationUrl;

    private final InternalUserAccountService userAccountService;
    private final WebAuthnCredentialsRepository credentialsRepository;
    private final ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    // leverage a local cache for fetching rps
    // TODO cache invalidation or check on load or drop cache
    private final LoadingCache<String, RelyingParty> registrations = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build(new CacheLoader<String, RelyingParty>() {
                @Override
                public RelyingParty load(final String providerId) throws Exception {
                    WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);

                    if (config == null) {
                        throw new NoSuchProviderException();
                    }

                    // build RP configuration
                    URL publicAppUrl = new URL(applicationUrl);
                    Set<String> origins = Collections.singleton(applicationUrl);

                    RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                            .id(publicAppUrl.getHost())
                            .name(config.getRealm())
                            .build();

                    WebAuthnYubicoCredentialsRepository webauthnRepository = new WebAuthnYubicoCredentialsRepository(
                            config.getRepositoryId(), userAccountService, credentialsRepository);

                    RelyingParty rp = RelyingParty.builder()
                            .identity(rpIdentity)
                            .credentialRepository(webauthnRepository)
                            .allowUntrustedAttestation(config.isAllowedUnstrustedAssertions())
                            .allowOriginPort(true)
                            .allowOriginSubdomain(false)
                            .origins(origins)
                            .build();

                    return rp;

                }
            });

    // TODO replace with external store
    // do note that info is NOT serializable as is
    private final Map<String, WebAuthnCredentialCreationInfo> activeRegistrations = new ConcurrentHashMap<>();

    public WebAuthnRpService(InternalUserAccountService userAccountService,
            WebAuthnCredentialsRepository credentialsRepository,
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
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
     * Registration ceremony
     * 
     * an already registered user can add credentials. For first login use either
     * confirmation link or account link via session
     */

    public WebAuthnCredentialCreationInfo getRegistration(String key) {
        return activeRegistrations.get(key);
    }

    public WebAuthnRegistrationResponse startRegistration(String registrationId, String username, String displayName)
            throws NoSuchUserException, RegistrationException, NoSuchProviderException {

        WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(registrationId);
        RelyingParty rp = getRelyingParty(registrationId);
        if (config == null || rp == null) {
            throw new NoSuchProviderException();
        }

        // load account to check status
        InternalUserAccount account = userAccountService.findAccountById(config.getRepositoryId(), username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        Optional<ByteArray> userHandle = rp.getCredentialRepository().getUserHandleForUsername(username);
        if (userHandle.isEmpty()) {
            throw new NoSuchUserException();
        }

        // build a new identity for this key
        String userDisplayName = StringUtils.hasText(displayName) ? displayName : username;
        UserIdentity identity = UserIdentity.builder()
                .name(username)
                .displayName(userDisplayName)
                .id(userHandle.get())
                .build();

        // default config
        // TODO make configurable
        AuthenticatorSelectionCriteria authenticatorSelection = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.REQUIRED)
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build();

        StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder()
                .user(identity)
                .authenticatorSelection(authenticatorSelection)
                .timeout(TIMEOUT)
                .build();

        PublicKeyCredentialCreationOptions options = rp.startRegistration(startRegistrationOptions);

        WebAuthnCredentialCreationInfo info = new WebAuthnCredentialCreationInfo();
        info.setUsername(username);
        info.setDisplayName(userDisplayName);
        info.setOptions(options);
        info.setProviderId(registrationId);

        // keep partial registration in memory
        // TODO remove, we can rebuild it later, otherwise move it to a repo
        String key = UUID.randomUUID().toString();
        activeRegistrations.put(key, info);

        // build response
        // TODO uniform content of response and active registration
        WebAuthnRegistrationResponse response = new WebAuthnRegistrationResponse();
        response.setKey(key);
        response.setOptions(options);
        return response;
    }

    public RegistrationResult finishRegistration(
            String registrationId, String key,
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc)
            throws RegistrationException, WebAuthnAuthenticationException, NoSuchUserException,
            NoSuchProviderException {
        WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(registrationId);
        RelyingParty rp = getRelyingParty(registrationId);
        if (config == null || rp == null) {
            throw new NoSuchProviderException();
        }

        WebAuthnCredentialCreationInfo info = activeRegistrations.get(key);
        if (info == null) {
            throw new RegistrationException("invalid key");
        }

        // remove, single use
        info = activeRegistrations.remove(key);

        String username = info.getUsername();
        Optional<ByteArray> userHandle = rp.getCredentialRepository().getUserHandleForUsername(username);
        if (userHandle.isEmpty()) {
            throw new NoSuchUserException();
        }

        try {
            // parse response
            PublicKeyCredentialCreationOptions options = info.getOptions();
            RegistrationResult result = rp
                    .finishRegistration(FinishRegistrationOptions.builder().request(options).response(pkc).build());
            boolean attestationIsTrusted = result.isAttestationTrusted();
            if (!attestationIsTrusted && !rp.isAllowUntrustedAttestation()) {
                throw new WebAuthnAuthenticationException("_", "Untrusted attestation");
            }

            return result;
//            // Create credential in the database
//            WebAuthnCredential credential = new WebAuthnCredential();
//            credential.setProvider(providerId);
//            credential.setUserHandle(account.getUserHandle());
//            credential.setCredentialId(result.getKeyId().getId().getBase64());
//
//            credential.setPublicKeyCose(result.getPublicKeyCose().getBase64());
//            credential.setSignatureCount(result.getSignatureCount());
//            if (result.getKeyId().getTransports().isPresent()) {
//                Set<AuthenticatorTransport> transports = result.getKeyId().getTransports().get();
//                List<String> transportCodes = transports.stream().map(t -> t.getId()).collect(Collectors.toList());
//                credential.setTransports(StringUtils.collectionToCommaDelimitedString(transportCodes));
//            }
//            credential.setCreatedOn(new Date());
//            credential.setLastUsedOn(new Date());
//
//            credential = userAccountService.addCredential(providerId, credential);
//
//            return credential;
        } catch (WebAuthnAuthenticationException | RegistrationFailedException e) {
            throw new WebAuthnAuthenticationException("_",
                    "Registration failed");
        }

    }

    /*
     * Login ceremony
     */

    public AssertionRequest startLogin(String registrationId, String username)
            throws NoSuchUserException, NoSuchProviderException {
        WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(registrationId);
        RelyingParty rp = getRelyingParty(registrationId);
        if (config == null || rp == null) {
            throw new NoSuchProviderException();
        }

        Optional<ByteArray> userHandle = rp.getCredentialRepository().getUserHandleForUsername(username);
        if (userHandle.isEmpty()) {
            throw new NoSuchUserException();
        }

        // build assertion
        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder()
                .userHandle(userHandle)
                .timeout(TIMEOUT)
                .userVerification(UserVerificationRequirement.REQUIRED)
                .username(username)
                .build();

        AssertionRequest assertionRequest = rp.startAssertion(startAssertionOptions);
        return assertionRequest;

//        // save active session via key
//        String key = UUID.randomUUID().toString();
//        activeAuthentications.put(key, startAssertion);
//
//        // build response
//        WebAuthnLoginResponse response = new WebAuthnLoginResponse();
//        response.setAssertionRequest(startAssertion);
//        response.setKey(key);
//
//        return response;
    }

    public AssertionResult finishLogin(
            String registrationId, AssertionRequest assertionRequest,
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc)
            throws NoSuchUserException, WebAuthnAuthenticationException, NoSuchProviderException {

        WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(registrationId);
        RelyingParty rp = getRelyingParty(registrationId);
        if (config == null || rp == null) {
            throw new NoSuchProviderException();
        }

        try {
            // build result
            AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder().request(assertionRequest)
                    .response(pkc).build());

            if (!(result.isSuccess() && result.isSignatureCounterValid())) {
                throw new WebAuthnAuthenticationException("", "Untrusted assertion");
            }

//            String userHandle = result.getUserHandle().getBase64();
//            WebAuthnUserAccount account = userAccountService.findAccountByUserHandle(providerId, userHandle);
//            if (account == null) {
//                throw new NoSuchUserException();
//            }
//
//            // fetch associated credential
//            String credentialId = result.getCredentialId().getBase64();
//            WebAuthnCredential credential = userAccountService.findByCredentialByUserHandleAndId(providerId, userHandle,
//                    credentialId);
//            if (credential == null) {
//                throw new WebAuthnAuthenticationException(account.getUserId(), "invalid credentials");
//            }
//
//            // update usage counter
//            credential = userAccountService.updateCredentialCounter(providerId, credentialId,
//                    result.getSignatureCount());

            return result;
        } catch (WebAuthnAuthenticationException | AssertionFailedException e) {
            throw new WebAuthnAuthenticationException("_", "Login failed");
        }
    }

}
