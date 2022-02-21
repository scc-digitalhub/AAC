package it.smartcommunitylab.aac.webauthn.service;

import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnCredentialCreationInfo;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnLoginResponse;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationResponse;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnRpService {

    private boolean allowUntrustedAttestation = false;
    private final WebAuthnUserAccountService webAuthnUserAccountService;

    private final SubjectService subjectService;

    private final RelyingParty rp;
    private final String provider;

    private static final Long TIMEOUT = 9000L;
    private final StringKeyGenerator keyGenerator = new Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 64);

    private final Map<String, WebAuthnCredentialCreationInfo> activeRegistrations = new ConcurrentHashMap<>();
    private final Map<String, AssertionRequest> activeAuthentications = new ConcurrentHashMap<>();

    public WebAuthnRpService(RelyingParty rp,
            WebAuthnUserAccountService webAuthnUserAccountService,
            SubjectService subjectService,
            String provider) {
        this.rp = rp;
        this.provider = provider;
        this.webAuthnUserAccountService = webAuthnUserAccountService;
        this.subjectService = subjectService;
    }

    public void setAllowUntrustedAttestation(boolean allowUntrustedAttestation) {
        this.allowUntrustedAttestation = allowUntrustedAttestation;
    }

    public WebAuthnRegistrationResponse startRegistration(String username, String displayName) {
        final AuthenticatorSelectionCriteria authenticatorSelection = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.REQUIRED).userVerification(UserVerificationRequirement.REQUIRED)
                .build();
        WebAuthnUserAccount existingAccount = webAuthnUserAccountService.findByProviderAndUsername(provider,
                username);
        if (existingAccount != null) {
            // TODO: civts, check if the user is already authenticated.
            // In that case, we should allow registering multiple credentials
            throw new WebAuthnAuthenticationException("_", "User already exists");
        }
        final UserIdentity user = generateUserIdentity(username, displayName);
        final StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder().user(user)
                .authenticatorSelection(authenticatorSelection).timeout(TIMEOUT).build();
        final PublicKeyCredentialCreationOptions options = rp.startRegistration(startRegistrationOptions);
        final WebAuthnCredentialCreationInfo info = new WebAuthnCredentialCreationInfo();
        info.setUsername(username);
        info.setOptions(options);
        info.setProviderId(provider);
        final String key = UUID.randomUUID().toString();
        activeRegistrations.put(key, info);
        final WebAuthnRegistrationResponse response = new WebAuthnRegistrationResponse();
        response.setKey(key);
        response.setOptions(options);
        return response;
    }

    /**
     * Returns:
     * - the username of the authenticated user on successful authentication
     * - null if the authentication was not successful
     * - throws a WebAuthnAuthenticationException if some other error occourred
     */
    public String finishRegistration(
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc,
            String realm,
            String key) throws WebAuthnAuthenticationException {
        try {
            final WebAuthnCredentialCreationInfo info = activeRegistrations.get(key);
            WebAuthnAuthenticationException noSuchRegistration = new WebAuthnAuthenticationException("_",
                    "Can not find matching active registration request");
            if (info == null) {
                throw noSuchRegistration;
            }
            final String username = info.getUsername();
            if (!StringUtils.hasText(username)) {
                throw new WebAuthnAuthenticationException("_",
                        "Could not finish registration: missing username");
            }
            WebAuthnUserAccount existingAccount = webAuthnUserAccountService.findByProviderAndUsername(provider,
                    username);
            if (existingAccount != null) {
                throw new WebAuthnAuthenticationException("_",
                        "Account already registered");
            }
            final PublicKeyCredentialCreationOptions options = info.getOptions();
            RegistrationResult result = rp
                    .finishRegistration(FinishRegistrationOptions.builder().request(options).response(pkc).build());
            boolean attestationIsTrusted = result.isAttestationTrusted();
            if (!attestationIsTrusted && !allowUntrustedAttestation) {
                throw new WebAuthnAuthenticationException("_", "Untrusted attestation");
            }
            // Create user account in the database
            WebAuthnUserAccount account = new WebAuthnUserAccount();
            account.setUsername(username);
            account.setRealm(realm);
            String subject = subjectService.generateUuid(SystemKeys.RESOURCE_USER);
            account.setSubject(subject);
            ByteArray userHandle = info.getOptions().getUser().getId();
            account.setUserHandle(userHandle.getBase64());
            account.setProvider(provider);
            webAuthnUserAccountService.addAccount(account);

            // Create credential in the database
            WebAuthnCredential newCred = new WebAuthnCredential();
            newCred.setCreatedOn(new Date());
            newCred.setLastUsedOn(new Date());
            newCred.setCredentialId(result.getKeyId().getId().getBase64());
            newCred.setPublicKeyCose(result.getPublicKeyCose().getBase64());
            newCred.setSignatureCount(result.getSignatureCount());
            Optional<SortedSet<AuthenticatorTransport>> transportsOpt = result.getKeyId().getTransports();
            if (transportsOpt.isPresent()) {
                SortedSet<AuthenticatorTransport> transports = transportsOpt.get();
                final List<String> transportCodes = new LinkedList<>();
                for (final AuthenticatorTransport t : transports) {
                    transportCodes.add(t.getId());
                }
                newCred.setTransports(StringUtils.collectionToCommaDelimitedString(transportCodes));
            }
            newCred.setUserHandle(account.getUserHandle());

            webAuthnUserAccountService.saveCredential(newCred);
            activeRegistrations.remove(key);
            return username;
        } catch (WebAuthnAuthenticationException | RegistrationFailedException e) {
            throw new WebAuthnAuthenticationException("_",
                    "Registration failed");
        }
    }

    public WebAuthnLoginResponse startLogin(String username) {
        WebAuthnUserAccount account = webAuthnUserAccountService.findByProviderAndUsername(provider, username);
        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder()
                .userHandle(ByteArray.fromBase64(account.getUserHandle())).timeout(TIMEOUT)
                .userVerification(UserVerificationRequirement.REQUIRED).username(username).build();
        AssertionRequest startAssertion = rp.startAssertion(startAssertionOptions);
        final String key = UUID.randomUUID().toString();
        activeAuthentications.put(key, startAssertion);
        final WebAuthnLoginResponse response = new WebAuthnLoginResponse();
        response.setAssertionRequest(startAssertion);
        response.setKey(key);
        return response;
    }

    // public AssertionRequest startLoginUsernameless(String sessionId) {
    // StartAssertionOptions startAssertionOptions =
    // StartAssertionOptions.builder().timeout(TIMEOUT)
    // .userVerification(UserVerificationRequirement.REQUIRED).build();
    // AssertionRequest startAssertion = rp.startAssertion(startAssertionOptions);
    // activeAuthentications.put(sessionId, startAssertion);
    // return startAssertion;
    // }

    /**
     * @return the authenticated username if authentication was successful, else
     *         null
     */
    public String finishLogin(
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc,
            String sessionId) throws WebAuthnAuthenticationException {
        try {
            AssertionRequest assertionRequest = activeAuthentications.get(sessionId);
            AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder().request(assertionRequest)
                    .response(pkc).build());
            if (!(result.isSuccess() && result.isSignatureCounterValid())) {
                throw new WebAuthnAuthenticationException("_",
                        "Untrusted assertion");
            }
            final WebAuthnUserAccount account = webAuthnUserAccountService
                    .findByUserHandle(result.getUserHandle().getBase64());
            if (account == null) {
                throw new WebAuthnAuthenticationException("_",
                        "Could not find the requested credential in the account");
            }
            ByteArray resultCredentialId = result.getCredentialId();
            String credentialId = resultCredentialId.getBase64();
            if (webAuthnUserAccountService.findByUserHandleAndCredentialId(result.getUserHandle().getBase64(),
                    credentialId) == null) {
                throw new WebAuthnAuthenticationException("_", "");
            }
            webAuthnUserAccountService.updateCredentialCounter(credentialId, result.getSignatureCount());
            return account.getUsername();
        } catch (WebAuthnAuthenticationException | AssertionFailedException | NoSuchUserException e) {
            throw new WebAuthnAuthenticationException("_",
                    "Login failed");
        }
    }

    private UserIdentity generateUserIdentity(String username, String displayName) {
        String userDisplayName = displayName != null ? displayName : "";
        String userHandle = keyGenerator.generateKey();
        ByteArray userHandleBA = null;
        try {
            userHandleBA = ByteArray.fromBase64Url(userHandle);
        } catch (Base64UrlException e) {
            System.out.println("The newly-generated user handle is not valid base64Url");
            return null;
        }
        UserIdentity newUserIdentity = UserIdentity.builder()
                .name(username).displayName(userDisplayName)
                .id(userHandleBA).build();
        return newUserIdentity;
    }
}
