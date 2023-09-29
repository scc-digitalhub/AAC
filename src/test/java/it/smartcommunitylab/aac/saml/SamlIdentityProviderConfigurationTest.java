package it.smartcommunitylab.aac.saml;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.model.PersistenceMode;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.io.*;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This test is aimed at validating if a configuration map will yield a valid
 * and corresponding SamlProviderConfiguration.
 */
public class SamlIdentityProviderConfigurationTest {

    private static final int IDP_METADATA_MOCK_PORT = 51994;
    private static final String MOCK_METADATA_ENDPOINT = "metadata";
    private static final String IDP_METADATA_EXAMPLE_FP = "saml/saml-idp-metadata-example.xml";
    private static final String expProviderId = "ProviderTestId";
    private static final String expEntityID = String.format("http://localhost:8080/auth/saml/metadata/%s", expProviderId);

    // key pair generated with openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -keyout privateKey.key -out certificate.crt, using openssl version 1.1.1n
    private static final String expSigningKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC2cpj1zbjBsr4NtvLa+3j8Kb1mwcHlRNwMjPrZQ1fVAFNKuSRgGU87aMavSxFUmQMNy8PEEb+X2IxgNt36JDIHqK6IaEiqOsdzUDXwbHGroFIM5utl6mD4zVqbuPt371QPMNPQUHLLb8QEnwLEeKSIlKLaNmCFeEvEykOSwbrtDxMLIa4Yw2SAvpynMrhSkmDxNzqbZSNPJ41/siSK9rsdNEF/S7pycz+QGXA3/DPkg/hLh+wyjVhBtV4fu6w/B7dyeQpeF5eNUD/wF0B01dxOEcXKWjsiDm/pU2EodX3+lwpPEnw9pDnvKbRdEbTb9Ise0cteDOG7NRg4lUb3DojZAgMBAAECggEAPNSqsVH9JwAMpA/6mw67gP/9uXQizOmPoNOkk6oDb+5i1wgx26S0qS8/B5U02wsFXKUyyX3Nbrhx3WaNzmghEjKotqxmhfOBKq50vYu6vql+kfSwSdPCr1HwwvkDRzLRyRrTlKIuFCxYo93Mk2tSGIPOZIk612WLhbqWmyjixUTv0aVr1h6/CYOwXObPo0fCTHEa5YqHtIwAKxyv1woslZE/Ler7nNOZdAX6hu4tI0LGSTrGBMc9NkiP7O4CMO1AddaYXx7D33JWF03OS6EurAGLUkZNjF+A/PmzMUroiLQp8mND6Io2hrx2sfMuMqAyEBgCM+gZt+bUIqI7HqB08QKBgQDqcpjVf68CLSiQpeStE5oGuYRHH1ay6ii1KMzcpGPIAhGqOybNZ/MDUJbaUkk0C6XdE8aIY/wdX3UcctnO7bwViKxoj4nexuJ0aV5CECAv3IyvBP7pZGAMrvp67R/xe892an58eagpe9rhd3nRzDwIFE2HQ+y+cp8FXbQSOd4e7QKBgQDHOECE9AKZCSvPbY5VW8Y/ztmz5Vcq02JllS2UPlMUebHRg8g2my/9vMr4cG2tPwqVn51QdTJkNfBgWl6KrPo3KJ5LQSezkiOj1nACcSJqUViTvpnlHd3X/ifU/IBrLygJiRxv0SDTz3y7jecVW/ZE38bjycaQW1Yv/I7e8lAoHQKBgGXUKl+o2qmWVaUl+MHX3rGHCFYf3XdOTyoIM5qt6AzqISQQFxVmTd2lti/TR6pMWNlCCpwY2Vskp+gYVlQTW/r6Zu/vUFGrjpZDYcZN3L0NDSnDgLh8eV9o7LBRp+sp/H0RWijUal7CRdpiG04tZ/GWZ+oVbZF2lW0uOtUjvz8tAoGBAKrJ1sYkSnXYHu7dBUC4ROU+9/P5kRjtz1U25rRIGgFbss3jJClsMWBeEcOa3uu/N9u90qe/UUwH0eNIlfRdBsVy1QG/AcI4bsVueOgfBVoQEtfWdyisyhr5kDxPm+hHrRM/sFlL99Cd+Fjx9kGhbSbukRuHR+tJ4kGRSwpmwcEhAoGBANZ8V4Rjs2/6ORLLwwHPcInV8j/gHFQT1ikiPHteBOcyC5Dv5Gd+wy6yuckTraqTcgbbjP19ViQoWO5chroAnUj+KoSkoxeZ0+KKJ4x1yAetmuf27mkLydO5Q8pYBzrDcQF2xEELn3BWXA/+h3MFWV0D+UPNbmZqr7XhwHzTVjOl";
    private static final String expSigningCertificate = "MIIDPzCCAiegAwIBAgIUfZ2mNcS2uxM6CBTdJRpOcPwjNH0wDQYJKoZIhvcNAQELBQAwLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMB4XDTIzMDkyNzA5MTgwMVoXDTI0MDkyNjA5MTgwMVowLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtnKY9c24wbK+Dbby2vt4/Cm9ZsHB5UTcDIz62UNX1QBTSrkkYBlPO2jGr0sRVJkDDcvDxBG/l9iMYDbd+iQyB6iuiGhIqjrHc1A18Gxxq6BSDObrZepg+M1am7j7d+9UDzDT0FByy2/EBJ8CxHikiJSi2jZghXhLxMpDksG67Q8TCyGuGMNkgL6cpzK4UpJg8Tc6m2UjTyeNf7Ikiva7HTRBf0u6cnM/kBlwN/wz5IP4S4fsMo1YQbVeH7usPwe3cnkKXheXjVA/8BdAdNXcThHFylo7Ig5v6VNhKHV9/pcKTxJ8PaQ57ym0XRG02/SLHtHLXgzhuzUYOJVG9w6I2QIDAQABo1MwUTAdBgNVHQ4EFgQUDvOK5Dy+4lkO0lQo5r8hjxXpRxQwHwYDVR0jBBgwFoAUDvOK5Dy+4lkO0lQo5r8hjxXpRxQwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAhTPaUkGAbepVzlTNyRD71MLeNUuKjVg5vNQj3kyQWiUo1lgudu/D0Aq74qeY2Dtsp7VoFuKOZFXuu0HmSCNbn9qE3Z8De8NVdBTBHqfXw6C2fD3DQbq/VxrfzXz9sWP28SCoN3MBpIqoUvqP+KqlpuaTM8ZJRtumSNaZ11LY2jlnJ2wKTBfBOjTux3Y4WCzMp4Zx/bu7ArpiK9j3UvYkqOvvgYT8MlHdE9X/jo9avjdqQaJ0YyPm4kNyLP5Uzfbfse/x3ymt1LPU/jInbGe+JOr6DFTnti0w6dZp4KiNhuTNoUnqwSG0KZe1rncwW3CsVxYwxd6BYAKkKV5sb73C0Q==";

    private static final String expCryptKey = null;
    private static final String expCryptCertificate = null;

    /*-- automatic discovery param --*/
    private static final String expIdpMetadataUrl = String.format("http://127.0.0.1:%s/%s", Integer.valueOf(IDP_METADATA_MOCK_PORT).toString(), MOCK_METADATA_ENDPOINT);
    /*-- manual discovery params start --*/
    private static final String expIdpEntityId = "http://toy-web-sso.it/toyservice/metadata";
    private static final String expWebSsoUrl = "http://toy-web-sso.it/toyservice/ssourl";
    private static final String expWebLogoutUrl = "http://toy-web-sso.it/toyservice/logout";
    private static final Boolean expSignAuthNRequest = true;
    private static final String expVerificationCertificate = "MIIDPzCCAiegAwIBAgIUfxQng0AcL1XU2Ry/B0CX4DdpXWcwDQYJKoZIhvcNAQELBQAwLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMB4XDTIzMDkyODA5NDMyOVoXDTI0MDkyNzA5NDMyOVowLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4G9prMo3UhIJquYw1oFjz4gBGzO8HLS//sJuZ7SIRu2tytc8eG890Mo1EFdSojUKzcH4+R94u6LwfmQLJ1eJEYDlSHB9lI1bfhVISCRWF7G2I7bS4d9eHXwizuVU7/DQZhSUMaOorR3KTYVXcNxatX8eSyqF9LDd86K5lkKRQ9c7Mm70KJR2skpL8enUHxc15v1jSyexagM3Job/p1XkYPRtD1vZuYVjHncp6B9H7S/UBvdqnQoJr9tzNtDpXo8xsZjQXkcuetvV+mc/LZczp9PlflMzaZafOpNWMxuxFad2Jx0GIXSAbUCNhuviJ3IXPa5dhCl4AMX9DGNaX8rASQIDAQABo1MwUTAdBgNVHQ4EFgQUEc0a+j1nZ5knjxetB2dx1qPMEnswHwYDVR0jBBgwFoAUEc0a+j1nZ5knjxetB2dx1qPMEnswDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAANg2NmJPah7GWKXt769inNxApXeDrEUHF8L1jLPzMC35LTtw0FV902Kyohw7sNRHqiHO9yAgyyMxr3DaHLIS6FG0o1K43yZBwrUhNnZHZ93ynmNCxLswj3yswT5a5dxHkhRJrmxuPVtUPmjr5UtHSIXjUWSTC13iXfZgp9efZHB41eKi+0y0yr9ltzJPbPfZ0H36tsFYcYWcBuKW5rSQL9xahRI6aHniTRORxLPCKUxNjzDW7nEgwzoAPl+oMmp1YXHi4Y78cEguRlhZXAOsy7W66O1v0Afzn4F/U5t3E3+QRw82rRe44/z7gNsE0lu9d6Q+Wt98A4Yc3EWvyK5qlg==";
    private static final String expSsoServiceBinding = "http://toy-web-sso.it/toyservice/ssobinding";
    /*-- manual discovery end --*/

    private static final String expNameIDFormat = null;
    private static final Boolean expNameIDAllowCreate = null;
    private static final Boolean expForceAuth = null;
    private static final Boolean expIsPassive = null;
    private static final HashSet<String> expAuthnContextClasses = null;
    private static final String expAuthnContextComparison = null;
    private static final String expUserNameAttributeName = null;


    private static final Boolean expTrustEmailAddress = true;
    private static final Boolean expAlwaysTrustEmailAddress = true;
    private static final Boolean expRequireEmailAddress = false;

    private static final String expAuthority = "saml";
    private static final Saml2MessageBinding expRPRAssertionConsumerBinding = Saml2MessageBinding.POST;
    private static final String expRPRAssertionConsumerLocation = String.format("{baseUrl}/auth/%s/sso/{registrationId}", expAuthority);
    /**
     *
     * @return an hardcoded saml identity provider configuration map (this mock the user provided map)
     */
    private Map<String, Serializable> buildRawConfigurationMap(boolean withAutomaticDiscovery) {
        Map<String, Serializable> rawCfgMap = new HashMap<>();
        rawCfgMap.put("type", SamlIdentityProviderConfigMap.RESOURCE_TYPE);
        rawCfgMap.put("entityId", expEntityID);
        rawCfgMap.put("signingKey", expSigningKey);
        rawCfgMap.put("signingCertificate", expSigningCertificate);
        rawCfgMap.put("cryptKey", expCryptKey);
        rawCfgMap.put("cryptCertificate", expCryptCertificate);
        if (withAutomaticDiscovery) {
            rawCfgMap.put("idpMetadataUrl", expIdpMetadataUrl);
            rawCfgMap.put("idpEntityId", null);
            rawCfgMap.put("webSsoUrl", null);
            rawCfgMap.put("webLogoutUrl", null);
            rawCfgMap.put("signAuthNRequest", null);
            rawCfgMap.put("verificationCertificate", null);
            rawCfgMap.put("ssoServiceBinding", null);
        } else {
            rawCfgMap.put("idpMetadataUrl", null);
            rawCfgMap.put("idpEntityId", expIdpEntityId);
            rawCfgMap.put("webSsoUrl", expWebSsoUrl);
            rawCfgMap.put("webLogoutUrl", expWebLogoutUrl);
            rawCfgMap.put("signAuthNRequest", expSignAuthNRequest);
            rawCfgMap.put("verificationCertificate", expVerificationCertificate);
            rawCfgMap.put("ssoServiceBinding", expSsoServiceBinding);
        }
        rawCfgMap.put("nameIDFormat", expNameIDFormat);
        rawCfgMap.put("nameIDAllowCreate", expNameIDAllowCreate);
        rawCfgMap.put("forceAuthn", expForceAuth);
        rawCfgMap.put("isPassive", expIsPassive);
        rawCfgMap.put("authnContextClasses", expAuthnContextClasses);
        rawCfgMap.put("authnContextComparison", expAuthnContextComparison);
        rawCfgMap.put("userNameAttributeName", expUserNameAttributeName);
        rawCfgMap.put("trustEmailAddress", expTrustEmailAddress);
        rawCfgMap.put("alwaysTrustEmailAddress", expAlwaysTrustEmailAddress);
        rawCfgMap.put("requireEmailAddress", expRequireEmailAddress);
        return  rawCfgMap;
    }

    private ConfigurableIdentityProvider getDefaultConfigurableIdentityProvider() {
        return new ConfigurableIdentityProvider(expAuthority, expProviderId, "toyRealmName");
    }

    private IdentityProviderSettingsMap getDefaultIdentityProviderSettingsMap() {
        Map<String, Serializable> rawSettingsMap = new HashMap<>();
        rawSettingsMap.put("type", IdentityProviderSettingsMap.RESOURCE_TYPE);
        rawSettingsMap.put("linkable", true);
        rawSettingsMap.put("persistence", PersistenceMode.REPOSITORY);
        rawSettingsMap.put("events", "full");
        rawSettingsMap.put("position", 0);
        IdentityProviderSettingsMap idProvSettingsMap = new IdentityProviderSettingsMap();
        idProvSettingsMap.setConfiguration(rawSettingsMap);

        return idProvSettingsMap;
    }

    /**
     * Fetch the Saml Identity Provider metadata from a example file and yields it as string
     * @return Saml identity provider metadata as string
     * @throws Exception when the file is not found or not valid
     */
    private String fetchIdpMetadataExample() throws Exception {
        InputStream idpMetadataStream = getClass().getClassLoader().getResourceAsStream(IDP_METADATA_EXAMPLE_FP);
        Assertions.assertThat(idpMetadataStream).isNotNull();
        String idpMetadataValueAsBody;
        try (
                InputStreamReader idpMetadataReader = new InputStreamReader(idpMetadataStream);
                BufferedReader buffer = new BufferedReader(idpMetadataReader)) {
            idpMetadataValueAsBody = buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        return idpMetadataValueAsBody;
    }

    private static WireMockServer buildMockIdpServer (String serverMetadataResponseBody) {
        // this method defines a mock idp server designed to mock an IDP metadata request
        WireMockServer wireMockServer = new WireMockServer(IDP_METADATA_MOCK_PORT);
        wireMockServer.stubFor(get(urlEqualTo("/"+ MOCK_METADATA_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/samlmetadata+xml;charset=utf-8")
                        .withBody(serverMetadataResponseBody)
                )
        );
        return wireMockServer;
    }

    /**
     * This test checks if the configuration map used to create a saml configuration matches the
     * actual information in the saml configuration map object.
     * @throws Exception
     */
    @Test
    public void validateSamlConfigMapCreation() throws Exception {
        List<Boolean> metadataDiscoveryCases = Arrays.asList(true, false);
        for (Boolean metadataCase : metadataDiscoveryCases) {
            Map<String, Serializable> rawCfgMap = buildRawConfigurationMap(metadataCase);
            SamlIdentityProviderConfigMap idpCfgMap = new SamlIdentityProviderConfigMap();
            idpCfgMap.setConfiguration(rawCfgMap);

            Assertions.assertThat(idpCfgMap.getEntityId()).isEqualTo(rawCfgMap.get("entityId"));
            Assertions.assertThat(idpCfgMap.getSigningKey()).isEqualTo(rawCfgMap.get("signingKey"));
            Assertions.assertThat(idpCfgMap.getSigningCertificate()).isEqualTo(rawCfgMap.get("signingCertificate"));
            Assertions.assertThat(idpCfgMap.getCryptKey()).isEqualTo(rawCfgMap.get("cryptKey"));
            Assertions.assertThat(idpCfgMap.getCryptCertificate()).isEqualTo(rawCfgMap.get("cryptCertificate"));

            Assertions.assertThat(idpCfgMap.getIdpMetadataUrl()).isEqualTo(rawCfgMap.get("idpMetadataUrl"));
            Assertions.assertThat(idpCfgMap.getIdpEntityId()).isEqualTo(rawCfgMap.get("idpEntityId"));
            Assertions.assertThat(idpCfgMap.getWebSsoUrl()).isEqualTo(rawCfgMap.get("webSsoUrl"));
            Assertions.assertThat(idpCfgMap.getWebLogoutUrl()).isEqualTo(rawCfgMap.get("webLogoutUrl"));
            Assertions.assertThat(idpCfgMap.getSignAuthNRequest()).isEqualTo(rawCfgMap.get("signAuthNRequest"));
            Assertions.assertThat(idpCfgMap.getVerificationCertificate()).isEqualTo(rawCfgMap.get("verificationCertificate"));
            Assertions.assertThat(idpCfgMap.getSsoServiceBinding()).isEqualTo(rawCfgMap.get("ssoServiceBinding"));

            Assertions.assertThat(idpCfgMap.getNameIDFormat()).isEqualTo(rawCfgMap.get("nameIDFormat"));
            Assertions.assertThat(idpCfgMap.getNameIDAllowCreate()).isEqualTo(rawCfgMap.get("nameIDAllowCreate"));
            Assertions.assertThat(idpCfgMap.getForceAuthn()).isEqualTo(rawCfgMap.get("forceAuthn"));
            Assertions.assertThat(idpCfgMap.getIsPassive()).isEqualTo(rawCfgMap.get("isPassive"));
            Assertions.assertThat(idpCfgMap.getAuthnContextClasses()).isEqualTo(rawCfgMap.get("authnContextClasses"));
            Assertions.assertThat(idpCfgMap.getAuthnContextComparison()).isEqualTo(rawCfgMap.get("authnContextComparison"));
            Assertions.assertThat(idpCfgMap.getUserNameAttributeName()).isEqualTo(rawCfgMap.get("userNameAttributeName"));

            Assertions.assertThat(idpCfgMap.getTrustEmailAddress()).isEqualTo(rawCfgMap.get("trustEmailAddress"));
            Assertions.assertThat(idpCfgMap.getAlwaysTrustEmailAddress()).isEqualTo(rawCfgMap.get("alwaysTrustEmailAddress"));
            Assertions.assertThat(idpCfgMap.getRequireEmailAddress()).isEqualTo(rawCfgMap.get("requireEmailAddress"));
        }
    }

    /**
     * This tests checks if s saml configuration map matches the content of the given saml configuration
     * @throws Exception
     */
    @Test
    public void validateSamlIdentityProviderConfigCreation() throws Exception {

        // Create (hardcoded) configuration map to be used for initialization
        Map<String, Serializable> rawCfgMap = buildRawConfigurationMap(true);
        SamlIdentityProviderConfigMap idProvCfgMap = new SamlIdentityProviderConfigMap();
        idProvCfgMap.setConfiguration(rawCfgMap);

        ConfigurableIdentityProvider cip = getDefaultConfigurableIdentityProvider();
        IdentityProviderSettingsMap idProvSettingMap = getDefaultIdentityProviderSettingsMap();

        SamlIdentityProviderConfig IdPCfg = new SamlIdentityProviderConfig(cip, idProvSettingMap, idProvCfgMap);
        Assertions.assertThat(IdPCfg.getEntityId()).isEqualTo(expEntityID);
    }

    /**
     * This test checks that a RelyingPartyRegistration contains information matching the saml configuration (with automatic idp metadata discovery)
     * @throws Exception
     */
    @Test
    public void validateRelyingPartyRegistrationAutomatic() throws Exception {

        boolean automaticDiscoverySetting = true;
        // Create (hardcoded) configuration map to be used for initialization
        Map<String, Serializable> rawCfgMap = buildRawConfigurationMap(automaticDiscoverySetting);
        SamlIdentityProviderConfigMap idProvCfgMap = new SamlIdentityProviderConfigMap();
        idProvCfgMap.setConfiguration(rawCfgMap);

        ConfigurableIdentityProvider cip = getDefaultConfigurableIdentityProvider();
        IdentityProviderSettingsMap idProvSettingMap = getDefaultIdentityProviderSettingsMap();

        SamlIdentityProviderConfig idProvCfg = new SamlIdentityProviderConfig(cip, idProvSettingMap, idProvCfgMap);
        // Run mock server to locally catch the identity provider metadata request.
        String IdpMetadata = fetchIdpMetadataExample();
        WireMockServer mockIdpServer = buildMockIdpServer(IdpMetadata);
        mockIdpServer.start();

        // check that the obtained saml Identity Provider matches the obtained map
        RelyingPartyRegistration rpRegistration = idProvCfg.getRelyingPartyRegistration();
        mockIdpServer.stop();

        Assertions.assertThat(rpRegistration.getEntityId()).isEqualTo(expEntityID);
        Assertions.assertThat(rpRegistration.getRegistrationId()).isEqualTo(expProviderId);
        Assertions.assertThat(rpRegistration.getSigningX509Credentials().size()).isEqualTo(1);
        for (Saml2X509Credential credential : rpRegistration.getSigningX509Credentials()) {
            // NOTE: these assertions currently assume that there is only one x509credential(s) -> for-loop is trivial
            // NOTE: if there is more than one x509credential(s), iteration order would NOT be guaranteed as we are iterating on a Set

            // check private information
            PrivateKey obtPrivateKey = credential.getPrivateKey();
            String obtPrivKeyb64Enc = Base64.getEncoder().encodeToString(obtPrivateKey.getEncoded());
            Assertions.assertThat(obtPrivKeyb64Enc).isEqualTo(expSigningKey);

            // check public information (certificate)
            X509Certificate obtCertificate = credential.getCertificate();
            String obtCertificateB64enc = Base64.getEncoder().encodeToString(obtCertificate.getEncoded());
            Assertions.assertThat(obtCertificateB64enc).isEqualTo(expSigningCertificate);
        }
        Assertions.assertThat(rpRegistration.getAssertionConsumerServiceLocation()).isEqualTo(expRPRAssertionConsumerLocation);
        Assertions.assertThat(rpRegistration.getAssertionConsumerServiceBinding()).isEqualTo(expRPRAssertionConsumerBinding);

        RelyingPartyRegistration.AssertingPartyDetails partyDetails =  rpRegistration.getAssertingPartyDetails();
        // sentinel value(s) used to check if auto configuration at least fetched data from the correct location
        // This is a simplified approach. A deep and complete analysis will eventually be delegated to a second test
        String expIdPEntityId = "https://idp.identityserver";
        boolean expWantAuthNRequestSigned=true;
        String expSSOLocation = "https://idp.identityserver/saml/sso";
        Saml2MessageBinding expSSOBinding = Saml2MessageBinding.REDIRECT;

        Assertions.assertThat(partyDetails.getEntityId()).isEqualTo(expIdPEntityId);
        Assertions.assertThat(partyDetails.getSingleSignOnServiceLocation()).isEqualTo(expSSOLocation);
        Assertions.assertThat(partyDetails.getSingleSignOnServiceBinding()).isEqualTo(expSSOBinding);
        Assertions.assertThat(partyDetails.getWantAuthnRequestsSigned()).isEqualTo(expWantAuthNRequestSigned);
    }

    /**
     * This test checks that a RelyingPartyRegistration contains information matching the saml configuration (with manual idp metadata discovery)
     * @throws Exception
     */
    @Test
    public void validateRelyingPartyRegistrationManual() throws Exception {

        boolean automaticDiscoverySetting = false;
        // Create (hardcoded) configuration map to be used for initialization
        Map<String, Serializable> rawCfgMap = buildRawConfigurationMap(automaticDiscoverySetting);
        SamlIdentityProviderConfigMap idProvCfgMap = new SamlIdentityProviderConfigMap();
        idProvCfgMap.setConfiguration(rawCfgMap);

        ConfigurableIdentityProvider cip = getDefaultConfigurableIdentityProvider();
        IdentityProviderSettingsMap idProvSettingMap = getDefaultIdentityProviderSettingsMap();

        SamlIdentityProviderConfig idProvCfg = new SamlIdentityProviderConfig(cip, idProvSettingMap, idProvCfgMap);

        // check that the obtained saml Identity Provider matches the obtained map
        RelyingPartyRegistration rpRegistration = idProvCfg.getRelyingPartyRegistration();
        Assertions.assertThat(rpRegistration.getEntityId()).isEqualTo(expEntityID);
        Assertions.assertThat(rpRegistration.getRegistrationId()).isEqualTo(expProviderId);
        Assertions.assertThat(rpRegistration.getSigningX509Credentials().size()).isEqualTo(1);
        Assertions.assertThat(rpRegistration.getAssertionConsumerServiceLocation()).isEqualTo(expRPRAssertionConsumerLocation);
        Assertions.assertThat(rpRegistration.getAssertionConsumerServiceBinding()).isEqualTo(expRPRAssertionConsumerBinding);

        RelyingPartyRegistration.AssertingPartyDetails partyDetails =  rpRegistration.getAssertingPartyDetails();
        Assertions.assertThat(partyDetails.getEntityId()).isEqualTo(expIdpEntityId);
        Assertions.assertThat(partyDetails.getSingleSignOnServiceLocation()).isEqualTo(expWebSsoUrl);
        Assertions.assertThat(partyDetails.getWantAuthnRequestsSigned()).isEqualTo(expSignAuthNRequest);
//        Assertions.assertThat(partyDetails.getSingleLogoutServiceLocation()).isEqualTo(expWebLogoutUrl);
        Assertions.assertThat(partyDetails.getVerificationX509Credentials().size()).isEqualTo(1);
        Assertions.assertThat(partyDetails.getEncryptionX509Credentials().size()).isEqualTo(0); // nessuna credenziale di cifratura fornita
        for (Saml2X509Credential credential : partyDetails.getVerificationX509Credentials()) {
            Assertions.assertThat(credential.getPrivateKey()).isNull();
            X509Certificate obtCertificate = credential.getCertificate();
            String obtCertificateB64enc = Base64.getEncoder().encodeToString(obtCertificate.getEncoded());

            Assertions.assertThat(obtCertificateB64enc).isEqualTo(expVerificationCertificate);
        }
    }
}

