package it.smartcommunitylab.aac.webauthn.service;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.icu.impl.Pair;
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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnCredentialCreationInfo;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;

public class WebAuthnRpService {

    WebAuthnUserAccountRepository webAuthnUserAccountRepository;

    WebAuthnCredentialsRepository webAuthnCredentialsRepository;

    SubjectService subjectService;

    final private RelyingParty rp;
    final String provider;

    private static Long TIMEOUT = 9000L;

    // TODO: civts make it so this gets cleaned from time to time
    private Map<String, WebAuthnCredentialCreationInfo> activeRegistrations = new ConcurrentHashMap<>();
    private Map<String, AssertionRequest> activeAuthentications = new ConcurrentHashMap<>();

    public WebAuthnRpService(RelyingParty rp,
            WebAuthnUserAccountRepository webAuthnUserAccountRepository,
            WebAuthnCredentialsRepository webAuthnCredentialsRepository,
            SubjectService subjectService,
            String provider) {
        this.rp = rp;
        this.provider = provider;
        this.webAuthnCredentialsRepository = webAuthnCredentialsRepository;
        this.webAuthnUserAccountRepository = webAuthnUserAccountRepository;
        this.subjectService = subjectService;
    }

    public Pair<PublicKeyCredentialCreationOptions, String> startRegistration(String username, String realm,
            Optional<String> displayName, Optional<Subject> optSub) {
        final AuthenticatorSelectionCriteria authenticatorSelection = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.REQUIRED).userVerification(UserVerificationRequirement.REQUIRED)
                .build();
        final UserIdentity user = getUserIdentityOrGenerate(username, realm, displayName, optSub);
        final StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder().user(user)
                .authenticatorSelection(authenticatorSelection).timeout(TIMEOUT).build();
        final PublicKeyCredentialCreationOptions options = rp.startRegistration(startRegistrationOptions);
        final WebAuthnCredentialCreationInfo info = new WebAuthnCredentialCreationInfo();
        info.setUsername(username);
        info.setRealm(realm);
        info.setOptions(options);
        info.setProviderId(provider);
        final String key = generateNewKey();
        activeRegistrations.put(key, info);
        return Pair.of(options, key);
    }

    /**
     * Returns the username of the user on successful authentication or null if the
     * authentication was not successful
     */
    public Optional<String> finishRegistration(
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc,
            String realm,
            String key) {
        try {
            final WebAuthnCredentialCreationInfo info = activeRegistrations.get(key);
            if (info == null || info.getRealm() != realm) {
                return Optional.empty();
            }
            final String username = info.getUsername();
            final WebAuthnUserAccount account = webAuthnUserAccountRepository.findByProviderAndUsername(provider,
                    username);
            assert (account != null);
            final PublicKeyCredentialCreationOptions options = info.getOptions();
            RegistrationResult result = rp
                    .finishRegistration(FinishRegistrationOptions.builder().request(options).response(pkc).build());
            boolean attestationIsTrusted = result.isAttestationTrusted();
            if (attestationIsTrusted) {
                final Set<WebAuthnCredential> previousCredentials = account.getCredentials();
                final WebAuthnCredential newCred = new WebAuthnCredential();
                newCred.setCreatedOn(new Date());
                newCred.setLastUsedOn(new Date());
                newCred.setCredentialId(result.getKeyId().getId().getBase64());
                newCred.setPublicKeyCose(result.getPublicKeyCose().getBase64());
                newCred.setSignatureCount(result.getSignatureCount());
                final Optional<SortedSet<AuthenticatorTransport>> transportsOpt = result.getKeyId().getTransports();
                if (transportsOpt.isPresent()) {
                    newCred.setTransports(
                            convertTransportsToString(transportsOpt.get()));
                } else {
                    newCred.setTransports("");
                }
                newCred.setParentAccount(account);

                previousCredentials.add(newCred);
                webAuthnCredentialsRepository.save(newCred);
                account.setCredentials(previousCredentials);
                webAuthnUserAccountRepository.save(account);
                activeRegistrations.remove(key);
                return Optional.of(username);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return Optional.empty();
    }

    private String convertTransportsToString(Set<AuthenticatorTransport> transports) {
        final List<String> result = new LinkedList<>();
        for (final AuthenticatorTransport t : transports) {
            if (t == AuthenticatorTransport.USB) {
                result.add("USB");
            } else if (t == AuthenticatorTransport.BLE) {
                result.add("BLE");
            } else if (t == AuthenticatorTransport.NFC) {
                result.add("NFC");
            } else if (t == AuthenticatorTransport.INTERNAL) {
                result.add("INTERNAL");
            } else {
                throw new IllegalArgumentException("Transport not found: " + t);
            }
        }
        return String.join(",", result);
    }

    public Pair<AssertionRequest, String> startLogin(String username, String realm) {
        WebAuthnUserAccount account = webAuthnUserAccountRepository.findByProviderAndUsername(provider, username);
        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder()
                .userHandle(ByteArray.fromBase64(account.getUserHandle())).timeout(TIMEOUT)
                .userVerification(UserVerificationRequirement.REQUIRED).username(username).build();
        AssertionRequest startAssertion = rp.startAssertion(startAssertionOptions);
        final String key = generateNewKey();
        activeAuthentications.put(key, startAssertion);
        return Pair.of(startAssertion, key);
    }

    private String generateNewKey() {
        String uuid = UUID.randomUUID().toString();
        return uuid;
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
            String sessionId) {
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
                    ByteArray cCredentialId = ByteArray.fromBase64(c.getCredentialId());
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

    UserIdentity getUserIdentityOrGenerate(String username, String realm, Optional<String> displayNameOpt,
            Optional<Subject> optSub) {
        String displayName = displayNameOpt.orElse("");
        Optional<UserIdentity> option = getUserIdentity(username, displayName);
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
            account.setCredentials(Collections.emptySet());
            account.setRealm(realm);
            String subject;
            if (optSub.isPresent()) {
                subject = optSub.get().getSubjectId();
            } else {
                subject = subjectService.generateUuid(SystemKeys.RESOURCE_USER);
            }
            account.setSubject(subject);
            account.setUserHandle(userHandleBA.getBase64());
            account.setProvider(provider);
            webAuthnUserAccountRepository.save(account);
            return newUserIdentity;
        }
    }

    Optional<UserIdentity> getUserIdentity(String username, String displayName) {
        WebAuthnUserAccount account = webAuthnUserAccountRepository.findByProviderAndUsername(provider, username);
        if (account == null) {
            return Optional.empty();
        }
        assert (account.getUsername() == username);
        return Optional.of(UserIdentity.builder().name(account.getUsername()).displayName(displayName)
                .id(ByteArray.fromBase64(account.getUserHandle())).build());
    }
}
