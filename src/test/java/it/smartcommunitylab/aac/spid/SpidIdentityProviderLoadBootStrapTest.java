package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.provider.AbstractIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.FirstIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SecondIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for validating the SPID configuration loading and setup.
 * It ensures that Relying Party Registrations, Identity Providers, X.509 Certificates,
 * and SPID attributes are correctly initialized in the Spring context before
 * actual SAML authentications are performed.
 */
@SpringBootTest
@AutoConfigureMockMvc
// Loads the base profile ("test") and then applies SPID overrides ("test-spid")
@ActiveProfiles({"test", "test-spid"})
// Add @Transactional to clean up the DB automatically between @Test methods within this class
@Transactional
public class SpidIdentityProviderLoadBootStrapTest extends BaseSpidTest {

    @Autowired
    private BootstrapConfig config;

    @Autowired
    @Qualifier("spidProviderConfigRepository")
    private ProviderConfigRepository<SpidIdentityProviderConfig> spidProviderConfigRepository;

    protected FirstIdentityProvider firstIdentityProvider = new FirstIdentityProvider();
    protected SecondIdentityProvider secondIdentityProvider = new SecondIdentityProvider();

    @BeforeEach
    public void setupConfiguration() {
        initMockMvc();

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                assertThat(realm.getIdentityProviders()).isNotNull();

                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                assertThat(idps).isNotNull();
                assertThat(idps).hasSizeGreaterThanOrEqualTo(2);

                ConfigurableIdentityProvider idp1 = idps.get(0);
                assertThat(idp1.getProvider()).isNotNull();

                firstIdentityProvider.signingIdpProvider = idp1.getProvider();
                firstIdentityProvider.signingIdpMetadataUrl = BASE_URL + METADATA_PATH + AbstractIdentityProvider.encodeRegistrationId(firstIdentityProvider.signingIdpProvider);
                firstIdentityProvider.signingIdpSsoUrl = BASE_URL + SSO_PATH + AbstractIdentityProvider.encodeRegistrationId(firstIdentityProvider.signingIdpProvider);
                firstIdentityProvider.signingIdpEntityId = BASE_URL + METADATA_PATH + AbstractIdentityProvider.encodeRegistrationId(firstIdentityProvider.signingIdpProvider);

                assertThat(idp1.getAuthority()).isNotNull();
                assertThat(idp1.getRealm()).isNotNull();
                assertThat(idp1.getConfiguration()).isNotNull();

                firstIdentityProvider.signingIdpAuthority = idp1.getAuthority();
                firstIdentityProvider.signingIdpProvider = idp1.getProvider();
                firstIdentityProvider.signingIdpSloUrl = BASE_URL + SLO_PATH + AbstractIdentityProvider.encodeRegistrationId(firstIdentityProvider.signingIdpProvider);

                SpidIdentityProviderConfigMap configmap = new SpidIdentityProviderConfigMap();
                configmap.setConfiguration(idp1.getConfiguration());

                assertThat(configmap.getSpidAttributes()).isNotNull();
                assertThat(configmap.getSigningCredentials()).isNotNull();
                assertThat(configmap.getSigningCredentials().get(1).getSigningKey()).isNotNull();
                assertThat(configmap.getSigningCredentials().get(1).getSigningCertificate()).isNotNull();
                assertThat(configmap.getActiveAuthRequestSigningCredentialId()).isNotNull();

                firstIdentityProvider.signingSetSpidAttributes = configmap.getSpidAttributes();
                firstIdentityProvider.signingCredentials = configmap.getSigningCredentials();
                firstIdentityProvider.signingActiveSigningCredentialId = configmap.getActiveAuthRequestSigningCredentialId();
                firstIdentityProvider.signingIdpSigningKey = configmap.getSigningCredentials().get(1).getSigningKey();
                firstIdentityProvider.signingIdpSigningCertificate = configmap.getSigningCredentials().get(1).getSigningCertificate();

                ConfigurableIdentityProvider idp2 = idps.get(1);
                assertThat(idp2.getProvider()).isNotNull();

                secondIdentityProvider.signingIdpProvider = idp2.getProvider();
                secondIdentityProvider.signingIdpMetadataUrl = BASE_URL + METADATA_PATH + AbstractIdentityProvider.encodeRegistrationId(secondIdentityProvider.signingIdpProvider);
                secondIdentityProvider.signingIdpSsoUrl = BASE_URL + SSO_PATH + AbstractIdentityProvider.encodeRegistrationId(secondIdentityProvider.signingIdpProvider);
                secondIdentityProvider.signingIdpEntityId = BASE_URL + METADATA_PATH + AbstractIdentityProvider.encodeRegistrationId(secondIdentityProvider.signingIdpProvider);

                SpidIdentityProviderConfigMap configmapSecond = new SpidIdentityProviderConfigMap();
                configmapSecond.setConfiguration(idp2.getConfiguration());

                assertThat(configmapSecond.getSpidAttributes()).isNotNull();
                assertThat(configmapSecond.getUseAssertionConsumerServiceUrl()).isNotNull();
                assertThat(configmapSecond.getAttributeConsumingServiceIndex()).isNotNull();

                secondIdentityProvider.signingSetSpidAttributes = configmapSecond.getSpidAttributes();
                secondIdentityProvider.signingUseAssertionConsumerServiceUrl = configmapSecond.getUseAssertionConsumerServiceUrl();
                secondIdentityProvider.signingAttributeConsumingServiceIndex = configmapSecond.getAttributeConsumingServiceIndex();
            }
        });
    }

    @Test
    @DisplayName("Verifica configurazione e caricamento del Primo Provider (spid-poste)")
    public void testFirstProviderConfiguration() {
        SpidIdentityProviderConfig configFirst = spidProviderConfigRepository.findByProviderId(firstIdentityProvider.signingIdpProvider);
        assertThat(configFirst).isNotNull();

        assertThat(configFirst.getProvider()).isEqualTo(firstIdentityProvider.signingIdpProvider);
        assertThat(configFirst.getAuthority()).isEqualTo(firstIdentityProvider.signingIdpAuthority);
        assertThat(configFirst.getConfigMap().getSpidAttributes()).isEqualTo(firstIdentityProvider.signingSetSpidAttributes);
        assertThat(configFirst.getConfigMap().getActiveAuthRequestSigningCredentialId()).isEqualTo(firstIdentityProvider.signingActiveSigningCredentialId);

        assertThat(configFirst.getProvider()).isEqualTo("d8b5a341-b1e9-46c5-8406-932b79313204");
        assertThat(configFirst.getName()).isEqualTo("spid-poste");
        assertThat(configFirst.getRealm()).isEqualTo("spid-test");
        assertThat(configFirst.getConfigMap().getOrganizationName()).isEqualTo("TN Provincia Test");
        assertThat(configFirst.getConfigMap().getOrganizationDisplayName()).isEqualTo("Provincia Test");
        assertThat(configFirst.getConfigMap().getAuthnContext()).isEqualTo(SpidAuthnContext.SPID_L2);

        assertThat(configFirst.getConfigMap().getSpidAttributes())
            .hasSize(5)
            .containsExactlyInAnyOrder(
                SpidAttribute.NAME,
                SpidAttribute.EMAIL,
                SpidAttribute.FISCAL_NUMBER,
                SpidAttribute.SPID_CODE,
                SpidAttribute.FAMILY_NAME
            );

        List<SigningCredential> credentials = configFirst.getConfigMap().getSigningCredentials();

        assertThat(credentials).usingRecursiveComparison().isEqualTo(firstIdentityProvider.signingCredentials);
        assertThat(credentials).hasSize(3);
        assertThat(credentials)
            .extracting("credentialId")
            .contains("active_request", "active_metadata");

        String expectedPrivateKey = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCTvUdaYROlgbHL
            vc2N8ieWbv17v2/0Hv12lw4+m/200PXZVW+15uFn0E5F+iUXfb8I0u9hDRKIg1gT
            MjK20GOu1yYgwbmhnoVpvuotktN9NAA9kLpJliPADBnx6cHvFgEP8aGISKg9xopm
            5FSGevQr1bKwAiOgncQ06GNbO9kNIo63td7w4oBq1wTUodOTR6/3tkYzHZnlsZuB
            Lzhx8RCMUgBkQc/tK0vWUCIC4MU83vA+oD3Q6ogcT642YfoX3emY+wVwcG7pD8Sh
            zeV9pDbmsy/yJ14blA6bS0cPT3cHa5t8EG40CCkOGZWDEfOGOOVVRudgub0b6tL0
            5cMrjPAlAgMBAAECggEAMu+IITXk8yPy656ltvGtCmV7yWVoMM9abb+IrrdTUjrU
            +DhHinxubD9aLTAgB6hX66/lzh0WrbAy9nNRHsxcAdS1lYeU+47OynWDAXFkrv71
            skQqpeI4sya47zm7njWE6j1Rhs4eCyZfYzgHmFHdyxsjPyGNrPuXwPH6B2Nr4uXo
            ClQhEZJCs9veijYKi4eKCqXeeJV+xUtpCU8Aay460T518l5/OxiGjBQuoSeEkcJh
            RvJV6qgSTMTpS4ZOhuFMGFs37TlTUdRt9upCTLG9jW+QqKVr1Gi0Ge0EVFvgd2KE
            S78qW+G/e+/VF77E/GJnvBi5G2GIAnW1iF+3MVJXxQKBgQDImz04sGU4LHRjLx3+
            /o/opRjlS0gMBkOh5sw/bJ+GX6UfK9vTG0sUxVSX5nBXVk5B+0o4NKGEiaHs+7YN
            ttG5X62nr1RR25Jlj4h+XX3sl65nV536zK5iXrzIZwhKdrPwy/KAmkVVs32527/v
            IWJ9LO20QhpSJk/rwb/dsZyglwKBgQC8iOWa3mQand4W2pbSd5OJ+vS+Cz2atkQP
            0wNil1HymOJpXOAyZHQ7xdKkR4len+9Uo7Z/ds5PqqltEMFb6K4pVtMRUUqyL9s1
            UQ8hRuuQe/xjFcKcLNJtxHggZjjdyQcGeARjpZDMvabNaqlUJ6mp4H0f5yH9rqYb
            x/bHs2jQowKBgDdWCzc+AU3ThW9uqdmTIuNL12g4sfEPMUzRu3mrXv3UGFpW4NaE
            6tsZ69HS8R5GYmP1C24hpoRG6vHSJU/3JDb8W4yr4piJ9wIo67/fzkKbPLKpCp0K
            JPhhpbWqJjFUOSKtP4GWDJYtYvsH0RQHo4FgCVn1+gi7JMSlt2VR/yCVAoGAUyrY
            jl64Lf2h8NbwXVueW+m2ePcgb9UjZQ+imKVD4w6KIgx0YgZqN8vmRc3AzVskCmRF
            pyjTjdUs9A3GHjMgUOAaL7N6jewKeRSO5hEQ7SWkilkZBifHk+BtVW2CfGOnk7Tx
            yrtIiujGYitBEvyEHYoH6EDff7bBU9P8CtAb1p0CgYEAv14JHufws2JfN0TTG0xL
            NQxYE7wp7ElPp9JU1qf1A088RTdhG6uCN+x35wwEY1Hl7qOQuZyk/7tOiWpPhGzG
            RTj0E50MJuOv/RKQuyBfsv5iBs5Tp3OBJx4LCqlQzIO+gJc+b/ud4rIAlMfXkJfe
            ztyRCKwg+qh/UH+8YD211QM=
            -----END PRIVATE KEY-----""";

        String expectedCertificate = """
            -----BEGIN CERTIFICATE-----
            MIIDHTCCAgWgAwIBAgIUcDv0dT2ndvYQ0zy8SZeknHYSy34wDQYJKoZIhvcNAQEL
            BQAwMDELMAkGA1UEBhMCSVQxDTALBgNVBAoMBFRlc3QxEjAQBgNVBAMMCWxvY2Fs
            aG9zdDAeFw0yNTEwMTAwODE1NTRaFw0yNTExMDkwODE1NTRaMDAxCzAJBgNVBAYT
            AklUMQ0wCwYDVQQKDARUZXN0MRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqG
            SIb3DQEBAQUAA4IBDwAwggEKAoIBAQCTvUdaYROlgbHLvc2N8ieWbv17v2/0Hv12
            lw4+m/200PXZVW+15uFn0E5F+iUXfb8I0u9hDRKIg1gTMjK20GOu1yYgwbmhnoVp
            vuotktN9NAA9kLpJliPADBnx6cHvFgEP8aGISKg9xopm5FSGevQr1bKwAiOgncQ0
            6GNbO9kNIo63td7w4oBq1wTUodOTR6/3tkYzHZnlsZuBLzhx8RCMUgBkQc/tK0vW
            UCIC4MU83vA+oD3Q6ogcT642YfoX3emY+wVwcG7pD8ShzeV9pDbmsy/yJ14blA6b
            S0cPT3cHa5t8EG40CCkOGZWDEfOGOOVVRudgub0b6tL05cMrjPAlAgMBAAGjLzAt
            MAwGA1UdEwQFMAMBAf8wHQYDVR0OBBYEFPBfVAuG0+aridbj5WQ+SqdGBQQ5MA0G
            CSqGSIb3DQEBCwUAA4IBAQAChPC5e9ie5AUS9H1/b6e7cC0eOtJelJnJQyMrLL9x
            RIAgXaZiunjf+1nPzaho/HPVapvYax+xwb3q4glTOu01yfJZGsBNxIs9HIOKQLhp
            9PIOzqQiP/g/APUUJqRPyLpVcdN5E8qKy/mWdB4JZKY84tdIZJq3EOs7VAq+hb2s
            ZoBfWxF/9HTr6vuQUr8agmY/zj+DU5R39Uk621apfHzZVrv9ft3wM2KhEm1aLhOm
            mQ8mySuKEEjSxwfAKaCqxe+/LVV2QRMhUr8AOZx184SF9Ff2txiMX56xeW0smFLp
            YSo82R54IbqKADire3br5/rjZ3evYuQrLYb+0bKbSEN4
            -----END CERTIFICATE-----""";

        assertThat(credentials.get(0).getSigningKey().trim()).isEqualTo(expectedPrivateKey.trim());
        assertThat(credentials.get(0).getSigningCertificate().trim()).isEqualTo(expectedCertificate.trim());
    }

    @Test
    @DisplayName("Verifica configurazione e caricamento del Secondo Provider (spid-lepida)")
    public void testSecondProviderConfiguration() {
        SpidIdentityProviderConfig configSecond = spidProviderConfigRepository.findByProviderId(secondIdentityProvider.signingIdpProvider);
        assertThat(configSecond).isNotNull();

        assertThat(configSecond.getProvider()).isEqualTo(secondIdentityProvider.signingIdpProvider);
        assertThat(configSecond.getConfigMap().getSpidAttributes()).isEqualTo(secondIdentityProvider.signingSetSpidAttributes);
        assertThat(configSecond.getConfigMap().getUseAssertionConsumerServiceUrl()).isEqualTo(secondIdentityProvider.signingUseAssertionConsumerServiceUrl);
        assertThat(configSecond.getConfigMap().getAttributeConsumingServiceIndex()).isEqualTo(secondIdentityProvider.signingAttributeConsumingServiceIndex);

        assertThat(configSecond.getProvider()).isEqualTo("34a83a02-45f5-4112-b783-4085535c617a");
        assertThat(configSecond.getName()).isEqualTo("spid-lepida");
        assertThat(configSecond.getRealm()).isEqualTo("spid-test");
        assertThat(configSecond.getConfigMap().getUseAssertionConsumerServiceUrl()).isTrue();
        assertThat(configSecond.getConfigMap().getAttributeConsumingServiceIndex()).isEqualTo(3);

        assertThat(configSecond.getConfigMap().getSpidAttributes())
            .hasSize(6)
            .containsExactlyInAnyOrder(
                SpidAttribute.NAME,
                SpidAttribute.EMAIL,
                SpidAttribute.FAMILY_NAME,
                SpidAttribute.ID_CARD,
                SpidAttribute.IVA_CODE,
                SpidAttribute.MOBILE_PHONE
            );

        List<SigningCredential> credentials = configSecond.getConfigMap().getSigningCredentials();
        assertThat(credentials).hasSize(1);

        String expectedPrivateKeySecond = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCTvUdaYROlgbHL
            vc2N8ieWbv17v2/0Hv12lw4+m/200PXZVW+15uFn0E5F+iUXfb8I0u9hDRKIg1gT
            MjK20GOu1yYgwbmhnoVpvuotktN9NAA9kLpJliPADBnx6cHvFgEP8aGISKg9xopm
            5FSGevQr1bKwAiOgncQ06GNbO9kNIo63td7w4oBq1wTUodOTR6/3tkYzHZnlsZuB
            Lzhx8RCMUgBkQc/tK0vWUCIC4MU83vA+oD3Q6ogcT642YfoX3emY+wVwcG7pD8Sh
            zeV9pDbmsy/yJ14blA6bS0cPT3cHa5t8EG40CCkOGZWDEfOGOOVVRudgub0b6tL0
            5cMrjPAlAgMBAAECggEAMu+IITXk8yPy656ltvGtCmV7yWVoMM9abb+IrrdTUjrU
            +DhHinxubD9aLTAgB6hX66/lzh0WrbAy9nNRHsxcAdS1lYeU+47OynWDAXFkrv71
            skQqpeI4sya47zm7njWE6j1Rhs4eCyZfYzgHmFHdyxsjPyGNrPuXwPH6B2Nr4uXo
            ClQhEZJCs9veijYKi4eKCqXeeJV+xUtpCU8Aay460T518l5/OxiGjBQuoSeEkcJh
            RvJV6qgSTMTpS4ZOhuFMGFs37TlTUdRt9upCTLG9jW+QqKVr1Gi0Ge0EVFvgd2KE
            S78qW+G/e+/VF77E/GJnvBi5G2GIAnW1iF+3MVJXxQKBgQDImz04sGU4LHRjLx3+
            /o/opRjlS0gMBkOh5sw/bJ+GX6UfK9vTG0sUxVSX5nBXVk5B+0o4NKGEiaHs+7YN
            ttG5X62nr1RR25Jlj4h+XX3sl65nV536zK5iXrzIZwhKdrPwy/KAmkVVs32527/v
            IWJ9LO20QhpSJk/rwb/dsZyglwKBgQC8iOWa3mQand4W2pbSd5OJ+vS+Cz2atkQP
            0wNil1HymOJpXOAyZHQ7xdKkR4len+9Uo7Z/ds5PqqltEMFb6K4pVtMRUUqyL9s1
            UQ8hRuuQe/xjFcKcLNJtxHggZjjdyQcGeARjpZDMvabNaqlUJ6mp4H0f5yH9rqYb
            x/bHs2jQowKBgDdWCzc+AU3ThW9uqdmTIuNL12g4sfEPMUzRu3mrXv3UGFpW4NaE
            6tsZ69HS8R5GYmP1C24hpoRG6vHSJU/3JDb8W4yr4piJ9wIo67/fzkKbPLKpCp0K
            JPhhpbWqJjFUOSKtP4GWDJYtYvsH0RQHo4FgCVn1+gi7JMSlt2VR/yCVAoGAUyrY
            jl64Lf2h8NbwXVueW+m2ePcgb9UjZQ+imKVD4w6KIgx0YgZqN8vmRc3AzVskCmRF
            pyjTjdUs9A3GHjMgUOAaL7N6jewKeRSO5hEQ7SWkilkZBifHk+BtVW2CfGOnk7Tx
            yrtIiujGYitBEvyEHYoH6EDff7bBU9P8CtAb1p0CgYEAv14JHufws2JfN0TTG0xL
            NQxYE7wp7ElPp9JU1qf1A088RTdhG6uCN+x35wwEY1Hl7qOQuZyk/7tOiWpPhGzG
            RTj0E50MJuOv/RKQuyBfsv5iBs5Tp3OBJx4LCqlQzIO+gJc+b/ud4rIAlMfXkJfe
            ztyRCKwg+qh/UH+8YD211QM=
            -----END PRIVATE KEY-----""";

        String expectedCertificateSecond = """
            -----BEGIN CERTIFICATE-----
            MIIDHTCCAgWgAwIBAgIUcDv0dT2ndvYQ0zy8SZeknHYSy34wDQYJKoZIhvcNAQEL
            BQAwMDELMAkGA1UEBhMCSVQxDTALBgNVBAoMBFRlc3QxEjAQBgNVBAMMCWxvY2Fs
            aG9zdDAeFw0yNTEwMTAwODE1NTRaFw0yNTExMDkwODE1NTRaMDAxCzAJBgNVBAYT
            AklUMQ0wCwYDVQQKDARUZXN0MRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqG
            SIb3DQEBAQUAA4IBDwAwggEKAoIBAQCTvUdaYROlgbHLvc2N8ieWbv17v2/0Hv12
            lw4+m/200PXZVW+15uFn0E5F+iUXfb8I0u9hDRKIg1gTMjK20GOu1yYgwbmhnoVp
            vuotktN9NAA9kLpJliPADBnx6cHvFgEP8aGISKg9xopm5FSGevQr1bKwAiOgncQ0
            6GNbO9kNIo63td7w4oBq1wTUodOTR6/3tkYzHZnlsZuBLzhx8RCMUgBkQc/tK0vW
            UCIC4MU83vA+oD3Q6ogcT642YfoX3emY+wVwcG7pD8ShzeV9pDbmsy/yJ14blA6b
            S0cPT3cHa5t8EG40CCkOGZWDEfOGOOVVRudgub0b6tL05cMrjPAlAgMBAAGjLzAt
            MAwGA1UdEwQFMAMBAf8wHQYDVR0OBBYEFPBfVAuG0+aridbj5WQ+SqdGBQQ5MA0G
            CSqGSIb3DQEBCwUAA4IBAQAChPC5e9ie5AUS9H1/b6e7cC0eOtJelJnJQyMrLL9x
            RIAgXaZiunjf+1nPzaho/HPVapvYax+xwb3q4glTOu01yfJZGsBNxIs9HIOKQLhp
            9PIOzqQiP/g/APUUJqRPyLpVcdN5E8qKy/mWdB4JZKY84tdIZJq3EOs7VAq+hb2s
            ZoBfWxF/9HTr6vuQUr8agmY/zj+DU5R39Uk621apfHzZVrv9ft3wM2KhEm1aLhOm
            mQ8mySuKEEjSxwfAKaCqxe+/LVV2QRMhUr8AOZx184SF9Ff2txiMX56xeW0smFLp
            YSo82R54IbqKADire3br5/rjZ3evYuQrLYb+0bKbSEN4
            -----END CERTIFICATE-----""";

        assertThat(credentials.get(0).getSigningKey().trim()).isEqualTo(expectedPrivateKeySecond.trim());
        assertThat(credentials.get(0).getSigningCertificate().trim()).isEqualTo(expectedCertificateSecond.trim());
    }
}
