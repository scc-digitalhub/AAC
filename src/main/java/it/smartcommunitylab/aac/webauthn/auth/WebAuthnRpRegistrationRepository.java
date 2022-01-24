package it.smartcommunitylab.aac.webauthn.auth;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccountRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnYubicoCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;

@Service
public class WebAuthnRpRegistrationRepository {
    @Autowired
    WebAuthnUserAccountRepository userAccountRepository;
    @Autowired
    WebAuthnCredentialsRepository webAuthnCredentialsRepository;

    private final Map<String, RelyingParty> rps;

    public WebAuthnRpRegistrationRepository() {
        this.rps = new ConcurrentHashMap<>();
    }

    /**
     * Returns the relying party associated with this provider id, if it has been
     * previously added.
     */
    public Optional<RelyingParty> getRpByProviderId(String providerId) {
        return Optional.ofNullable(rps.get(providerId));
    }

    public RelyingParty addRp(String providerId, WebAuthnIdentityProviderConfigMap config) {
        final Pattern localhostPattern = Pattern.compile("^(localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0)$");
        final String rpid = config.getRpid();
        final Matcher localhostMatcher = localhostPattern.matcher(rpid);
        final boolean isRpidLocalhost = localhostMatcher.matches();
        Set<String> origins = new HashSet<>();
        origins.add("https://" + rpid);
        if (isRpidLocalhost) {
            origins.add("http://" + rpid);
        }
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder().id(rpid).name(config.getRpName())
                .build();
        final WebAuthnYubicoCredentialsRepository webauthnRepository = new WebAuthnYubicoCredentialsRepository(
                providerId,
                userAccountRepository,
                webAuthnCredentialsRepository);
        RelyingParty rp = RelyingParty.builder().identity(rpIdentity).credentialRepository(webauthnRepository)
                .allowUntrustedAttestation(config.isTrustUnverifiedAuthenticatorResponses()).allowOriginPort(true)
                .allowOriginSubdomain(false)
                .origins(origins)
                .build();
        rps.put(providerId, rp);
        return rp;
    }
}
