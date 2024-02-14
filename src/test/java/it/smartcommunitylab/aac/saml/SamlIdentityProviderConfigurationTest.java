package it.smartcommunitylab.aac.saml;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import java.io.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import javax.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

/**
 * This test is aimed at validating if a configuration map will yield a valid
 * and corresponding SamlProviderConfiguration, including the appropriate
 * Relying Party Registration.
 */
@SpringBootTest
@EnableWireMock({ @ConfigureWireMock(name = "idp-server", property = "idp-server-url.example") })
public class SamlIdentityProviderConfigurationTest {

    @InjectWireMock("idp-server")
    private WireMockServer mockIdPServer;

    @Autowired
    private Environment env;

    private static final String PROVIDER_ID = "ProviderTestId";
    private static final String ENTITY_ID = String.format("http://localhost:8080/auth/saml/metadata/%s", PROVIDER_ID);

    // key pair generated with openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -keyout privateKey.key -out certificate.crt, using openssl version 1.1.1n
    private static final String SIGN_PRIV_KEY =
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC2cpj1zbjBsr4NtvLa+3j8Kb1mwcHlRNwMjPrZQ1fVAFNKuSRgGU87aMavSxFUmQMNy8PEEb+X2IxgNt36JDIHqK6IaEiqOsdzUDXwbHGroFIM5utl6mD4zVqbuPt371QPMNPQUHLLb8QEnwLEeKSIlKLaNmCFeEvEykOSwbrtDxMLIa4Yw2SAvpynMrhSkmDxNzqbZSNPJ41/siSK9rsdNEF/S7pycz+QGXA3/DPkg/hLh+wyjVhBtV4fu6w/B7dyeQpeF5eNUD/wF0B01dxOEcXKWjsiDm/pU2EodX3+lwpPEnw9pDnvKbRdEbTb9Ise0cteDOG7NRg4lUb3DojZAgMBAAECggEAPNSqsVH9JwAMpA/6mw67gP/9uXQizOmPoNOkk6oDb+5i1wgx26S0qS8/B5U02wsFXKUyyX3Nbrhx3WaNzmghEjKotqxmhfOBKq50vYu6vql+kfSwSdPCr1HwwvkDRzLRyRrTlKIuFCxYo93Mk2tSGIPOZIk612WLhbqWmyjixUTv0aVr1h6/CYOwXObPo0fCTHEa5YqHtIwAKxyv1woslZE/Ler7nNOZdAX6hu4tI0LGSTrGBMc9NkiP7O4CMO1AddaYXx7D33JWF03OS6EurAGLUkZNjF+A/PmzMUroiLQp8mND6Io2hrx2sfMuMqAyEBgCM+gZt+bUIqI7HqB08QKBgQDqcpjVf68CLSiQpeStE5oGuYRHH1ay6ii1KMzcpGPIAhGqOybNZ/MDUJbaUkk0C6XdE8aIY/wdX3UcctnO7bwViKxoj4nexuJ0aV5CECAv3IyvBP7pZGAMrvp67R/xe892an58eagpe9rhd3nRzDwIFE2HQ+y+cp8FXbQSOd4e7QKBgQDHOECE9AKZCSvPbY5VW8Y/ztmz5Vcq02JllS2UPlMUebHRg8g2my/9vMr4cG2tPwqVn51QdTJkNfBgWl6KrPo3KJ5LQSezkiOj1nACcSJqUViTvpnlHd3X/ifU/IBrLygJiRxv0SDTz3y7jecVW/ZE38bjycaQW1Yv/I7e8lAoHQKBgGXUKl+o2qmWVaUl+MHX3rGHCFYf3XdOTyoIM5qt6AzqISQQFxVmTd2lti/TR6pMWNlCCpwY2Vskp+gYVlQTW/r6Zu/vUFGrjpZDYcZN3L0NDSnDgLh8eV9o7LBRp+sp/H0RWijUal7CRdpiG04tZ/GWZ+oVbZF2lW0uOtUjvz8tAoGBAKrJ1sYkSnXYHu7dBUC4ROU+9/P5kRjtz1U25rRIGgFbss3jJClsMWBeEcOa3uu/N9u90qe/UUwH0eNIlfRdBsVy1QG/AcI4bsVueOgfBVoQEtfWdyisyhr5kDxPm+hHrRM/sFlL99Cd+Fjx9kGhbSbukRuHR+tJ4kGRSwpmwcEhAoGBANZ8V4Rjs2/6ORLLwwHPcInV8j/gHFQT1ikiPHteBOcyC5Dv5Gd+wy6yuckTraqTcgbbjP19ViQoWO5chroAnUj+KoSkoxeZ0+KKJ4x1yAetmuf27mkLydO5Q8pYBzrDcQF2xEELn3BWXA/+h3MFWV0D+UPNbmZqr7XhwHzTVjOl";
    private static final String SIGN_CERTIFICATE =
        "MIIDPzCCAiegAwIBAgIUfZ2mNcS2uxM6CBTdJRpOcPwjNH0wDQYJKoZIhvcNAQELBQAwLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMB4XDTIzMDkyNzA5MTgwMVoXDTI0MDkyNjA5MTgwMVowLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtnKY9c24wbK+Dbby2vt4/Cm9ZsHB5UTcDIz62UNX1QBTSrkkYBlPO2jGr0sRVJkDDcvDxBG/l9iMYDbd+iQyB6iuiGhIqjrHc1A18Gxxq6BSDObrZepg+M1am7j7d+9UDzDT0FByy2/EBJ8CxHikiJSi2jZghXhLxMpDksG67Q8TCyGuGMNkgL6cpzK4UpJg8Tc6m2UjTyeNf7Ikiva7HTRBf0u6cnM/kBlwN/wz5IP4S4fsMo1YQbVeH7usPwe3cnkKXheXjVA/8BdAdNXcThHFylo7Ig5v6VNhKHV9/pcKTxJ8PaQ57ym0XRG02/SLHtHLXgzhuzUYOJVG9w6I2QIDAQABo1MwUTAdBgNVHQ4EFgQUDvOK5Dy+4lkO0lQo5r8hjxXpRxQwHwYDVR0jBBgwFoAUDvOK5Dy+4lkO0lQo5r8hjxXpRxQwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAhTPaUkGAbepVzlTNyRD71MLeNUuKjVg5vNQj3kyQWiUo1lgudu/D0Aq74qeY2Dtsp7VoFuKOZFXuu0HmSCNbn9qE3Z8De8NVdBTBHqfXw6C2fD3DQbq/VxrfzXz9sWP28SCoN3MBpIqoUvqP+KqlpuaTM8ZJRtumSNaZ11LY2jlnJ2wKTBfBOjTux3Y4WCzMp4Zx/bu7ArpiK9j3UvYkqOvvgYT8MlHdE9X/jo9avjdqQaJ0YyPm4kNyLP5Uzfbfse/x3ymt1LPU/jInbGe+JOr6DFTnti0w6dZp4KiNhuTNoUnqwSG0KZe1rncwW3CsVxYwxd6BYAKkKV5sb73C0Q==";
    private static final String KEY_PAIR_ALGORITHM = "RSA";

    /*-- config for automatic discovery param --*/
    private static final String IDP_METADATA_ENDPOINT = "/metadata";
    private String IDP_METADATA_URL; // NOTE: variable evaluated at runtime using environment properties

    /*-- config for manual discovery params start --*/
    private static final String IDP_ENTITY_ID = "http://toy-web-sso.invalid/toyservice/metadata";
    private static final String IDP_WEB_SSO_URL = "http://toy-web-sso.invalid/toyservice/ssourl";
    private static final String IDP_WEB_LOGOUT_URL = "http://toy-web-sso.invalid/toyservice/logout";
    private static final Boolean IDP_SIGN_AUTHN_REQUEST = true;
    private static final String IDP_VERIFICATION_CERTIFICATE =
        "MIIDPzCCAiegAwIBAgIUfxQng0AcL1XU2Ry/B0CX4DdpXWcwDQYJKoZIhvcNAQELBQAwLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMB4XDTIzMDkyODA5NDMyOVoXDTI0MDkyNzA5NDMyOVowLzELMAkGA1UEBhMCSVQxDzANBgNVBAgMBlRyZW50bzEPMA0GA1UEBwwGVHJlbnRvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4G9prMo3UhIJquYw1oFjz4gBGzO8HLS//sJuZ7SIRu2tytc8eG890Mo1EFdSojUKzcH4+R94u6LwfmQLJ1eJEYDlSHB9lI1bfhVISCRWF7G2I7bS4d9eHXwizuVU7/DQZhSUMaOorR3KTYVXcNxatX8eSyqF9LDd86K5lkKRQ9c7Mm70KJR2skpL8enUHxc15v1jSyexagM3Job/p1XkYPRtD1vZuYVjHncp6B9H7S/UBvdqnQoJr9tzNtDpXo8xsZjQXkcuetvV+mc/LZczp9PlflMzaZafOpNWMxuxFad2Jx0GIXSAbUCNhuviJ3IXPa5dhCl4AMX9DGNaX8rASQIDAQABo1MwUTAdBgNVHQ4EFgQUEc0a+j1nZ5knjxetB2dx1qPMEnswHwYDVR0jBBgwFoAUEc0a+j1nZ5knjxetB2dx1qPMEnswDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAANg2NmJPah7GWKXt769inNxApXeDrEUHF8L1jLPzMC35LTtw0FV902Kyohw7sNRHqiHO9yAgyyMxr3DaHLIS6FG0o1K43yZBwrUhNnZHZ93ynmNCxLswj3yswT5a5dxHkhRJrmxuPVtUPmjr5UtHSIXjUWSTC13iXfZgp9efZHB41eKi+0y0yr9ltzJPbPfZ0H36tsFYcYWcBuKW5rSQL9xahRI6aHniTRORxLPCKUxNjzDW7nEgwzoAPl+oMmp1YXHi4Y78cEguRlhZXAOsy7W66O1v0Afzn4F/U5t3E3+QRw82rRe44/z7gNsE0lu9d6Q+Wt98A4Yc3EWvyK5qlg==";
    private static final String IDP_SSO_SERVICE_BINDING_LOCATION = "http://toy-web-sso.invalid/toyservice/ssobinding";
    private static final Saml2MessageBinding IDP_SSO_BINDING_TYPE = Saml2MessageBinding.REDIRECT;
    /*-- manual discovery end --*/

    private static final String AUTHORITY = "saml";
    private static final Saml2MessageBinding RPR_ASSERTION_CONSUMER_BINDING = Saml2MessageBinding.POST;
    private static final String RPR_ASSERTION_CONSUMER_LOCATION = String.format(
        "{baseUrl}/auth/%s/sso/{registrationId}",
        AUTHORITY
    );

    /*
     Identity Provider (SAML assessing party) metadata templates and values - involved when doing relying party registration, automatic assessing party discovery from metadata url
     This template was inspired from the following source/reference:
     https://github.com/spring-projects/spring-security/blob/main/saml2/saml2-service-provider/src/test/java/org/springframework/security/saml2/provider/service/registration/OpenSamlMetadataRelyingPartyRegistrationConverterTests.java
     The filled template is used to produce the stub response, available in the mapping folder
     NOTE: Spring Security will validate that the (filled) template is well formed
     NOTE: references, marked as (N), with N positive integer, are used as helpers/reference when filling the template
     */
    private static final String ASSERTING_PARTY_METADATA_TEMPLATE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:alg=\"urn:oasis:names:tc:SAML:metadata:algsupport\" " +
        "entityID=\"%s\" ID=\"_bf133aac099b99b3d81286e1a341f2d34188043a77fe15bf4bf1487dae9b2ea3\">\n" + //(1)
        "<md:IDPSSODescriptor WantAuthnRequestsSigned=\"%s\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n" + //(2)
        "<md:SingleSignOnService Binding=\"%s\" Location=\"%s\"/>\n" + // (3), (4)
        "<md:SingleLogoutService Binding=\"%s\" Location=\"%s\"/>\n" + // (5), (6)
        "<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>\n" +
        //+ "<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</md:NameIDFormat>\n"
        "<md:KeyDescriptor use=\"%s\">\n" + // (7)
        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
        "<ds:X509Data>\n" +
        "<ds:X509Certificate>%s</ds:X509Certificate>\n" + // (8)
        "</ds:X509Data>\n" +
        "</ds:KeyInfo>\n" +
        "</md:KeyDescriptor>\n" +
        "</md:IDPSSODescriptor>\n" +
        "</md:EntityDescriptor>";
    private static final String ASSERTING_PARTY_ENTITY_ID = "https://idp.identityserver.invalid";
    private static final boolean ASSERTING_PARTY_WANT_AUTHN_SIGNED = true;
    private static final Saml2MessageBinding ASSERTING_PARTY_SSO_CONSUMER_BINDING = Saml2MessageBinding.REDIRECT;
    private static final String ASSERTING_PARTY_SSO_BINDING_LOCATION = "https://idp.identityserver.invalid/saml/sso";
    private static final String ASSESSING_PARTY_KEY_USAGE = "signing";

    @PostConstruct
    private void postConstruct() {
        IDP_METADATA_URL = env.getProperty("idp-server-url.example") + IDP_METADATA_ENDPOINT;
    }

    /**
     * This test checks if the configuration map used to create a saml configuration matches the
     * actual information in the saml configuration map object.
     * @throws Exception
     */
    @Test
    public void validateSamlConfigMapAutoDiscoveryCreation() throws Exception {
        // manually set the configuration map that will be used to build the config map
        Map<String, Serializable> rawCfgMap = new HashMap<>();
        rawCfgMap.put("type", SamlIdentityProviderConfigMap.RESOURCE_TYPE);
        rawCfgMap.put("entityId", ENTITY_ID);
        rawCfgMap.put("signingKey", SIGN_PRIV_KEY);
        rawCfgMap.put("signingCertificate", SIGN_CERTIFICATE);
        rawCfgMap.put("cryptKey", "ToyPEMbase64PrivateKeyValue");
        rawCfgMap.put("cryptCertificate", "Toyx509PublicKeyCertificate(s)");
        rawCfgMap.put("idpMetadataUrl", IDP_METADATA_URL);
        rawCfgMap.put("idpEntityId", "ToyIdPEntityManualDiscovery");
        rawCfgMap.put("webSsoUrl", "toyWebSsoUrlManualDiscovery");
        rawCfgMap.put("webLogoutUrl", "toyWebLougoutUrlManualDiscovery");
        rawCfgMap.put("signAuthNRequest", true);
        rawCfgMap.put("verificationCertificate", "ToyVerificationCertificateForManualDiscovery");
        rawCfgMap.put("ssoServiceBinding", "ToySsoServiceBindingManual");
        rawCfgMap.put("nameIDFormat", "toy:name:id:format");
        rawCfgMap.put("nameIDAllowCreate", true);
        rawCfgMap.put("forceAuthn", true);
        rawCfgMap.put("isPassive", true);
        HashSet<String> toySetContexts = new HashSet<String>();
        toySetContexts.add("ToyOneAuthnContextClasses");
        rawCfgMap.put("authnContextClasses", toySetContexts);
        rawCfgMap.put("authnContextComparison", "ToyAuthnContextComparison");
        rawCfgMap.put("userNameAttributeName", "MyAttributes");
        rawCfgMap.put("trustEmailAddress", true);
        rawCfgMap.put("alwaysTrustEmailAddress", true);
        rawCfgMap.put("requireEmailAddress", true);

        SamlIdentityProviderConfigMap idpCfgMap = new SamlIdentityProviderConfigMap();
        idpCfgMap.setConfiguration(rawCfgMap);

        assertThat(idpCfgMap.getEntityId()).isEqualTo(rawCfgMap.get("entityId"));
        assertThat(idpCfgMap.getSigningKey()).isEqualTo(rawCfgMap.get("signingKey"));
        assertThat(idpCfgMap.getSigningCertificate()).isEqualTo(rawCfgMap.get("signingCertificate"));
        assertThat(idpCfgMap.getCryptKey()).isEqualTo(rawCfgMap.get("cryptKey"));
        assertThat(idpCfgMap.getCryptCertificate()).isEqualTo(rawCfgMap.get("cryptCertificate"));

        assertThat(idpCfgMap.getIdpMetadataUrl()).isEqualTo(rawCfgMap.get("idpMetadataUrl"));
        assertThat(idpCfgMap.getIdpEntityId()).isEqualTo(rawCfgMap.get("idpEntityId"));
        assertThat(idpCfgMap.getWebSsoUrl()).isEqualTo(rawCfgMap.get("webSsoUrl"));
        assertThat(idpCfgMap.getWebLogoutUrl()).isEqualTo(rawCfgMap.get("webLogoutUrl"));
        assertThat(idpCfgMap.getSignAuthNRequest()).isEqualTo(rawCfgMap.get("signAuthNRequest"));
        assertThat(idpCfgMap.getVerificationCertificate()).isEqualTo(rawCfgMap.get("verificationCertificate"));
        assertThat(idpCfgMap.getSsoServiceBinding()).isEqualTo(rawCfgMap.get("ssoServiceBinding"));

        assertThat(idpCfgMap.getNameIDFormat()).isEqualTo(rawCfgMap.get("nameIDFormat"));
        assertThat(idpCfgMap.getNameIDAllowCreate()).isEqualTo(rawCfgMap.get("nameIDAllowCreate"));
        assertThat(idpCfgMap.getForceAuthn()).isEqualTo(rawCfgMap.get("forceAuthn"));
        assertThat(idpCfgMap.getIsPassive()).isEqualTo(rawCfgMap.get("isPassive"));
        assertThat(idpCfgMap.getAuthnContextClasses()).isEqualTo(rawCfgMap.get("authnContextClasses"));
        assertThat(idpCfgMap.getAuthnContextComparison()).isEqualTo(rawCfgMap.get("authnContextComparison"));
        assertThat(idpCfgMap.getUserNameAttributeName()).isEqualTo(rawCfgMap.get("userNameAttributeName"));

        assertThat(idpCfgMap.getTrustEmailAddress()).isEqualTo(rawCfgMap.get("trustEmailAddress"));
        assertThat(idpCfgMap.getAlwaysTrustEmailAddress()).isEqualTo(rawCfgMap.get("alwaysTrustEmailAddress"));
        assertThat(idpCfgMap.getRequireEmailAddress()).isEqualTo(rawCfgMap.get("requireEmailAddress"));
    }

    /**
     * This test checks that a RelyingPartyRegistration contains information matching the saml configuration (with automatic idp metadata discovery)
     * @throws Exception
     */
    @Test
    public void validateRelyingPartyRegistrationAutomatic() throws Exception {
        // manually set the Saml configuration (case: assessing party details automatically resolved from metadata discovery)
        Map<String, Serializable> rawCfgMap = new HashMap<>();
        rawCfgMap.put("type", SamlIdentityProviderConfigMap.RESOURCE_TYPE);
        rawCfgMap.put("entityId", ENTITY_ID);
        rawCfgMap.put("signingKey", SIGN_PRIV_KEY);
        rawCfgMap.put("signingCertificate", SIGN_CERTIFICATE);
        rawCfgMap.put("cryptKey", null);
        rawCfgMap.put("cryptCertificate", null);
        rawCfgMap.put("idpMetadataUrl", IDP_METADATA_URL); // url checked for automatic relying party registration
        rawCfgMap.put("idpEntityId", null);
        rawCfgMap.put("webSsoUrl", null);
        rawCfgMap.put("webLogoutUrl", null);
        rawCfgMap.put("signAuthNRequest", null);
        rawCfgMap.put("verificationCertificate", null);
        rawCfgMap.put("ssoServiceBinding", null);
        rawCfgMap.put("nameIDFormat", null);
        rawCfgMap.put("nameIDAllowCreate", null);
        rawCfgMap.put("forceAuthn", null);
        rawCfgMap.put("isPassive", null);
        rawCfgMap.put("authnContextClasses", null);
        rawCfgMap.put("authnContextComparison", null);
        rawCfgMap.put("userNameAttributeName", null);
        rawCfgMap.put("trustEmailAddress", null);
        rawCfgMap.put("alwaysTrustEmailAddress", null);
        rawCfgMap.put("requireEmailAddress", null);

        // set the IdP metadata XML returned by the idp server and used for automatic relying party registration
        String idpMetadata = String.format(
            ASSERTING_PARTY_METADATA_TEMPLATE,
            ASSERTING_PARTY_ENTITY_ID, // (1)
            ASSERTING_PARTY_WANT_AUTHN_SIGNED, // (2)
            ASSERTING_PARTY_SSO_CONSUMER_BINDING.getUrn(), // (3)
            ASSERTING_PARTY_SSO_BINDING_LOCATION, // (4)
            ASSERTING_PARTY_SSO_CONSUMER_BINDING.getUrn(), // (5)
            ASSERTING_PARTY_SSO_BINDING_LOCATION, // (6)
            ASSESSING_PARTY_KEY_USAGE, // (7)
            IDP_VERIFICATION_CERTIFICATE // (8)
        );
        mockIdPServer.stubFor(
            get(urlEqualTo(IDP_METADATA_ENDPOINT))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/xml").withBody(idpMetadata)
                )
        );

        SamlIdentityProviderConfigMap idProvCfgMap = new SamlIdentityProviderConfigMap();
        idProvCfgMap.setConfiguration(rawCfgMap);

        ConfigurableIdentityProvider cip = new ConfigurableIdentityProvider(AUTHORITY, PROVIDER_ID, "toyRealmName");
        IdentityProviderSettingsMap idProvSettingMap = new IdentityProviderSettingsMap();

        SamlIdentityProviderConfig idProvCfg = new SamlIdentityProviderConfig(cip, idProvSettingMap, idProvCfgMap);

        RelyingPartyRegistration rpRegistration = idProvCfg.getRelyingPartyRegistration();

        // check that the registration happened and setted relying party expected values
        assertThat(rpRegistration).isNotNull();
        assertThat(rpRegistration.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(rpRegistration.getRegistrationId()).isEqualTo(PROVIDER_ID);
        assertThat(rpRegistration.getSigningX509Credentials().size()).isEqualTo(1);
        Saml2X509Credential credential = rpRegistration.getSigningX509Credentials().iterator().next();
        assertThat(credential).isNotNull();

        // check private information
        PrivateKey obtainedPrivateKey = credential.getPrivateKey();
        // NOTE: key parsing approach inspired by https://github.com/spring-projects/spring-security/blob/main/saml2/saml2-service-provider/src/test/java/org/springframework/security/saml2/provider/service/registration/OpenSamlMetadataRelyingPartyRegistrationConverterTests.java#L189
        InputStream keyStream = new ByteArrayInputStream(Base64.getDecoder().decode(SIGN_PRIV_KEY.getBytes()));
        PrivateKey expectedPrivateKey = KeyFactory
            .getInstance(KEY_PAIR_ALGORITHM)
            .generatePrivate(new PKCS8EncodedKeySpec(keyStream.readAllBytes()));
        assertThat(obtainedPrivateKey).isEqualTo(expectedPrivateKey);

        // check public information (certificate)
        X509Certificate obtainedCertificate = credential.getCertificate();
        // NOTE: certificate parsing approach inspired by https://github.com/spring-projects/spring-security/blob/main/saml2/saml2-service-provider/src/test/java/org/springframework/security/saml2/provider/service/registration/OpenSamlMetadataRelyingPartyRegistrationConverterTests.java#L189
        InputStream certificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(SIGN_CERTIFICATE.getBytes())
        );
        X509Certificate expectedSigningCertificate = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(certificateStream);
        assertThat(obtainedCertificate).isEqualTo(expectedSigningCertificate);

        assertThat(rpRegistration.getAssertionConsumerServiceLocation()).isEqualTo(RPR_ASSERTION_CONSUMER_LOCATION);
        assertThat(rpRegistration.getAssertionConsumerServiceBinding()).isEqualTo(RPR_ASSERTION_CONSUMER_BINDING);

        // check that the assessing party public signing information was occrrectly registered
        RelyingPartyRegistration.AssertingPartyDetails partyDetails = rpRegistration.getAssertingPartyDetails();
        assertThat(partyDetails).isNotNull();
        // sentinel value(s) used to check if auto configuration at least fetched data from the correct location
        // This is a simplified approach. A deep and complete analysis will eventually be delegated to a second test
        assertThat(partyDetails.getEntityId()).isEqualTo(ASSERTING_PARTY_ENTITY_ID);
        assertThat(partyDetails.getWantAuthnRequestsSigned()).isEqualTo(ASSERTING_PARTY_WANT_AUTHN_SIGNED);
        assertThat(partyDetails.getSingleSignOnServiceLocation()).isEqualTo(ASSERTING_PARTY_SSO_BINDING_LOCATION);
        assertThat(partyDetails.getSingleSignOnServiceBinding()).isEqualTo(ASSERTING_PARTY_SSO_CONSUMER_BINDING);
        assertThat(partyDetails.getSingleLogoutServiceBinding()).isEqualTo(ASSERTING_PARTY_SSO_CONSUMER_BINDING);
        assertThat(partyDetails.getSingleLogoutServiceResponseLocation())
            .isEqualTo(ASSERTING_PARTY_SSO_BINDING_LOCATION);
        Collection<Saml2X509Credential> partyDetailsVerificationCredentials =
            partyDetails.getVerificationX509Credentials();
        assertThat(partyDetailsVerificationCredentials).isNotNull();
        assertThat(partyDetailsVerificationCredentials.size()).isEqualTo(1); // we do not check that credentials (7), (8) were parsed correctly as this task was delegated to spring
    }

    /**
     * This test checks that a RelyingPartyRegistration contains information matching the saml configuration (with manual idp metadata discovery)
     * @throws Exception
     */
    @Test
    public void validateRelyingPartyRegistrationManual() throws Exception {
        // manually set the Saml configuration (case: assessing party details manually resolved from configg map values)
        Map<String, Serializable> rawCfgMap = new HashMap<>();
        rawCfgMap.put("type", SamlIdentityProviderConfigMap.RESOURCE_TYPE);
        rawCfgMap.put("entityId", ENTITY_ID);
        rawCfgMap.put("signingKey", SIGN_PRIV_KEY);
        rawCfgMap.put("signingCertificate", SIGN_CERTIFICATE);
        rawCfgMap.put("cryptKey", null);
        rawCfgMap.put("cryptCertificate", null);
        rawCfgMap.put("idpMetadataUrl", null);
        rawCfgMap.put("idpEntityId", IDP_ENTITY_ID);
        rawCfgMap.put("webSsoUrl", IDP_WEB_SSO_URL);
        rawCfgMap.put("webLogoutUrl", IDP_WEB_LOGOUT_URL);
        rawCfgMap.put("signAuthNRequest", IDP_SIGN_AUTHN_REQUEST);
        rawCfgMap.put("verificationCertificate", IDP_VERIFICATION_CERTIFICATE);
        rawCfgMap.put("ssoServiceBinding", IDP_SSO_SERVICE_BINDING_LOCATION);
        rawCfgMap.put("nameIDFormat", null);
        rawCfgMap.put("nameIDAllowCreate", null);
        rawCfgMap.put("forceAuthn", null);
        rawCfgMap.put("isPassive", null);
        rawCfgMap.put("authnContextClasses", null);
        rawCfgMap.put("authnContextComparison", null);
        rawCfgMap.put("userNameAttributeName", null);
        rawCfgMap.put("trustEmailAddress", null);
        rawCfgMap.put("alwaysTrustEmailAddress", null);
        rawCfgMap.put("requireEmailAddress", null);

        SamlIdentityProviderConfigMap idProvCfgMap = new SamlIdentityProviderConfigMap();
        idProvCfgMap.setConfiguration(rawCfgMap);

        ConfigurableIdentityProvider cip = new ConfigurableIdentityProvider(AUTHORITY, PROVIDER_ID, "toyRealmName");
        IdentityProviderSettingsMap idProvSettingMap = new IdentityProviderSettingsMap();

        SamlIdentityProviderConfig idProvCfg = new SamlIdentityProviderConfig(cip, idProvSettingMap, idProvCfgMap);

        RelyingPartyRegistration rpRegistration = idProvCfg.getRelyingPartyRegistration();

        // check that the registration happened and setted relying party expected values
        assertThat(rpRegistration).isNotNull();
        assertThat(rpRegistration.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(rpRegistration.getRegistrationId()).isEqualTo(PROVIDER_ID);
        assertThat(rpRegistration.getAssertionConsumerServiceLocation()).isEqualTo(RPR_ASSERTION_CONSUMER_LOCATION);
        assertThat(rpRegistration.getAssertionConsumerServiceBinding()).isEqualTo(RPR_ASSERTION_CONSUMER_BINDING);
        assertThat(rpRegistration.getSigningX509Credentials().size()).isEqualTo(1);

        RelyingPartyRegistration.AssertingPartyDetails partyDetails = rpRegistration.getAssertingPartyDetails();

        // check that the registration setted minimal asserting party expected values
        assertThat(partyDetails).isNotNull();
        assertThat(partyDetails.getEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(partyDetails.getSingleSignOnServiceLocation()).isEqualTo(IDP_WEB_SSO_URL);
        assertThat(partyDetails.getWantAuthnRequestsSigned()).isEqualTo(IDP_SIGN_AUTHN_REQUEST);
        //        Assertions.assertThat(partyDetails.getSingleLogoutServiceLocation()).isEqualTo(expWebLogoutUrl);
        assertThat(partyDetails.getVerificationX509Credentials().size()).isEqualTo(1);
        assertThat(partyDetails.getEncryptionX509Credentials().size()).isEqualTo(0); // nessuna credenziale di cifratura fornita

        // check that the assessing party public signing information was occrrectly registered
        Saml2X509Credential credential = partyDetails.getVerificationX509Credentials().iterator().next();
        assertThat(credential).isNotNull();
        assertThat(credential.getPrivateKey()).isNull();
        X509Certificate obtainedCertificate = credential.getCertificate();
        assertThat(obtainedCertificate).isNotNull();
        InputStream certificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(IDP_VERIFICATION_CERTIFICATE.getBytes())
        );
        X509Certificate expectedSigningCertificate = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(certificateStream);
        assertThat(obtainedCertificate).isEqualTo(expectedSigningCertificate);
    }
}
