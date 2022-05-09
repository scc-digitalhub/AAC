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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;

@Service
public class WebAuthnRpServiceRegistrationRepository {

    @Value("${application.url}")
    private String applicationUrl;

    private final ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository;
    private final WebAuthnUserAccountService userAccountService;

    public WebAuthnRpServiceRegistrationRepository(
            WebAuthnUserAccountService userAccountService,
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.registrationRepository = registrationRepository;
    }

    private RelyingParty buildRp(String providerId, WebAuthnIdentityProviderConfig config)
            throws MalformedURLException {
        // build RP configuration
        URL publicAppUrl = new URL(applicationUrl);
        Set<String> origins = Collections.singleton(applicationUrl);

        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(publicAppUrl.getHost())
                .name("AAC " + providerId)
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

                    RelyingParty rp = buildRp(providerId, config);
                    WebAuthnRpService rpservice = new WebAuthnRpService(rp, userAccountService, providerId);
                    rpservice.setAllowUntrustedAttestation(config.isAllowedUnstrustedAssertions());

                    return rpservice;
                }
            });

    public WebAuthnRpService get(String providerId) throws NoSuchProviderException {
        try {
            return rpServicesByProviderId.get(providerId);
        } catch (ExecutionException e) {
            throw new NoSuchProviderException();
        }
    }

}
