package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SecondIdentityProvider;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockMetadataIDP;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.setupflow.SpidResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for verifying the security and validity of SPID SAML Responses
 * during the authentication phase.
 * It ensures that the Identity Provider correctly validates signatures,
 * audience restrictions, and prevents common vulnerabilities like Replay Attacks.
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
public class SpidIdentityProviderSecondProviderTest extends BaseSpidTest {

    @Autowired
    private BootstrapConfig config;

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    private WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    private WireMockServer mockIdPServerPost;

    protected MockMetadataIDP mockMetadataIDP = new MockMetadataIDP();
    protected SecondIdentityProvider secondIdentityProvider = new SecondIdentityProvider();

    @BeforeEach
    public void setupConfigurationAndMocks() throws IOException {
        initMockMvc();
        mockMetadataIDP.preprareMockMetadata(mockIdPServerRedirect, mockIdPServerPost);

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                ConfigurableIdentityProvider idp = idps.get(1);

                secondIdentityProvider.initReamlByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);
                secondIdentityProvider.initRegistrationIdBinding(mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    @Test
    @DisplayName("Autenticazione errata con SetAttribute Specifico: HTTP-POST Binding")
    public void testAuthenticationSuccedsSetAttributeWithPostBinding() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(secondIdentityProvider.registrationIdPost) // POST
            .withPostBinding(true)
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockMetadataIDP.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(secondIdentityProvider.signingIdpSsoUrl)
            .withEntityIds(mockMetadataIDP.ASSERTING_PARTY_ENTITY_ID_POST, secondIdentityProvider.signingIdpEntityId) // POST
            .withCertificates(mockMetadataIDP.IDP_VERIFICATION_PRIVATE_KEY, mockMetadataIDP.IDP_VERIFICATION_CERTIFICATE)
            .withSetSpidAttributes(secondIdentityProvider.signingSetSpidAttributes) // SPECIFIC SET ATTRIBUTE
            .withSignature()
            .buildResponse();

        this.mockMvc.perform(post(secondIdentityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL)); // spidCode - fiscalNumber MISSING
    }

    @Test
    @DisplayName("Verifica runtime AssertionConsumerServiceURL e AttributeConsumingServiceIndex")
    public void testSpidAuthnRequestAssertionURLAndAttribute() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(secondIdentityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify the SP is requesting the correct value DEFINE IN BOOTSTRAP CONFIG
        assertThat(xmlRequest).contains("AssertionConsumerServiceURL=\"" + secondIdentityProvider.signingIdpSsoUrl + "\"");
        assertThat(xmlRequest).contains("AttributeConsumingServiceIndex=\"" + secondIdentityProvider.signingAttributeConsumingServiceIndex + "\"");
    }
}
