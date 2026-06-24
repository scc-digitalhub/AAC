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
import org.springframework.security.web.WebAttributes;
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

    /**
     * REGOLE TECNICHE SPID: Sezione "Attributi" e "Ricezione delle risposte (SAML Response)".
     * Test di Sicurezza e Riconciliazione: Verifica che l'autenticazione fallisca se l'asserzione restituita
     * dall'Identity Provider manca degli attributi minimi richiesti dal Service Provider per l'identificazione univoca dell'utente.
     * Nello scenario SPID, sebbene l'IdP firmi e trasmetta un set personalizzato di attributi concordato, il modulo di controllo
     * del Service Provider deve invalidare il flusso qualora manchino i dati vitali per il provisioning o l'accoppiamento dell'account locale
     * (es. 'spidCode' o 'fiscalNumber'). Il test garantisce che l'assenza di tali identificativi provochi il rifiuto della sessione
     * e il reindirizzamento protetto verso la pagina di errore del login.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/attributi.html">Regole Tecniche SPID - Attributi e Riconciliazione</a>
     */
    @Test
    @DisplayName("Autenticazione Fallita con SPID_CODE e FISCAL_NUMBER mancanti")
    public void testAuthenticationFailsWithSetAttributeCustom() throws Exception {
        // Validate that the underlying custom configuration map is populated but explicitly lacks vital identifiers
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getSpidAttributes()).isNotEmpty();
        assertThat(configmap.getSpidAttributes())
            .doesNotContain(SpidAttribute.SPID_CODE, SpidAttribute.FISCAL_NUMBER);

        // Execute the outbound authentication request enforcing the HTTP-POST binding
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost) // POST
            .withPostBinding(true)
            .withSession()
            .executeRequest();

        // Build the inbound SAML Response containing the custom attributes, deliberately omitting spidCode and fiscalNumber
        String response = new SpidResponseBuilder(mockIdpSpid.XML_RESPONSE_TEMPLATE, spidRequest.getRequestId())
            .withIdpConfig(identityProvider.signingIdpSsoUrl)
            .withEntityIds(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST, identityProvider.signingIdpEntityId) // POST
            .withCertificates(mockIdpSpid.IDP_MOCK_PRIVATE_KEY, mockIdpSpid.IDP_MOCK_CERTIFICATE)
            .withSetSpidAttributes(spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
                .getConfigMap().getSpidAttributes()) // CUSTOM SET ATTRIBUTE
            .withSignature()
            .buildResponse();

        // Dispatch the incomplete assertion payload to the ACS listener and assert proper authentication rejection
        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL)); // spidCode - fiscalNumber MISSING

        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica la generazione della AuthnRequest quando il Service Provider è configurato per trasmettere l'URL esplicito
     * dell'Assertion Consumer Service (AssertionConsumerServiceURL) in combinazione con un indice di attributi personalizzato.
     * Sebbene AgID definisca l'uso esplicito dell'URL come "scelta sconsigliata" rispetto all'indice posizionale (Index), esso
     * rimane pienamente conforme alle specifiche. Il test garantisce che l'autenticatore popoli l'attributo con l'endpoint esatto
     * dell'SP e includa contemporaneamente l'indice del set di attributi richiesto, coprendo interamente questo ramo logico.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
    @Test
    @DisplayName("Verifica runtime AssertionConsumerServiceURL e AttributeConsumingServiceIndex Custom")
    public void testAuthnRequestWithAssertionURLAndAttributeIndexCustom() throws Exception {
        // Verify that the database configuration overrides are active for explicit ACS URL and Custom Attribute Index
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getUseAssertionConsumerServiceUrl()).isNotNull();
        assertThat(configmap.getAttributeConsumingServiceIndex()).isNotNull();
        assertThat(configmap.getUseAssertionConsumerServiceUrl()).isTrue();

        Integer attributeConsumingServiceIndex = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getAttributeConsumingServiceIndex();

        // Execute the outbound authentication request flow
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify that the outbound XML requests the explicit SP AssertionConsumerServiceURL matching your application endpoint
        assertThat(xmlRequest).contains("AssertionConsumerServiceURL=\"" + identityProvider.signingIdpSsoUrl + "\"");

        // Assert that the custom AttributeConsumingServiceIndex configured is correctly injected into the payload
        assertThat(xmlRequest).contains("AttributeConsumingServiceIndex=\"" + attributeConsumingServiceIndex + "\"");
    }
}
