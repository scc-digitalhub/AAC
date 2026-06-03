package it.smartcommunitylab.aac.spid;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidMetadataConfiguration;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for verifying the correct initialization of SPID Identity Providers from bootstrap configurations.
 * Ensures that identity provider configurations, metadata, configuration maps, and cryptographic credentials are accurately loaded and mapped.

 # COMMAND GENERATE CERTIFICATE AND PRIVATE KEY
 # openssl req -x509 -config credential.cnf -days 3650 -keyout private.key -out public.crt

 credential.cnf:
     [ req ]
     default_bits       = 2048
     distinguished_name = req_distinguished_name
     prompt             = no
     encrypt_key        = no
     x509_extensions    = v3_req

     [ req_distinguished_name ]
     C  = IT
     O  = Test
     CN = localhost

     [ v3_req ]
     basicConstraints = CA:FALSE
 */
@SpringBootTest
@ActiveProfiles({"test", "test-spid"})
public class CheckLoadBootStrapTest extends BaseSpidTest {

    private String providerDefaultId;
    private String providerCredentialsId;
    private String providerCustomId;
    private String providerOrganizationId;

    @BeforeEach
    public void setupConfiguration() {
        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                assertThat(idps).isNotNull();
                assertThat(idps).hasSizeGreaterThanOrEqualTo(4);

                providerDefaultId = idps.stream().filter(idp -> "spid-test-default".equals(idp.getName())).findFirst().orElseThrow().getProvider();
                providerCredentialsId = idps.stream().filter(idp -> "spid-test-credentials".equals(idp.getName())).findFirst().orElseThrow().getProvider();
                providerCustomId = idps.stream().filter(idp -> "spid-test-custom".equals(idp.getName())).findFirst().orElseThrow().getProvider();
                providerOrganizationId = idps.stream().filter(idp -> "spid-test-organization".equals(idp.getName())).findFirst().orElseThrow().getProvider();
            }
        });
    }

    @Test
    @DisplayName("Verifica Provider (spid-test-default)")
    public void testCheckLoadProviderDefault() {
        SpidIdentityProviderConfig configIdentityProvider = spidProviderConfigRepository.findByProviderId(providerDefaultId);
        SpidIdentityProviderConfigMap configmap = checkCommonBaseProperties(configIdentityProvider);

        assertThat(configmap.getSpidAttributes()).isNotNull();
        assertThat(configmap.getSpidAttributes()).hasSize(5);

        assertThat(configmap.getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCertificate()).isNotNull();

        assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isNull();
        assertThat(configmap.getActiveMetadataSigningCredentialId()).isNull();

        assertThat(configmap.getSigningCredentials()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Verifica Provider (spid-test-credentials)")
    public void testCheckLoadProviderCredentials() {
        SpidIdentityProviderConfig configIdentityProvider = spidProviderConfigRepository.findByProviderId(providerCredentialsId);
        SpidIdentityProviderConfigMap configmap = checkCommonBaseProperties(configIdentityProvider);

        assertThat(configmap.getSpidAttributes()).isNotNull();
        assertThat(configmap.getSpidAttributes()).hasSize(5);

        assertThat(configmap.getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCertificate()).isNotNull();

        assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isNotNull();
        assertThat(configmap.getActiveMetadataSigningCredentialId()).isNotNull();
        assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isEqualTo("active_request");
        assertThat(configmap.getActiveMetadataSigningCredentialId()).isEqualTo("active_metadata");

        assertThat(configmap.getSigningCredentials()).isNotNull();
        assertThat(configmap.getSigningCredentials()).hasSize(3);

        assertThat(configmap.getSigningCredentials().get(0).getCredentialId()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(0).getCredentialId()).isEqualTo("active_request");
        assertThat(configmap.getSigningCredentials().get(0).getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(0).getSigningCertificate()).isNotNull();

        assertThat(configmap.getSigningCredentials().get(1).getCredentialId()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(1).getCredentialId()).isEqualTo("active_metadata");
        assertThat(configmap.getSigningCredentials().get(1).getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(1).getSigningCertificate()).isNotNull();

        assertThat(configmap.getSigningCredentials().get(2).getCredentialId()).isNull();
        assertThat(configmap.getSigningCredentials().get(2).getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(2).getSigningCertificate()).isNotNull();
    }

    @Test
    @DisplayName("Verifica Provider (spid-test-Custom)")
    public void testCheckLoadProviderCustom() {
        SpidIdentityProviderConfig configIdentityProvider = spidProviderConfigRepository.findByProviderId(providerCustomId);
        SpidIdentityProviderConfigMap configmap = checkCommonBaseProperties(configIdentityProvider);

        assertThat(configmap.getUseAssertionConsumerServiceUrl()).isNotNull();
        assertThat(configmap.getAttributeConsumingServiceIndex()).isNotNull();
        assertThat(configmap.getUseAssertionConsumerServiceUrl()).isTrue();
        assertThat(configmap.getAttributeConsumingServiceIndex()).isEqualTo(3);

        assertThat(configmap.getSpidAttributes()).isNotNull();
        assertThat(configmap.getSpidAttributes()).hasSize(6);

        assertThat(configmap.getSigningKey()).isNull();
        assertThat(configmap.getSigningCertificate()).isNull();

        assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isNull();
        assertThat(configmap.getActiveMetadataSigningCredentialId()).isNull();

        assertThat(configmap.getSigningCredentials()).isNotNull();
        assertThat(configmap.getSigningCredentials()).hasSize(1);

        assertThat(configmap.getSigningCredentials().get(0).getCredentialId()).isNull();
        assertThat(configmap.getSigningCredentials().get(0).getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(0).getSigningCertificate()).isNotNull();
    }

    @Test
    @DisplayName("Verifica Provider (spid-test-organization)")
    public void testCheckLoadProviderOrganization() {
        SpidIdentityProviderConfig configIdentityProvider = spidProviderConfigRepository.findByProviderId(providerOrganizationId);
        SpidIdentityProviderConfigMap configmap = checkCommonBaseProperties(configIdentityProvider);

        assertThat(configmap.getSpidAttributes()).isNotNull();
        assertThat(configmap.getSpidAttributes()).hasSize(5);

        assertThat(configmap.getOrganizationName()).isNotNull();
        assertThat(configmap.getOrganizationDisplayName()).isNotNull();
        assertThat(configmap.getOrganizationUrl()).isNotNull();
        assertThat(configmap.getContactPersonEmailAddress()).isNotNull();
        assertThat(configmap.getContactPersonIPACode()).isNotNull();

        assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isNotNull();
        assertThat(configmap.getActiveMetadataSigningCredentialId()).isNotNull();
        assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isEqualTo("active_request");
        assertThat(configmap.getActiveMetadataSigningCredentialId()).isEqualTo("active_metadata");

        assertThat(configmap.getSigningCredentials()).isNotNull();
        assertThat(configmap.getSigningCredentials()).hasSize(2);

        assertThat(configmap.getSigningCredentials().get(0).getCredentialId()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(0).getCredentialId()).isEqualTo("active_request");
        assertThat(configmap.getSigningCredentials().get(0).getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(0).getSigningCertificate()).isNotNull();

        assertThat(configmap.getSigningCredentials().get(1).getCredentialId()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(1).getCredentialId()).isEqualTo("active_metadata");
        assertThat(configmap.getSigningCredentials().get(1).getSigningKey()).isNotNull();
        assertThat(configmap.getSigningCredentials().get(1).getSigningCertificate()).isNotNull();
    }

    private SpidIdentityProviderConfigMap checkCommonBaseProperties(SpidIdentityProviderConfig configIdentityProvider) {
        assertThat(configIdentityProvider).isNotNull();
        assertThat(configIdentityProvider.getProvider()).isNotNull();
        assertThat(configIdentityProvider.getAuthority()).isNotNull();
        assertThat(configIdentityProvider.getRealm()).isNotNull();

        assertThat(configIdentityProvider.getMetadataConfiguration()).isNotNull();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SpidMetadataConfiguration configmetadata = mapper.convertValue(
            configIdentityProvider.getMetadataConfiguration(),
            SpidMetadataConfiguration.class
        );
        assertThat(configmetadata).isNotNull();

        SpidIdentityProviderConfigMap configmap = configIdentityProvider.getConfigMap();
        assertThat(configmap).isNotNull();

        assertThat(configmap.getUsernameAttributeName()).isNotNull();
        assertThat(configmap.getSubAttributeName()).isNotNull();
        assertThat(configmap.getAuthnContext()).isNotNull();

        return configmap;
    }
}
