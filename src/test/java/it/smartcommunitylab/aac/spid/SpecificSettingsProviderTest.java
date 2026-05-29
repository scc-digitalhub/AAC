package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.setupflow.SpidResponseBuilder;
import it.smartcommunitylab.aac.spid.utils.MetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for verifying the behavior of the SPID Identity Provider under specific or edge-case configuration overrides.
 * Checks the correct handling of missing mandatory attributes (e.g., fiscalNumber/spidCode) leading to authentication failures,
 * and verifies custom setups for AssertionConsumerServiceURL and AttributeConsumingServiceIndex.
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
public class SpecificSettingsProviderTest extends BaseSpidTest {

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected MetadataUtils metadataUtils = new MetadataUtils();
    protected MockIdpSpid mockIdpSpid = new MockIdpSpid();
    protected IdentityProvider identityProvider = new IdentityProvider();

    @BeforeEach
    public void setupConfigurationAndMocks() throws IOException {
        initMockMvc();
        mockIdpSpid.preprareMockMetadata(mockIdPServerRedirect, mockIdPServerPost);

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();

                // SPECIFIC SETTING
                ConfigurableIdentityProvider idp = idps.get(2);

                identityProvider.initReamlByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);
                identityProvider.initRegistrationIdBinding(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    @Test
    @DisplayName("Autenticazione Fallita con SetAttribute Specifico")
    public void testAuthenticationFailsWithSetAttributeSpecified() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost) // POST
            .withPostBinding(true)
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST, identityProvider.signingIdpEntityId) // POST
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSetSpidAttributes(spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
                .getConfigMap().getSpidAttributes()) // SPECIFIC SET ATTRIBUTE
            .withSignature()
            .buildResponse();

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
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
    @DisplayName("Verifica che gli attributi SPID specifici nel Metadata")
    public void testRequestedSpidAttributesAreSpecified() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<AttributeConsumingService> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getAttributeConsumingServices();

        assertThat(keyDescriptors.size()).isEqualTo(1);

        Set<SpidAttribute> attributes = new HashSet<>();
        for(RequestedAttribute attribute: keyDescriptors.get(0).getRequestedAttributes()){
            attributes.add(SpidAttribute.parse(attribute.getName()));
        }

        assertThat(attributes).isEqualTo(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap().getSpidAttributes()
        );
    }

    @Test
    @DisplayName("Verifica runtime AssertionConsumerServiceURL e AttributeConsumingServiceIndex Specifici")
    public void testAuthnRequestWithAssertionURLAndAttributeIndexSpecified() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify the SP is requesting the correct value SPECIFIED
        assertThat(xmlRequest).contains("AssertionConsumerServiceURL=\"" + identityProvider.signingIdpSsoUrl + "\"");
        assertThat(xmlRequest).contains("AttributeConsumingServiceIndex=\"" +
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getAttributeConsumingServiceIndex() + "\"");
    }
}
