package it.smartcommunitylab.aac.webauthn.provider;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yubico.webauthn.RelyingParty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnRpRegistrationRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;

@Service
public class WebAuthnRpServiceReigistrationRepository {

    @Autowired
    private WebAuthnRpRegistrationRepository webAuthnRpRegistrationRepository;
    private final ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    @Autowired
    private WebAuthnUserAccountRepository webAuthnUserAccountRepository;
    @Autowired
    private WebAuthnCredentialsRepository webAuthnCredentialsRepository;
    @Autowired
    private SubjectService subjectService;

    public WebAuthnRpServiceReigistrationRepository(
            ProviderRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        this.registrationRepository = registrationRepository;
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

                    final Optional<RelyingParty> optionalRp = webAuthnRpRegistrationRepository
                            .getRpByProviderId(providerId);
                    final WebAuthnIdentityProviderConfigMap configMap = config.getConfigMap();
                    final RelyingParty rp = optionalRp
                            .orElse(webAuthnRpRegistrationRepository.addRp(providerId, configMap));
                    return new WebAuthnRpService(rp,
                            webAuthnUserAccountRepository,
                            webAuthnCredentialsRepository,
                            subjectService,
                            providerId);
                }
            });

    public WebAuthnRpService getOrCreate(String providerId) throws ExecutionException {
        return rpServicesByProviderId.get(providerId);
    }

    public String getRealm(String providerId) {
        return registrationRepository.findByProviderId(providerId).getRealm();
    }

    public WebAuthnIdentityProviderConfig getProviderConfig(String providerId) {
        return registrationRepository.findByProviderId(providerId);
    }

}
