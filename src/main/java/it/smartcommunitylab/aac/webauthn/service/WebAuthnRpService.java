package it.smartcommunitylab.aac.webauthn.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnCredentialCreationInfo;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnLoginResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

@Service
public class WebAuthnRpService {
    private static final Long TIMEOUT = 9000L;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebAuthnUserAccountService userAccountService;
    private final WebAuthnRpRegistrationRepository registrationRepository;

    // TODO replace with external store
    private final Map<String, WebAuthnCredentialCreationInfo> activeRegistrations = new ConcurrentHashMap<>();

    public WebAuthnRpService(WebAuthnUserAccountService userAccountService,
            WebAuthnRpRegistrationRepository registrationRepository) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(registrationRepository, "relaying party registration repository is mandatory");

        this.registrationRepository = registrationRepository;
        this.userAccountService = userAccountService;

    }

    /*
     * Registration ceremony
     * 
     * an already registered user can add credentials. For first login use either
     * confirmation link or account link via session
     */

    public WebAuthnRegistrationResponse startRegistration(String registrationId, String username, String displayName)
            throws NoSuchUserException, RegistrationException, NoSuchProviderException {
        RelyingParty rp = registrationRepository.findByRegistrationId(registrationId);
        if (rp == null) {
            throw new NoSuchProviderException();
        }

        String providerId = registrationId;

        WebAuthnUserAccount account = userAccountService.findAccountByUsername(providerId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // build a new identity for this key
        UserIdentity user = generateUserIdentity(username, displayName, account.getUserHandle());

        // default config
        // TODO make configurable
        AuthenticatorSelectionCriteria authenticatorSelection = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.REQUIRED)
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build();

        StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder()
                .user(user)
                .authenticatorSelection(authenticatorSelection)
                .timeout(TIMEOUT)
                .build();

        PublicKeyCredentialCreationOptions options = rp.startRegistration(startRegistrationOptions);

        WebAuthnCredentialCreationInfo info = new WebAuthnCredentialCreationInfo();
        info.setUsername(username);
        info.setOptions(options);
        info.setProviderId(providerId);

        // keep partial registration in memory
        // TODO remove, we can rebuild it later, otherwise move it to a repo
        String key = UUID.randomUUID().toString();
        activeRegistrations.put(key, info);

        // build response
        WebAuthnRegistrationResponse response = new WebAuthnRegistrationResponse();
        response.setKey(key);
        response.setOptions(options);
        return response;
    }

    public WebAuthnCredential finishRegistration(
            String registrationId, String key,
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc)
            throws RegistrationException, WebAuthnAuthenticationException, NoSuchUserException,
            NoSuchProviderException {
        RelyingParty rp = registrationRepository.findByRegistrationId(registrationId);
        if (rp == null) {
            throw new NoSuchProviderException();
        }

        String providerId = registrationId;

        WebAuthnCredentialCreationInfo info = activeRegistrations.get(key);
        if (info == null) {
            throw new RegistrationException("invalid key");
        }

        // remove, single use
        activeRegistrations.remove(key);
        String username = info.getUsername();
        WebAuthnUserAccount account = userAccountService.findAccountByUsername(providerId, username);
        if (account == null) {
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

            // Create credential in the database
            WebAuthnCredential credential = new WebAuthnCredential();
            credential.setProvider(providerId);
            credential.setUserHandle(account.getUserHandle());
            credential.setCredentialId(result.getKeyId().getId().getBase64());

            credential.setPublicKeyCose(result.getPublicKeyCose().getBase64());
            credential.setSignatureCount(result.getSignatureCount());
            if (result.getKeyId().getTransports().isPresent()) {
                Set<AuthenticatorTransport> transports = result.getKeyId().getTransports().get();
                List<String> transportCodes = transports.stream().map(t -> t.getId()).collect(Collectors.toList());
                credential.setTransports(StringUtils.collectionToCommaDelimitedString(transportCodes));
            }
            credential.setCreatedOn(new Date());
            credential.setLastUsedOn(new Date());

            credential = userAccountService.addCredential(providerId, credential);

            return credential;
        } catch (WebAuthnAuthenticationException | RegistrationFailedException e) {
            throw new WebAuthnAuthenticationException("_",
                    "Registration failed");
        }

    }

    /*
     * Login ceremony
     */

    public AssertionRequest startLogin(String registrationId, String userHandle, String username)
            throws NoSuchUserException, NoSuchProviderException {
        RelyingParty rp = registrationRepository.findByRegistrationId(registrationId);
        if (rp == null) {
            throw new NoSuchProviderException();
        }

        String providerId = registrationId;

        // build assertion
        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder()
                .userHandle(ByteArray.fromBase64(userHandle))
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
        RelyingParty rp = registrationRepository.findByRegistrationId(registrationId);
        if (rp == null) {
            throw new NoSuchProviderException();
        }

        String providerId = registrationId;

        try {
            // build result
            AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder().request(assertionRequest)
                    .response(pkc).build());

            if (!(result.isSuccess() && result.isSignatureCounterValid())) {
                throw new WebAuthnAuthenticationException("", "Untrusted assertion");
            }

            String userHandle = result.getUserHandle().getBase64();
            WebAuthnUserAccount account = userAccountService.findAccountByUserHandle(providerId, userHandle);
            if (account == null) {
                throw new NoSuchUserException();
            }

            // fetch associated credential
            String credentialId = result.getCredentialId().getBase64();
            WebAuthnCredential credential = userAccountService.findByCredentialByUserHandleAndId(providerId, userHandle,
                    credentialId);
            if (credential == null) {
                throw new WebAuthnAuthenticationException(account.getUserId(), "invalid credentials");
            }

            // update usage counter
            credential = userAccountService.updateCredentialCounter(providerId, credentialId,
                    result.getSignatureCount());

            return result;
        } catch (WebAuthnAuthenticationException | AssertionFailedException | NoSuchUserException e) {
            throw new WebAuthnAuthenticationException("_", "Login failed");
        }
    }

    /*
     * Helpers
     */
    private UserIdentity generateUserIdentity(String username, String displayName, String userHandle) {
        try {
            String userDisplayName = StringUtils.hasText(displayName) ? displayName : username;
            UserIdentity identity = UserIdentity.builder()
                    .name(username)
                    .displayName(userDisplayName)
                    .id(ByteArray.fromBase64Url(userHandle))
                    .build();

            return identity;
        } catch (Base64UrlException e) {
            logger.error("User handle is not valid base64Url");
            return null;
        }
    }
}
