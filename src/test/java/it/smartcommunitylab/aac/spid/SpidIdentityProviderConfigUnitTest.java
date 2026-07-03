package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAttributeConsumingService;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure unit tests for the SpidIdentityProviderConfig class.
 * This class DOES NOT load the Spring Boot context (no @SpringBootTest).
 * It exclusively tests from-scratch instantiation, static utilities, and
 * controlled failures (expected exceptions) within the internal business logic.
 */
public class SpidIdentityProviderConfigUnitTest {

    @Test
    @DisplayName("Verifica codifica, decodifica ed estrazione del Registration ID")
    public void testRegistrationIdUtilities() {
        String rawId = "my-provider|idp-poste";

        String encoded = SpidIdentityProviderConfig.encodeRegistrationId(rawId);
        assertThat(encoded).isNotBlank().isNotEqualTo(rawId);

        String decoded = SpidIdentityProviderConfig.decodeRegistrationId(encoded);
        assertThat(decoded).isEqualTo(rawId);

        assertThat(SpidIdentityProviderConfig.getProviderId(decoded)).isEqualTo("my-provider");
        assertThat(SpidIdentityProviderConfig.getProviderId("simple-provider")).isEqualTo("simple-provider");
    }

    @Test
    @DisplayName("Creazione da zero (from scratch) con valori base")
    public void testInstantiationFromScratch() {
        SpidIdentityProviderConfig config = new SpidIdentityProviderConfig("my-spid-provider", "my-realm");
        config.setBaseUrl("http://localhost:8080");

        assertThat(config.getProvider()).isEqualTo("my-spid-provider");
        assertThat(config.getRealm()).isEqualTo("my-realm");
        assertThat(config.getAuthority()).isEqualTo("spid");

        assertThat(config.getIdentityProviders()).isEmpty();
    }

    @Test
    @DisplayName("Rottura Controllata: Nessun IDP upstream definito")
    public void testExceptionOnEmptyUpstreamIdps() {
        SpidIdentityProviderConfig config = new SpidIdentityProviderConfig("my-provider", "my-realm");

        assertThrows(IllegalArgumentException.class, () -> config.setIdentityProviders(Collections.emptyList()));
    }

    @Test
    @DisplayName("Rottura Controllata: Metadata XML malformato o assente")
    public void testExceptionOnInvalidMetadataXml() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setProvider("test-provider");

        SpidIdentityProviderConfigMap configMap = new SpidIdentityProviderConfigMap();
        configMap.setMetadataXML("<xml-break><break>");

        assertThrows(IllegalArgumentException.class, () -> new SpidIdentityProviderConfig(cp, new IdentityProviderSettingsMap(), configMap));
    }

    @Test
    @DisplayName("Rottura Controllata: URI non valido per estrazione IdP Key")
    public void testExceptionOnInvalidUriSyntax() {
        SpidIdentityProviderConfig config = new SpidIdentityProviderConfig("my-provider", "my-realm");

        String badUri = "http://bad url with spaces.com";

        assertThrows(URISyntaxException.class, () -> config.evalIdpKeyIdentifier(badUri));
    }

    @Test
    @DisplayName("Rottura Controllata: Estrazione Registration ID vuoto/nullo")
    public void testExceptionOnEmptyRegistrationId() {
        assertThrows(IllegalArgumentException.class, () -> SpidIdentityProviderConfig.getProviderId(""));
    }

    /* =========================================================================
     * CREDENTIAL TESTS (CONTROLLED FAILURES)
     * ========================================================================= */

    @Test
    @DisplayName("Errata Configurazione - Credenziali Vuote (Empty Credentials)")
    public void testEmptyCredentials() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setProvider("test-provider");

        SpidIdentityProviderConfigMap configMap = new SpidIdentityProviderConfigMap();
        configMap.setEntityId("http://my-entity-id");

        assertThrows(IllegalArgumentException.class, () -> new SpidIdentityProviderConfig(cp, new IdentityProviderSettingsMap(), configMap));
    }

    @Test
    @DisplayName("Errata Configurazione - Chiave mancante (Standalone credential)")
    public void testKeyStandaloneCredential() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setProvider("test-provider");

        SpidIdentityProviderConfigMap configMap = new SpidIdentityProviderConfigMap();
        configMap.setEntityId("http://my-entity-id");
        configMap.setSigningCertificate("-----BEGIN CERTIFICATE-----\nCERTIFICATE\n-----END CERTIFICATE-----");

        assertThrows(IllegalArgumentException.class, () -> new SpidIdentityProviderConfig(cp, new IdentityProviderSettingsMap(), configMap));
    }

    @Test
    @DisplayName("Errata Configurazione - Chiave mancante nella lista credenziali")
    public void testKeyListCredentials() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setProvider("test-provider");

        SpidIdentityProviderConfigMap configMap = new SpidIdentityProviderConfigMap();
        configMap.setEntityId("http://my-entity-id");

        SigningCredential badCredential = new SigningCredential("active_req", null, "-----BEGIN CERTIFICATE-----\nCERTIFICATE\n-----END CERTIFICATE-----");
        configMap.setSigningCredentials(Collections.singletonList(badCredential));
        configMap.setActiveAuthRequestSigningCredentialId("active_req");

        assertThrows(IllegalArgumentException.class, () -> new SpidIdentityProviderConfig(cp, new IdentityProviderSettingsMap(), configMap));
    }

    @Test
    @DisplayName("Errata Configurazione - ID Credenziale non corrispondente (Not Found Id Matching)")
    public void testNotFoundIdMatching() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setProvider("test-provider");

        SpidIdentityProviderConfigMap configMap = new SpidIdentityProviderConfigMap();
        configMap.setEntityId("http://my-entity-id");

        SigningCredential credential = new SigningCredential("id_valid", "KEY", "CERT");
        configMap.setSigningCredentials(Collections.singletonList(credential));

        configMap.setActiveAuthRequestSigningCredentialId("id_lost");

        assertThrows(IllegalArgumentException.class, () -> new SpidIdentityProviderConfig(cp, new IdentityProviderSettingsMap(), configMap));
    }

    @Test
    @DisplayName("Errata Configurazione - Certificati Duplicati nella METADATA_EXPOSURE")
    public void testDuplicateCertificates() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setProvider("test-provider");

        SpidIdentityProviderConfigMap configMap = new SpidIdentityProviderConfigMap();
        configMap.setEntityId("http://my-entity-id");

        String sameCert = "-----BEGIN CERTIFICATE-----\nSAME_CERTIFICATE\n-----END CERTIFICATE-----";
        SigningCredential cred1 = new SigningCredential("id1", "KEY1", sameCert);
        SigningCredential cred2 = new SigningCredential("id2", "KEY2", sameCert);

        configMap.setSigningCredentials(Arrays.asList(cred1, cred2));
        configMap.setActiveAuthRequestSigningCredentialId("id1");

        assertThrows(IllegalArgumentException.class, () -> new SpidIdentityProviderConfig(cp, new IdentityProviderSettingsMap(), configMap));
    }

    @Test
    @DisplayName("Parsing di un Metadato XML Valido (XPath e AttributeConsumingService)")
    void testValidMetadataXmlParsing() {
        String validCertBase64 =
            "MIIDHTCCAgWgAwIBAgIUcDv0dT2ndvYQ0zy8SZeknHYSy34wDQYJKoZIhvcNAQEL" +
            "BQAwMDELMAkGA1UEBhMCSVQxDTALBgNVBAoMBFRlc3QxEjAQBgNVBAMMCWxvY2Fs" +
            "aG9zdDAeFw0yNTEwMTAwODE1NTRaFw0yNTExMDkwODE1NTRaMDAxCzAJBgNVBAYT" +
            "AklUMQ0wCwYDVQQKDARUZXN0MRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqG" +
            "SIb3DQEBAQUAA4IBDwAwggEKAoIBAQCTvUdaYROlgbHLvc2N8ieWbv17v2/0Hv12" +
            "lw4+m/200PXZVW+15uFn0E5F+iUXfb8I0u9hDRKIg1gTMjK20GOu1yYgwbmhnoVp" +
            "vuotktN9NAA9kLpJliPADBnx6cHvFgEP8aGISKg9xopm5FSGevQr1bKwAiOgncQ0" +
            "6GNbO9kNIo63td7w4oBq1wTUodOTR6/3tkYzHZnlsZuBLzhx8RCMUgBkQc/tK0vW" +
            "UCIC4MU83vA+oD3Q6ogcT642YfoX3emY+wVwcG7pD8ShzeV9pDbmsy/yJ14blA6b" +
            "S0cPT3cHa5t8EG40CCkOGZWDEfOGOOVVRudgub0b6tL05cMrjPAlAgMBAAGjLzAt" +
            "MAwGA1UdEwQFMAMBAf8wHQYDVR0OBBYEFPBfVAuG0+aridbj5WQ+SqdGBQQ5MA0G" +
            "CSqGSIb3DQEBCwUAA4IBAQAChPC5e9ie5AUS9H1/b6e7cC0eOtJelJnJQyMrLL9x" +
            "RIAgXaZiunjf+1nPzaho/HPVapvYax+xwb3q4glTOu01yfJZGsBNxIs9HIOKQLhp" +
            "9PIOzqQiP/g/APUUJqRPyLpVcdN5E8qKy/mWdB4JZKY84tdIZJq3EOs7VAq+hb2s" +
            "ZoBfWxF/9HTr6vuQUr8agmY/zj+DU5R39Uk621apfHzZVrv9ft3wM2KhEm1aLhOm" +
            "mQ8mySuKEEjSxwfAKaCqxe+/LVV2QRMhUr8AOZx184SF9Ff2txiMX56xeW0smFLp" +
            "YSo82R54IbqKADire3br5/rjZ3evYuQrLYb+0bKbSEN4";

        String validPemCert = "-----BEGIN CERTIFICATE-----\n" + validCertBase64 + "\n-----END CERTIFICATE-----";

        String validPemKey = """
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

        // XML string containing the SAME certificate inside the <ds:X509Certificate> tag
        String validXml = """
            <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" xmlns:spid="https://spid.gov.it/saml-extensions" entityID="http://test-entity-id">
                <md:Organization>
                    <md:OrganizationName>OrgTest</md:OrganizationName>
                    <md:OrganizationDisplayName>Org Display Test</md:OrganizationDisplayName>
                    <md:OrganizationURL>https://org.test</md:OrganizationURL>
                </md:Organization>
                <md:ContactPerson>
                    <md:EmailAddress>test@test.com</md:EmailAddress>
                    <md:Extensions>
                        <spid:IPACode>IPA123</spid:IPACode>
                    </md:Extensions>
                </md:ContactPerson>
                <md:SPSSODescriptor>
                    <md:KeyDescriptor use="signing">
                       <ds:KeyInfo>
                           <ds:X509Data>
                               <ds:X509Certificate>%s</ds:X509Certificate>
                           </ds:X509Data>
                       </ds:KeyInfo>
                    </md:KeyDescriptor>
                    <md:AttributeConsumingService index="0">
                        <md:ServiceName>Service Test</md:ServiceName>
                        <md:RequestedAttribute Name="name"/>
                        <md:RequestedAttribute Name="fiscalNumber"/>
                    </md:AttributeConsumingService>
                </md:SPSSODescriptor>
            </md:EntityDescriptor>
            """.formatted(validCertBase64);

        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setProvider("test-provider");

        SpidIdentityProviderConfigMap configMap = new SpidIdentityProviderConfigMap();
        configMap.setMetadataXML(validXml);

        // Provide the valid cryptographic key pair
        SigningCredential cred = new SigningCredential("id1", validPemKey, validPemCert);
        configMap.setSigningCredentials(Collections.singletonList(cred));
        configMap.setActiveAuthRequestSigningCredentialId("id1");

        configMap.setAttributeConsumingServiceIndex(0);

        // Instantiate the configuration (It will not crash anymore!)
        SpidIdentityProviderConfig config = new SpidIdentityProviderConfig(cp, new IdentityProviderSettingsMap(), configMap);

        // Verify the XPath parsing
        assertThat(config.getEntityId()).isEqualTo("http://test-entity-id");
        assertThat(config.getOrganizationName()).isEqualTo("OrgTest");
        assertThat(config.getOrganizationDisplayName()).isEqualTo("Org Display Test");
        assertThat(config.getOrganizationUrl()).isEqualTo("https://org.test");
        assertThat(config.getContactPersonEmailAddress()).isEqualTo("test@test.com");
        assertThat(config.getContactPersonIPACode()).isEqualTo("IPA123");

        // Verify the AttributeConsumingService
        Set<SpidAttributeConsumingService> acsSet = config.getAttributeConsumingServices();
        assertThat(acsSet).hasSize(1);
        SpidAttributeConsumingService acs = acsSet.iterator().next();
        assertThat(acs.getIndex()).isEqualTo(0);
        assertThat(acs.getName()).isEqualTo("Service Test");
        assertThat(acs.getAttributes()).containsExactlyInAnyOrder(SpidAttribute.NAME, SpidAttribute.FISCAL_NUMBER);
    }
}
