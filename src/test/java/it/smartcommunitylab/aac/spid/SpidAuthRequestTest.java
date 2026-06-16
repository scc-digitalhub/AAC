package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SigningCredentialHelper;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for the generation and dispatch of SPID SAML 2.0 AuthnRequests.
 * Verifies correct HTTP bindings (Redirect/POST), signatures, mandatory XML attributes...
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "test-spid"})
@EnableWireMock({
    // Setup two fixed-port WireMock servers, mapping them to their respective YAML configuration properties
    @ConfigureWireMock(port = 58838, name = "idp-server-redirect", property = "wiremock.idp.redirect.url"),
    @ConfigureWireMock(port = 58839, name = "idp-server-post", property = "wiremock.idp.post.url")
})
// Add @Transactional to clean up the DB automatically between @Test methods within this class
@Transactional
public class SpidAuthRequestTest extends BaseSpidTest {

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected MockIdpSpid mockIdpSpid = new MockIdpSpid();
    protected IdentityProvider identityProvider = new IdentityProvider();

    @BeforeEach
    public void setupConfigurationAndMocks() {
        initMockMvc();
        mockIdpSpid.prepareMockMetadata(mockIdPServerRedirect, mockIdPServerPost);

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();

                // Any Identity Provider loaded from the bootstrap can be used here
                ConfigurableIdentityProvider idp = idps.get(0);

                identityProvider.initRealmByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);
                identityProvider.initRegistrationIdBinding(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    @Test
    @DisplayName("Verifica runtime Dispatch")
    public void testSpidAuthnRequestDispatch() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String destination = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
            .getRelyingPartyRegistration(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT)
            .getAssertingPartyDetails()
            .getSingleSignOnServiceLocation();

        // Verify that SP performed
        String redirectUrl = spidRequest.getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();

        // Verify the request is routed to the correct IdP destination
        assertThat(redirectUrl).contains(destination);

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();
        assertThat(xmlRequest).contains("saml2p:AuthnRequest");

        assertThat(xmlRequest).contains("ID=\"");
        assertThat(xmlRequest).contains("IssueInstant=\"");

        // Verify the Destination points to the IdP
        assertThat(xmlRequest).contains("Destination=\"" + destination + "\"");
    }

    @Test
    @DisplayName("Verifica runtime default AssertionConsumerServiceIndex e AttributeConsumingServiceIndex")
    public void testSpidAuthnRequestDefaultAssertionURLAndAttribute() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify the SP is requesting the correct value by bootstrap
        if (spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getUseAssertionConsumerServiceUrl()){
            assertThat(xmlRequest).contains("AssertionConsumerServiceURL=\"" +
                spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getAssertionConsumerUrl() + "\"");
        }else {
            assertThat(xmlRequest).contains("AssertionConsumerServiceIndex=\"" +
                spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getAttributeConsumingServiceIndex() + "\"");
        }
        assertThat(xmlRequest).contains("AttributeConsumingServiceIndex=\"" +
                spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getAttributeConsumingServiceIndex() + "\"");
    }

    @Test
    @DisplayName("Verifica runtime Binding e Firma (SigAlg/Signature)")
    public void testRuntimeBindingAndSignature() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String redirectUrl = spidRequest.getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(redirectUrl).build();
        assertThat(uriComponents.getFragment()).isNull();

        String rawSamlRequest = uriComponents.getQueryParams().getFirst("SAMLRequest");
        String rawRelayState = uriComponents.getQueryParams().getFirst("RelayState");
        String rawSigAlg = uriComponents.getQueryParams().getFirst("SigAlg");
        String rawSignature = uriComponents.getQueryParams().getFirst("Signature");

        assertThat(rawSamlRequest).isNotBlank();
        assertThat(rawRelayState).isNotBlank();
        assertThat(rawSigAlg).isNotBlank();
        assertThat(rawSignature).isNotBlank();

        String sigAlg = URLDecoder.decode(rawSigAlg, StandardCharsets.UTF_8);
        String signature = URLDecoder.decode(rawSignature, StandardCharsets.UTF_8);
        String samlRequest = URLDecoder.decode(rawSamlRequest, StandardCharsets.UTF_8);

        assertThat(sigAlg).isEqualTo("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        assertThat(samlRequest).doesNotContain(" ");
        assertThat(signature).doesNotContain(" ");
    }

    @Test
    @DisplayName("Verifica runtime Issuer (EntityID del SP)")
    public void testRuntimeIssuer() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        assertThat(xmlRequest).contains("<saml2:Issuer");
        assertThat(xmlRequest).contains(">" + identityProvider.signingIdpEntityId + "</saml2:Issuer>");
        assertThat(xmlRequest).doesNotContain(">" + mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT + "</saml2:Issuer>");
    }

    @Test
    @DisplayName("Verifica runtime Livello SPID e ForceAuthn")
    public void testAuthnRequestSpidLevelAndForceAuthn() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify that the SP enforces authentication (AgID strictly requires ForceAuthn="true" for L2/L3)
        assertThat(xmlRequest).contains("ForceAuthn=\"true\"");

        // Verify that the SP explicitly requests the correct SPID security level
        assertThat(xmlRequest).contains("<saml2p:RequestedAuthnContext");
        SpidAuthnContext spidAuthnContext = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
            .getConfigMap().getAuthnContext();
        assertThat(xmlRequest).contains(spidAuthnContext.getValue());
        assertThat(spidAuthnContext).isIn(
            SpidAuthnContext.SPID_L1,
            SpidAuthnContext.SPID_L2,
            SpidAuthnContext.SPID_L3
        );
    }

    @Test
    @DisplayName("Verifica runtime NameID Policy")
    public void testAuthnRequestNameIdPolicy() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // SPID strictly requires the NameID format to be 'transient' to protect user privacy
        assertThat(xmlRequest).contains("<saml2p:NameIDPolicy");
        assertThat(xmlRequest).contains("Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"");
    }

    @Test
    @DisplayName("Verifica runtime Timestamp e Version")
    public void testAuthnRequestTimestampAndVersion() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // SPID strictly relies on the SAML 2.0 protocol version
        assertThat(xmlRequest).contains("Version=\"2.0\"");

        // Verify the IssueInstant format (Must be ISO 8601 in UTC, ending with 'Z')
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("IssueInstant=\"([^\"]+)\"").matcher(xmlRequest);
        assertThat(matcher.find()).isTrue();

        String issueInstant = matcher.group(1);
        assertThat(issueInstant).endsWith("Z");
    }

    // ==========================================================
    // TEST WITH HTTP-POST BINDING
    // ==========================================================

    @Test
    @DisplayName("Verifica runtime Binding (HTTP-POST) e Algoritmi di Firma")
    public void testRuntimeBindingPostAndSignature() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost)
            .withSession()
            .withPostBinding(true)
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        assertThat(xmlRequest).contains("<saml2p:AuthnRequest");
        assertThat(xmlRequest).contains("<ds:Signature");
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"");
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"");
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"");
    }

    @Test
    @DisplayName("Verifica runtime Certificato Binding (HTTP-POST) - AUTH_REQUEST")
    public void testRuntimeSigningCertificatePost() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost)
            .withSession()
            .withPostBinding(true)
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        String normalizedXml = xmlRequest.replaceAll("\\s+", "");

        String signingCertificate = SigningCredentialHelper.signingCredentialList(
                spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
                SigningCredentialHelper.CredentialPurpose.AUTH_REQUEST)
            .get(0).getSigningCertificate()
            .replace(BEGIN_CERT, "")
            .replace(END_CERT, "")
            .replaceAll("\\s+", "");

        // Verify that the certificate selected for signing the AuthRequest is present
        assertThat(normalizedXml).contains(signingCertificate);
    }

    @Test
    @DisplayName("Verifica runtime attributo Comparison='minimum' nel RequestedAuthnContext")
    public void testAuthnRequestComparisonMinimum() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // The system uses 'minimum' (e.g., it accepts SPID L3 even if L2 is requested),
        // which is the correct and recommended behavior for SPID AuthnContext flexibility.
        assertThat(xmlRequest).contains("<saml2p:RequestedAuthnContext Comparison=\"minimum\"");
    }

    @Test
    @DisplayName("Verifica assenza o corretta formattazione dell'attributo Consent")
    public void testAuthnRequestConsentAttribute() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // If Spring Security includes the Consent attribute, it must comply with OASIS standards.
        // Typically for SPID, it is either omitted or set to "urn:oasis:names:tc:SAML:2.0:consent:unspecified"
        if (xmlRequest.contains("Consent=")) {
            assertThat(xmlRequest).contains("Consent=\"urn:oasis:names:tc:SAML:2.0:consent:unspecified\"");
        }
    }
}
