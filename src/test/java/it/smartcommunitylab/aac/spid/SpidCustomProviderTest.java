package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.setupflow.SpidResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for verifying the behavior of the SPID Identity Provider under custom or edge-case configuration overrides.
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
public class SpidCustomProviderTest extends BaseSpidTest {

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

                ConfigurableIdentityProvider idpCustom = idps.stream().filter(
                    idp -> "spid-test-custom".equals(idp.getName()))
                    .findFirst().orElseThrow();

                identityProvider.initRealmByBoostrap(idpCustom, BASE_URL, METADATA_PATH, SSO_PATH);
                identityProvider.initRegistrationIdBinding(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    @Test
    @DisplayName("Autenticazione Fallita con SetAttribute Custom")
    public void testAuthenticationFailsWithSetAttributeCustom() throws Exception {
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getSpidAttributes()).isNotEmpty();
        assertThat(configmap.getSpidAttributes())
            .doesNotContain(SpidAttribute.SPID_CODE, SpidAttribute.FISCAL_NUMBER);

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
                .getConfigMap().getSpidAttributes()) // CUSTOM SET ATTRIBUTE
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
    @DisplayName("Verifica runtime AssertionConsumerServiceURL e AttributeConsumingServiceIndex Custom")
    public void testAuthnRequestWithAssertionURLAndAttributeIndexCustom() throws Exception {
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getUseAssertionConsumerServiceUrl()).isNotNull();
        assertThat(configmap.getAttributeConsumingServiceIndex()).isNotNull();
        assertThat(configmap.getUseAssertionConsumerServiceUrl()).isTrue();

        Integer attributeConsumingServiceIndex = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getAttributeConsumingServiceIndex();

        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify the SP is requesting the correct value custom
        assertThat(xmlRequest).contains("AssertionConsumerServiceURL=\"" + identityProvider.signingIdpSsoUrl + "\"");
        assertThat(xmlRequest).contains("AttributeConsumingServiceIndex=\"" + attributeConsumingServiceIndex + "\"");
    }
}
