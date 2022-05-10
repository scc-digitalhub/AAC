package it.smartcommunitylab.aac.webauthn.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;

/*
 * Local registration repository
 */

public class WebAuthnRpRegistrationRepository {

    @Value("${application.url}")
    private String applicationUrl;

    private final WebAuthnUserAccountService userAccountService;
    private ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository;

    public WebAuthnRpRegistrationRepository(
            WebAuthnUserAccountService userAccountService) {
        Assert.notNull(userAccountService, "user account service is mandatory");

        this.userAccountService = userAccountService;

        // build a local in memory repository to hold active provider configs
        this.registrationRepository = new InMemoryProviderConfigRepository<>();
    }

    public void setRegistrationRepository(
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        Assert.notNull(registrationRepository, "registrationRepository is mandatory");
        this.registrationRepository = registrationRepository;
    }

    // leverage a local cache for fetching rps
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

                    return buildRp(providerId, config);

                }
            });

    public RelyingParty findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        try {
            return registrations.get(registrationId);
        } catch (ExecutionException e) {
            return null;
        }
    }

    public void addRegistration(WebAuthnIdentityProviderConfig registration) {
        // we override old registration if present
        registrationRepository.addRegistration(registration);
    }

    public void removeRegistration(WebAuthnIdentityProviderConfig registration) {
        registrationRepository.removeRegistration(registration);
        registrations.invalidate(registration.getProvider());
    }

    public void removeRegistration(String registrationId) {
        registrationRepository.removeRegistration(registrationId);
        registrations.invalidate(registrationId);
    }

    private RelyingParty buildRp(String providerId, WebAuthnIdentityProviderConfig config)
            throws MalformedURLException {
        // build RP configuration
        URL publicAppUrl = new URL(applicationUrl);
        Set<String> origins = Collections.singleton(applicationUrl);

        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(publicAppUrl.getHost())
                .name(config.getRealm())
                .build();

        WebAuthnYubicoCredentialsRepository webauthnRepository = new WebAuthnYubicoCredentialsRepository(
                providerId, userAccountService);

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

}
