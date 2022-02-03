package it.smartcommunitylab.aac.webauthn.provider;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnYubicoCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;

@Service
public class WebAuthnRpServiceReigistrationRepository {
    private final ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    @Autowired
    private WebAuthnUserAccountRepository webAuthnUserAccountRepository;
    @Autowired
    private WebAuthnCredentialsRepository webAuthnCredentialsRepository;
    @Autowired
    private SubjectService subjectService;

    @Value("${application.url}")
    private String applicationUrl;

    public WebAuthnRpServiceReigistrationRepository(
            ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    private RelyingParty buildRp(String providerId, WebAuthnIdentityProviderConfigMap config) {
        final String rpid = config.getRpid();
        Set<String> origins = new HashSet<>();
        // TODO: civts, remove this later
        origins.add("http://localhost");
        origins.add(applicationUrl);
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder().id(rpid).name("AAC " + providerId)
                .build();
        WebAuthnYubicoCredentialsRepository webauthnRepository = new WebAuthnYubicoCredentialsRepository(
                providerId,
                webAuthnUserAccountRepository,
                webAuthnCredentialsRepository);
        RelyingParty rp = RelyingParty.builder().identity(rpIdentity).credentialRepository(webauthnRepository)
                .allowUntrustedAttestation(config.isTrustUnverifiedAuthenticatorResponses()).allowOriginPort(true)
                .allowOriginSubdomain(false)
                .origins(origins)
                .build();
        return rp;
    }

    private final LoadingCache<String, WebAuthnRpService> rpServicesByProviderId = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, WebAuthnRpService>() {
                @Override
                public WebAuthnRpService load(final String providerId) throws Exception {
                    WebAuthnIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);

                    if (config == null) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }

                    RelyingParty rp = buildRp(providerId, config.getConfigMap());
                    return new WebAuthnRpService(rp,
                            webAuthnUserAccountRepository,
                            webAuthnCredentialsRepository,
                            subjectService,
                            providerId);
                }
            });

    public WebAuthnRpService get(String providerId) throws ExecutionException {
        return rpServicesByProviderId.get(providerId);
    }

}
