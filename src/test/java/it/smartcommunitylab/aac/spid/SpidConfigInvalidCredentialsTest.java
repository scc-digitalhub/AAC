package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.InvalidIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test suite for validating the robustness of SPID Identity Provider configurations against invalid cryptographic material.
 * Verifies that the system correctly detects and rejects mismatches between private keys and X.509 certificates, throwing the appropriate exceptions.
 */
@SpringBootTest
@ActiveProfiles({"test", "test-spid"})
public class SpidConfigInvalidCredentialsTest extends BaseSpidTest {

    private String spidProviderId;
    protected InvalidIdentityProvider invalidIdentityProvider = new InvalidIdentityProvider();

    @BeforeEach
    public void setupConfiguration() {
        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();

                spidProviderId = idps.stream().filter(
                    idp -> "spid-test-credentials".equals(idp.getName()))
                    .findFirst().orElseThrow().getProvider();

                SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(spidProviderId).getConfigMap();

                assertThat(configmap.getSigningCredentials()).hasSizeGreaterThanOrEqualTo(2);
                assertThat(configmap.getSigningKey()).isNotBlank();
                assertThat(configmap.getSigningCertificate()).isNotBlank();
                assertThat(configmap.getSigningKey()).startsWith("-----BEGIN PRIVATE KEY-----");
                assertThat(configmap.getSigningCertificate()).startsWith("-----BEGIN CERTIFICATE-----");
                assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isNotBlank();
                assertThat(configmap.getActiveMetadataSigningCredentialId()).isNotBlank();
            }
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch in list credentials")
    public void testCustomKeyAndCertificateMismatchInCredentialsList() {
        invalidIdentityProvider.reset(spidProviderConfigRepository.findByProviderId(spidProviderId));

        // CORRECT CREDENTIAL
        invalidIdentityProvider.signingListCredentialsInvalid.add(new SigningCredential(
            null,
            invalidIdentityProvider.signingIdpSigningKeyByProviderId,
            invalidIdentityProvider.signingIdpSigningCertificateByProviderId));

        // INCORRECT CREDENTIAL
        invalidIdentityProvider.signingListCredentialsInvalid.add(new SigningCredential(
            invalidIdentityProvider.signingActiveSigningCredentialIdInvalid,
            // PRIVATE KEY BY LIST CREDENTIALS
            invalidIdentityProvider.signingListCredentialsByProviderId.get(0).getSigningKey(),
            // CERTIFICATE BY STANDALONE
            invalidIdentityProvider.signingIdpSigningCertificateByProviderId));

        invalidIdentityProvider.configsInvalid.setSigningCredentials(invalidIdentityProvider.signingListCredentialsInvalid);
        invalidIdentityProvider.configsInvalid.setActiveAuthRequestSigningCredentialId(invalidIdentityProvider.signingActiveSigningCredentialIdInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            invalidIdentityProvider.createInvalidSpidIdentityProvider(invalidIdentityProvider.configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch")
    public void testKeyAndCertificateMismatchInCredentialsList() {
        invalidIdentityProvider.reset(spidProviderConfigRepository.findByProviderId(spidProviderId));

        invalidIdentityProvider.signingListCredentialsInvalid.add(new SigningCredential(
            null,
            invalidIdentityProvider.signingListCredentialsByProviderId.get(0).getSigningKey(),
            invalidIdentityProvider.signingIdpSigningCertificateByProviderId));

        invalidIdentityProvider.configsInvalid.setSigningCredentials(invalidIdentityProvider.signingListCredentialsInvalid);

        assertThrows(IllegalArgumentException.class, () -> {
            invalidIdentityProvider.createInvalidSpidIdentityProvider(invalidIdentityProvider.configsInvalid);
        });
    }

    @Test
    @DisplayName("Errata Configurazione - Key and Certificate mismatch in standalone credential")
    public void testKeyAndCertificateMismatchInStandaloneCredential() {
        invalidIdentityProvider.reset(spidProviderConfigRepository.findByProviderId(spidProviderId));

        invalidIdentityProvider.configsInvalid.setSigningCertificate(invalidIdentityProvider.signingIdpSigningCertificateByProviderId);
        invalidIdentityProvider.configsInvalid.setSigningKey(invalidIdentityProvider.signingListCredentialsByProviderId.get(0).getSigningKey());

        assertThrows(IllegalArgumentException.class, () -> {
            invalidIdentityProvider.createInvalidSpidIdentityProvider(invalidIdentityProvider.configsInvalid);
        });
    }
}
