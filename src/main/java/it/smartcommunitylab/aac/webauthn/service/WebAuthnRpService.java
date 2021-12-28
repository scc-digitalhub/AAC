package it.smartcommunitylab.aac.webauthn.service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.RegistrationFailedException;

import org.springframework.beans.factory.annotation.Autowired;

import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;

public class WebAuthnRpService {

    @Autowired
    private WebAuthnUserAccountRepository webAuthnUserAccountRepository;
    @Autowired
    private WebAuthnCredentialsRepository webAuthnCredentialsRepository;

    final boolean trustUnverifiedAuthenticatorResponses;
    final private RelyingParty rp;

    private static Long TIMEOUT = 9000L;

    // TODO: civts make it so this gets cleaned from time to time
    private Map<String, CredentialCreationInfo> activeRegistrations = new HashMap<>();
    private Map<String, AssertionRequest> activeAuthentications = new HashMap<>();

    public WebAuthnRpService(RelyingParty rp, boolean trustUnverifiedAuthenticatorResponses) {
        this.rp = rp;
        this.trustUnverifiedAuthenticatorResponses = trustUnverifiedAuthenticatorResponses;
    }

    public PublicKeyCredentialCreationOptions startRegistration(String username, String realm, String sessionId,
            Optional<String> displayName, String providerId) {
        final AuthenticatorSelectionCriteria authenticatorSelection = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.REQUIRED).userVerification(UserVerificationRequirement.REQUIRED)
                .build();
        final UserIdentity user = getUserIdentityOrGenerate(username, realm, displayName);
        final StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder().user(user)
                .authenticatorSelection(authenticatorSelection).timeout(TIMEOUT).build();
        final PublicKeyCredentialCreationOptions options = rp.startRegistration(startRegistrationOptions);
        final CredentialCreationInfo info = new CredentialCreationInfo();
        info.username = username;
        info.realm = realm;
        info.options = options;
        info.providerId = providerId;
        activeRegistrations.put(sessionId, info);
        return options;
    }

    /**
     * Returns the username of the user on successful authentication or null if the
     * authentication was not successful
     */
    public Optional<String> finishRegistration(String providerId,
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc,
            String sessionId, String realm) {
        try {
            final CredentialCreationInfo info = activeRegistrations.get(sessionId);
            if (info == null || info.realm != realm) {
                return Optional.empty();
            }
            final String username = info.username;
            final WebAuthnUserAccount account = webAuthnUserAccountRepository.findByRealmAndUsername(realm, username);
            assert (account != null);
            final PublicKeyCredentialCreationOptions options = info.options;
            RegistrationResult result = rp
                    .finishRegistration(FinishRegistrationOptions.builder().request(options).response(pkc).build());
            boolean attestationIsTrusted = result.isAttestationTrusted();
            if (attestationIsTrusted || trustUnverifiedAuthenticatorResponses) {
                final Set<WebAuthnCredential> previousCredentials = account.getCredentials();
                final WebAuthnCredential newCred = new WebAuthnCredential();
                newCred.setCreatedOn(new Date());
                newCred.setLastUsedOn(new Date());
                newCred.setCredentialId(result.getKeyId().getId());
                newCred.setPublicKeyCose(result.getPublicKeyCose());
                newCred.setSignatureCount(result.getSignatureCount());
                newCred.setTransports(result.getKeyId().getTransports().orElse(new TreeSet<>()));
                newCred.setParentAccount(account);

                previousCredentials.add(newCred);
                account.setCredentials(previousCredentials);
                webAuthnUserAccountRepository.save(account);
                webAuthnCredentialsRepository.save(newCred);
                activeRegistrations.remove(sessionId);
                return Optional.of(username);
            }
        } catch (RegistrationFailedException e) {
            System.out.println(e);
        }
        return Optional.empty();
    }

    public AssertionRequest startLogin(String username, String realm, String mapKey, String providerId) {
        WebAuthnUserAccount account = webAuthnUserAccountRepository.findByRealmAndUsername(realm, username);
        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder()
                .userHandle(account.getUserHandle()).timeout(TIMEOUT)
                .userVerification(UserVerificationRequirement.REQUIRED).username(username).build();
        AssertionRequest startAssertion = rp.startAssertion(startAssertionOptions);
        activeAuthentications.put(mapKey, startAssertion);
        return startAssertion;
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
    public Optional<String> finishLogin(
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc,
            String sessionId, String providerId) {
        try {
            AssertionRequest assertionRequest = activeAuthentications.get(sessionId);
            AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder().request(assertionRequest)
                    // The PublicKeyCredentialRequestOptions from startAssertion above
                    .response(pkc).build());
            if (result.isSuccess() && result.isSignatureCounterValid()) {
                final WebAuthnUserAccount account = webAuthnUserAccountRepository
                        .findByUserHandle(result.getUserHandle().getBase64());
                Set<WebAuthnCredential> credentials = account.getCredentials();
                Optional<WebAuthnCredential> toUpdate = Optional.empty();
                ByteArray resultCredentialId = result.getCredentialId();
                for (WebAuthnCredential c : credentials) {
                    ByteArray cCredentialId = c.getCredentialId();
                    if (cCredentialId.equals(resultCredentialId)) {
                        toUpdate = Optional.of(c);
                    }
                }
                if (toUpdate.isEmpty()) {
                    return Optional.empty();
                }
                WebAuthnCredential credential = toUpdate.get();
                credentials.remove(credential);
                credential.setSignatureCount(result.getSignatureCount());
                credential.setLastUsedOn(new Date());
                webAuthnCredentialsRepository.save(credential);
                return Optional.of(account.getUsername());
            }
        } catch (Exception e) {
        }
        return Optional.empty();
    }

    UserIdentity getUserIdentityOrGenerate(String username, String realm, Optional<String> displayNameOpt) {
        String displayName = displayNameOpt.orElse("");
        Optional<UserIdentity> option = getUserIdentity(username, realm, displayName);
        if (option.isPresent()) {
            return option.get();
        } else {
            byte[] userHandle = new byte[64];
            SecureRandom random = new SecureRandom();
            random.nextBytes(userHandle);
            final ByteArray userHandleBA = new ByteArray(userHandle);
            final UserIdentity newUserIdentity = UserIdentity.builder()
                    .name(username).displayName(displayName)
                    .id(userHandleBA).build();
            final WebAuthnUserAccount account = new WebAuthnUserAccount();
            account.setUsername(username);
            account.setCredentials(new HashSet<>());
            account.setRealm(realm);
            account.setUserHandle(userHandleBA);
            webAuthnUserAccountRepository.save(account);
            return newUserIdentity;
        }
    }

    Optional<UserIdentity> getUserIdentity(String username, String realm, String displayName) {
        WebAuthnUserAccount account = webAuthnUserAccountRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            return Optional.empty();
        }
        assert (account.getUsername() == username);
        return Optional.of(UserIdentity.builder().name(account.getUsername()).displayName(displayName)
                .id(account.getUserHandle()).build());
    }
}

class CredentialCreationInfo {
    PublicKeyCredentialCreationOptions options;
    String username;
    String realm;
    String providerId;
}