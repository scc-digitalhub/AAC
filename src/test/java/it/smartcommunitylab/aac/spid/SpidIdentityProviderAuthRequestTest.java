package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.FirstIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockMetadataIDP;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Test suite for the generation and dispatch of SPID SAML 2.0 AuthnRequests.
 * Verifies correct HTTP bindings (Redirect/POST), signatures, mandatory XML attributes...
 */
@SpringBootTest
@AutoConfigureMockMvc
// Loads the base profile ("test") and then applies SPID overrides ("test-spid")
@ActiveProfiles({"test", "test-spid"})
@EnableWireMock({
    // Setup two fixed-port WireMock servers, mapping them to their respective YAML configuration properties
    @ConfigureWireMock(port = 58838, name = "idp-server-redirect", property = "wiremock.idp.redirect.url"),
    @ConfigureWireMock(port = 58839, name = "idp-server-post", property = "wiremock.idp.post.url")
})
// Add @Transactional to clean up the DB automatically between @Test methods within this class
@Transactional
public class SpidIdentityProviderAuthRequestTest extends BaseSpidTest {

    @Autowired
    private BootstrapConfig config;

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    private WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    private WireMockServer mockIdPServerPost;

    protected UserUtils userUtils = new UserUtils();
    protected MockMetadataIDP mockMetadataIDP = new MockMetadataIDP();
    protected FirstIdentityProvider firstIdentityProvider = new FirstIdentityProvider();

    @BeforeEach
    public void setupConfigurationAndMocks() throws IOException {
        initMockMvc();
        mockMetadataIDP.preprareMockMetadata(mockIdPServerRedirect, mockIdPServerPost);

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                ConfigurableIdentityProvider idp = idps.get(0);

                firstIdentityProvider.initReamlByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);

                SpidIdentityProviderConfigMap configmap = new SpidIdentityProviderConfigMap();
                configmap.setConfiguration(idp.getConfiguration());
                firstIdentityProvider.signingIdpSigningCertificate = configmap.getSigningCredentials().get(0).getSigningCertificate()
                    .replace(BEGIN_CERT, "")
                    .replace(END_CERT, "")
                    .replace("\n", "");

                firstIdentityProvider.initRegistrationIdBinding(mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    @Test
    @DisplayName("Verifica runtime Dispatch")
    public void testSpidAuthnRequestDispatch() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Verify that SP performed
        String redirectUrl = spidRequest.getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();

        // Verify the request is routed to the correct IdP destination
        assertThat(redirectUrl).contains(mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT + SSO_PATH);

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();
        assertThat(xmlRequest).contains("saml2p:AuthnRequest");

        assertThat(xmlRequest).contains("ID=\"");
        assertThat(xmlRequest).contains("IssueInstant=\"");

        // Verify the Destination points to the IdP
        assertThat(xmlRequest).contains("Destination=\"" + mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT + SSO_PATH + "\"");
    }

    @Test
    @DisplayName("Verifica runtime default AssertionConsumerServiceIndex e AttributeConsumingServiceIndex")
    public void testSpidAuthnRequestDefaultAssertionURLAndAttribute() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify the SP is requesting the correct defaulf value. NOT SPECIFIC IN BOOTSTRAP CONFIG
        assertThat(xmlRequest).contains("AssertionConsumerServiceIndex=\"0\"");
        assertThat(xmlRequest).contains("AttributeConsumingServiceIndex=\"0\"");
    }

    @Test
    @DisplayName("Verifica runtime Binding (HTTP-Redirect)")
    public void testRuntimeBindingRedirect() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String redirectUrl = spidRequest.getRedirectedUrl();

        // Verify redirection to the IdP
        assertThat(redirectUrl).isNotNull();
        assertThat(redirectUrl).contains(mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT + SSO_PATH);

        // Verify SAML parameters are encoded in the query string
        assertThat(redirectUrl).contains("SAMLRequest=");
        assertThat(redirectUrl).contains("RelayState=");

        // In HTTP-Redirect, the signature must be a detached query parameter (covers CODE_05/07)
        assertThat(redirectUrl).contains("SigAlg=");
        assertThat(redirectUrl).contains("Signature=");
    }

    @Test
    @DisplayName("Verifica runtime Firma (SigAlg e Signature)")
    public void testRuntimeSignature() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String redirectUrl = spidRequest.getRedirectedUrl();

        assertThat(redirectUrl).contains("SigAlg=http");
        assertThat(redirectUrl).contains("Signature=");
        assertThat(redirectUrl).contains("RelayState=");
    }

    @Test
    @DisplayName("Verifica runtime Issuer (EntityID del SP)")
    public void testRuntimeIssuer() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

        assertThat(xmlRequest).contains("<saml2:Issuer");
        assertThat(xmlRequest).contains(">" + firstIdentityProvider.signingIdpEntityId + "</saml2:Issuer>");
        assertThat(xmlRequest).doesNotContain(">" + mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT + "</saml2:Issuer>");
    }

    @Test
    @DisplayName("Verifica runtime Attributi")
    public void testAuthnRequestMandatoryAttributes() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        String requestId = spidRequest.getRequestId();

        assertThat(xmlRequest).contains("ID=\"");
        assertThat(requestId).isNotBlank();
        assertThat(requestId).matches("^[a-zA-Z].*");

        assertThat(xmlRequest).contains("IssueInstant=\"");
    }

    @Test
    @DisplayName("Verifica runtime Livello SPID e ForceAuthn")
    public void testAuthnRequestSpidLevelAndForceAuthn() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

        // Verify that the SP enforces authentication (AgID strictly requires ForceAuthn="true" for L2/L3)
        assertThat(xmlRequest).contains("ForceAuthn=\"true\"");

        // Verify that the SP explicitly requests the correct SPID security level (e.g., SpidL2)
        assertThat(xmlRequest).contains("<saml2p:RequestedAuthnContext");
        assertThat(xmlRequest).contains("https://www.spid.gov.it/SpidL2");
    }

    @Test
    @DisplayName("Verifica runtime NameID Policy")
    public void testAuthnRequestNameIdPolicy() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

        // SPID strictly requires the NameID format to be 'transient' to protect user privacy
        assertThat(xmlRequest).contains("<saml2p:NameIDPolicy");
        assertThat(xmlRequest).contains("Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"");
    }

    @Test
    @DisplayName("Verifica runtime Timestamp e Version")
    public void testAuthnRequestTimestampAndVersion() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

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
    @DisplayName("Verifica runtime Algoritmi Firma (HTTP-POST)")
    public void testRuntimeSignatureAlgorithmsPost() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdPost)
            .withSession()
            .withPostBinding(true)
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

        // Verify Signature Method is RSA-SHA256 (SHA-1 is deprecated and banned by AgID)
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"");

        // Verify Digest Method is SHA-256
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"");

        // Verify Canonicalization Method is Exclusive C14N
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"");
    }

    @Test
    @DisplayName("Verifica runtime HTML Form (HTTP-POST)")
    public void testRuntimeHtmlFormStructurePost() throws Exception {
        // 1. Create an active session pre-populated with a protected resource request
        // This ensures Spring Security will automatically generate a valid RelayState
        MockHttpSession session = userUtils.createSessionWithSavedClientRequest(BASE_URL);

        // 2. Execute the SSO initialization request directly and capture the raw HTML response
        String htmlResponse = mockMvc.perform(post(BASE_URL + AUTHENTICATE_PATH + firstIdentityProvider.registrationIdPost)
            .secure(true)
            .session(session))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // 3. Verify the HTML response contains a form intended for the IdP via POST
        assertThat(htmlResponse).contains("<form");
        assertThat(htmlResponse).containsIgnoringCase("method=\"post\"");
        assertThat(htmlResponse).contains("action=\"" + mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_POST + SSO_PATH + "\"");

        // 4. Verify the presence of the mandatory hidden SAML inputs
        assertThat(htmlResponse).contains("name=\"SAMLRequest\"");
        assertThat(htmlResponse).contains("name=\"RelayState\"");

        // 5. Verify the auto-submit Javascript logic or a fallback submit button is present
        assertThat(htmlResponse).matches("(?s).*document.forms\\[0\\]\\.submit\\(\\).*|.*<input type=\"submit\".*");
    }

    @Test
    @DisplayName("Verifica runtime Binding (HTTP-POST)")
    public void testRuntimeBindingPost() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdPost)
            .withSession()
            .withPostBinding(true)
            .executeRequest();


        // In HTTP-POST, the XML signature must be embedded directly within the document
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();
        assertThat(xmlRequest).contains("<saml2p:AuthnRequest");
        assertThat(xmlRequest).contains("<ds:Signature");
    }

    @Test
    @DisplayName("Verifica runtime Certificato - AUTH_REQUEST")
    public void testRuntimeCertificate() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdPost)
            .withSession()
            .withPostBinding(true)
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

        String normalizedXml = xmlRequest.replaceAll("\\s+", "");
        String normalizedCert = firstIdentityProvider.signingIdpSigningCertificate.replaceAll("\\s+", "");

        // Verify that the certificate selected for signing the AuthRequest is present
        assertThat(normalizedXml).contains(normalizedCert);
    }

    @Test
    @DisplayName("Verifica runtime attributo Comparison='minimum' nel RequestedAuthnContext")
    public void testAuthnRequestComparisonMinimum() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

        // The system uses 'minimum' (e.g., it accepts SPID L3 even if L2 is requested),
        // which is the correct and recommended behavior for SPID AuthnContext flexibility.
        assertThat(xmlRequest).contains("<saml2p:RequestedAuthnContext Comparison=\"minimum\"");
    }

    @Test
    @DisplayName("Verifica assenza o corretta formattazione dell'attributo Consent")
    public void testAuthnRequestConsentAttribute() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(firstIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();

        // If Spring Security includes the Consent attribute, it must comply with OASIS standards.
        // Typically for SPID, it is either omitted or set to "urn:oasis:names:tc:SAML:2.0:consent:unspecified"
        if (xmlRequest.contains("Consent=")) {
            assertThat(xmlRequest).contains("Consent=\"urn:oasis:names:tc:SAML:2.0:consent:unspecified\"");
        }
    }
}
