package it.smartcommunitylab.aac.webauthn;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
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
import com.yubico.webauthn.exception.RegistrationFailedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnYubicoCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

class CredentialCreationInfo {
    PublicKeyCredentialCreationOptions options;
    String username;
    String realm;
}

@Service
public class WebAuthnIdentityAuthority implements IdentityAuthority, InitializingBean {
    public static final String AUTHORITY_URL = "/auth/webauthn/";
    private WebAuthnIdentityProviderConfigMap defaultProviderConfig;
    private WebAuthnIdentityProviderConfig template;

    // This notation refers to application.yml file
    @Value("${authorities.webauthn.rpid}")
    private String rpid;

    @Value("${authorities.webauthn.rpName}")
    private String rpName;

    @Value("${authorities.webauthn.enableRegistration}")
    private boolean enableRegistration;

    @Value("${authorities.webauthn.enableUpdate}")
    private boolean enableUpdate;

    @Value("${authorities.webauthn.maxSessionDuration}")
    private int maxSessionDuration;

    @Value("${authorities.webauthn.trustUnverifiedAuthenticatorResponses}")
    private boolean trustUnverifiedAuthenticatorResponses;

    private final ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository;
    private final WebAuthnUserAccountService userAccountService;
    private final UserEntityService userEntityService;
    private final WebAuthnYubicoCredentialsRepository webauthnRepository;

    private RelyingParty rp;

    private static Long TIMEOUT = 9000L;

    @Autowired
    private WebAuthnUserAccountRepository webAuthnUserAccountRepository;

    @Autowired
    private WebAuthnCredentialsRepository webAuthnCredentialsRepository;

    // TODO: civts make it so this gets cleaned from time to time
    private Map<String, CredentialCreationInfo> activeRegistrations = new HashMap<>();
    private Map<String, AssertionRequest> activeAuthentications = new HashMap<>();

    public WebAuthnIdentityAuthority(
            WebAuthnUserAccountService userAccountService,
            UserEntityService userEntityService,
            ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository,
            WebAuthnYubicoCredentialsRepository webauthnRepository) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(userEntityService, "user service is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.userEntityService = userEntityService;
        this.registrationRepository = registrationRepository;
        this.webauthnRepository = webauthnRepository;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // build default config from props
        defaultProviderConfig = new WebAuthnIdentityProviderConfigMap();
        defaultProviderConfig.setRpid(rpid);
        defaultProviderConfig.setEnableRegistration(enableRegistration);
        defaultProviderConfig.setEnableUpdate(enableUpdate);
        defaultProviderConfig.setMaxSessionDuration(maxSessionDuration);

        template = new WebAuthnIdentityProviderConfig("webauthn.default", null);
        template.setConfigMap(defaultProviderConfig);
        template.setName("webauthn default");

        initRp();
    }

    private void initRp() {
        // TODO: civts, set https as sheme
        Set<String> origins = Set.of("http://" + rpid);
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder().id(rpid).name(rpName)
                .build();
        rp = RelyingParty.builder().identity(rpIdentity).credentialRepository(webauthnRepository)
                .allowUntrustedAttestation(true).allowOriginPort(true).allowOriginSubdomain(false).origins(origins)
                .build();
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_WEBAUTHN;
    }

    @Override
    public boolean hasIdentityProvider(String providerId) {
        WebAuthnIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    private final LoadingCache<String, WebAuthnIdentityService> providers = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, WebAuthnIdentityService>() {
                @Override
                public WebAuthnIdentityService load(final String id) throws Exception {
                    WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(id);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    WebAuthnIdentityService idp = new WebAuthnIdentityService(
                            id,
                            userAccountService, userEntityService,
                            config, config.getRealm());

                    return idp;

                }
            });

    @Override
    public WebAuthnIdentityService getIdentityProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");

        try {
            return providers.get(providerId);
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public List<IdentityProvider> getIdentityProviders(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<WebAuthnIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityProvider(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public String getUserProviderId(String userId) {
        // unpack id
        return extractProviderId(userId);
    }

    @Override
    public WebAuthnIdentityService getUserIdentityProvider(String userId) {
        // unpack id
        String providerId = extractProviderId(userId);
        return getIdentityProvider(providerId);
    }

    private String extractProviderId(String userId) throws IllegalArgumentException {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("empty or null id");
        }

        String[] s = userId.split(Pattern.quote("|"));

        if (s.length != 3) {
            throw new IllegalArgumentException("invalid resource id");
        }

        // check match
        if (!getAuthorityId().equals(s[0])) {
            throw new IllegalArgumentException("authority mismatch");
        }

        if (!StringUtils.hasText(s[1])) {
            throw new IllegalArgumentException("empty provider id");
        }
        return s[1];
    }

    @Override
    public WebAuthnIdentityService registerIdentityProvider(ConfigurableIdentityProvider cp) {
        // we support only identity provider as resource providers
        if (cp != null
                && getAuthorityId().equals(cp.getAuthority())
                && SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            // check if exists or id clashes with another provider from a different realm
            WebAuthnIdentityProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null) {
                if (!realm.equals(e.getRealm())) {
                    // name clash
                    throw new RegistrationException(
                            "a provider with the same id already exists under a different realm");
                }

                // same realm, unload and reload
                unregisterIdentityProvider(providerId);
            }

            // we also enforce a single webauthn idp per realm
            if (!registrationRepository.findByRealm(realm).isEmpty()) {
                throw new RegistrationException("an webauthn provider is already registered for the given realm");
            }

            try {
                // build config
                WebAuthnIdentityProviderConfig providerConfig = getProviderConfig(providerId, realm, cp);

                // register, we defer loading
                registrationRepository.addRegistration(providerConfig);

                // load and return
                return providers.get(providerId);
            } catch (Exception ee) {
                // cleanup
                registrationRepository.removeRegistration(providerId);

                throw new RegistrationException(ee.getMessage());
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private WebAuthnIdentityProviderConfig getProviderConfig(String provider, String realm,
            ConfigurableIdentityProvider cp) {

        // build empty config if missing
        if (cp == null) {
            cp = new ConfigurableIdentityProvider(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
        } else {
            Assert.isTrue(SystemKeys.AUTHORITY_WEBAUTHN.equals(cp.getAuthority()),
                    "configuration does not match this provider");
        }

        // merge config with default
        return WebAuthnIdentityProviderConfig.fromConfigurableProvider(
                cp, defaultProviderConfig);

    }

    @Override
    public void unregisterIdentityProvider(String providerId) {
        WebAuthnIdentityProviderConfig registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            // someone else should have already destroyed sessions
            // manual shutdown, not required here
            // WebAuthnIdentityProvider idp = providers.get(providerId);
            // idp.shutdown();

            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
            registrationRepository.removeRegistration(providerId);

        }

    }

    @Override
    public WebAuthnIdentityService getIdentityService(String providerId) {
        return getIdentityProvider(providerId);
    }

    @Override
    public List<IdentityService> getIdentityServices(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<WebAuthnIdentityProviderConfig> registrations = registrationRepository.findByRealm(realm);
        return registrations.stream().map(r -> getIdentityService(r.getProvider()))
                .filter(p -> (p != null)).collect(Collectors.toList());
    }

    @Override
    public Collection<ConfigurableIdentityProvider> getConfigurableProviderTemplates() {
        return Collections.singleton(WebAuthnIdentityProviderConfig.toConfigurableProvider(template));
    }

    @Override
    public ConfigurableIdentityProvider getConfigurableProviderTemplate(String templateId)
            throws NoSuchProviderException {
        if ("webauthn.default".equals(templateId)) {
            return WebAuthnIdentityProviderConfig.toConfigurableProvider(template);
        }

        throw new NoSuchProviderException("no templates available");
    }

    public PublicKeyCredentialCreationOptions startRegistration(String username, String realm, String sessionId,
            Optional<String> displayName) {
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
        activeRegistrations.put(sessionId, info);
        return options;
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
                    .name(realm + WebAuthnYubicoCredentialsRepository.separator + username).displayName(displayName)
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

    /**
     * Returns the username of the user on successful authentication or null if the
     * authentication was not successful
     */
    public Optional<String> finishRegistration(
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

    public AssertionRequest startLogin(String username, String realm, String mapKey) {
        WebAuthnUserAccount account = webAuthnUserAccountRepository.findByRealmAndUsername(realm, username);
        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder()
                .userHandle(account.getUserHandle()).timeout(TIMEOUT)
                .userVerification(UserVerificationRequirement.REQUIRED).username(username).build();
        AssertionRequest startAssertion = rp.startAssertion(startAssertionOptions);
        activeAuthentications.put(mapKey, startAssertion);
        return startAssertion;
    }

    public AssertionRequest startLoginUsernameless(String sessionId) {
        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder().timeout(TIMEOUT)
                .userVerification(UserVerificationRequirement.REQUIRED).build();
        AssertionRequest startAssertion = rp.startAssertion(startAssertionOptions);
        activeAuthentications.put(sessionId, startAssertion);
        return startAssertion;
    }

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
}
