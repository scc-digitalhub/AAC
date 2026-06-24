package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SigningCredentialHelper;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
public class SpidAuthnRequestTest extends BaseSpidTest {

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

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica il corretto avvio del flusso di Single Sign-On, il dispatch verso l'Identity Provider (IdP)
     * e l'integrità strutturale minima dell'elemento radice AuthnRequest.
     * AgID impone che la richiesta contenga tassativamente un identificativo univoco (ID), la data/ora
     * di generazione (IssueInstant) e l'attributo Destination coincidente con l'URL di SSO dell'IdP di
     * destinazione, per impedire attacchi di tipo man-in-the-middle e garantire la tracciabilità della richiesta.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
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

        // SPID mandates that IsPassive must NEVER be true (it should be false or omitted entirely)
        assertThat(xmlRequest).doesNotContain("IsPassive=\"true\"");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica l'inclusione e la corretta valorizzazione di default degli attributi AssertionConsumerServiceIndex
     * e AttributeConsumingServiceIndex all'interno della AuthnRequest generata.
     * AgID richiede che il Service Provider specifichi formalmente questi indici numerici anziché passare gli URL completi;
     * questo garantisce che l'Identity Provider sappia esattamente a quale endpoint inviare l'asserzione (ACS) e quale
     * set minimo di attributi utente rilasciare, basandosi unicamente sulle informazioni preventivamente censite e
     * validate nel Metadata dell'SP.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
    @Test
    @DisplayName("Verifica runtime default AssertionConsumerServiceIndex e AttributeConsumingServiceIndex")
    public void testSpidAuthnRequestDefaultAssertionURLAndAttribute() throws Exception {
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getUseAssertionConsumerServiceUrl()).isNull();
        assertThat(configmap.getAttributeConsumingServiceIndex()).isNull();

        // DEFAULT_ATTRIBUTE_CONSUMING_SERVICE_INDEX
        Integer attributeConsumingServiceIndex = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getAttributeConsumingServiceIndex();

        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Verify the SAML payload was generated...
        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        assertThat(xmlRequest).contains("AssertionConsumerServiceIndex=\"" + attributeConsumingServiceIndex + "\"");
        assertThat(xmlRequest).contains("AttributeConsumingServiceIndex=\"" + attributeConsumingServiceIndex + "\"");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Trasmissione dei messaggi (binding)" -> "Binding HTTP-Redirect".
     * Verifica la corretta applicazione del binding HTTP-Redirect e la validità dei parametri di firma nella query string.
     * AgID prescrive che per il binding HTTP-Redirect la firma digitale non sia inclusa nel documento XML, ma calcolata
     * concatenando i parametri della richiesta e passata separatamente tramite 'SigAlg' e 'Signature'. Il test garantisce
     * l'uso tassativo dell'algoritmo RSA-SHA256 e l'assenza di spazi o caratteri di formattazione non autorizzati prima
     * e dopo la decodifica, evitando il fallimento della verifica crittografica lato Identity Provider.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/trasmissione.html#binding-http-redirect">Regole Tecniche SPID - Binding HTTP-Redirect</a>
     */
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

        // Parse the generated redirection URL to isolate and inspect the query string components
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(redirectUrl).build();
        assertThat(uriComponents.getFragment()).isNull();

        String rawSamlRequest = uriComponents.getQueryParams().getFirst("SAMLRequest");
        String rawRelayState = uriComponents.getQueryParams().getFirst("RelayState");
        String rawSigAlg = uriComponents.getQueryParams().getFirst("SigAlg");
        String rawSignature = uriComponents.getQueryParams().getFirst("Signature");

        // Assert that all mandatory HTTP-Redirect parameters required by AgID are present
        assertThat(rawSamlRequest).isNotBlank();
        assertThat(rawRelayState).isNotBlank();
        assertThat(rawSigAlg).isNotBlank();
        assertThat(rawSignature).isNotBlank();

        // Decode URL components to perform strict cryptographic and structural validation
        String sigAlg = URLDecoder.decode(rawSigAlg, StandardCharsets.UTF_8);
        String signature = URLDecoder.decode(rawSignature, StandardCharsets.UTF_8);
        String samlRequest = URLDecoder.decode(rawSamlRequest, StandardCharsets.UTF_8);

        // Verify strict compliance with AgID security profiles (RSA-SHA256 signature algorithm)
        assertThat(sigAlg).isEqualTo("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");

        // Ensure the payloads are free of unencoded white spaces which would corrupt signature verification
        assertThat(samlRequest).doesNotContain(" ");
        assertThat(signature).doesNotContain(" ");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica la presenza e la corretta valorizzazione dell'elemento `<saml2:Issuer>` all'interno della AuthnRequest.
     * AgID richiede che ogni richiesta di autenticazione indichi esplicitamente l'identificatore univoco (EntityID) del
     * Service Provider mittente. Questo valore deve corrispondere esattamente all'entityID dichiarato nel metadato dell'SP,
     * consentendo all'Identity Provider di associare la richiesta al corretto profilo di trust configurato nella federazione
     * e di rifiutare tentativi di spoofing o richieste anonime.
     *
     * @see <<a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
    @Test
    @DisplayName("Verifica runtime Issuer (EntityID del SP)")
    public void testRuntimeIssuer() throws Exception {
        // Execute the SPID authentication flow to generate the runtime AuthnRequest XML
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Ensure the XML payload contains a formally valid saml2:Issuer tag structure
        assertThat(xmlRequest).contains("<saml2:Issuer");

        // Verify that the Issuer value explicitly matches the SP's EntityID and does not mimic the IdP's EntityID
        assertThat(xmlRequest).contains(">" + identityProvider.signingIdpEntityId + "</saml2:Issuer>");
        assertThat(xmlRequest).doesNotContain(">" + mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT + "</saml2:Issuer>");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica la corretta configurazione del livello di sicurezza SPID richiesto (L1/L2/L3) e l'impostazione
     * tassativa del meccanismo di autenticazione forzata (ForceAuthn).
     * AgID esige che l'attributo ForceAuthn sia impostato sempre a "true" su tutte le richieste per impedire
     * il riutilizzo di sessioni SSO precedentemente aperte lato Identity Provider, obbligando l'utente a reinserire
     * le proprie credenziali ad ogni accesso. Inoltre, verifica che il nodo RequestedAuthnContext contenga l'URI
     * esatto corrispondente al livello SPID stabilito per il servizio.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
    @Test
    @DisplayName("Verifica runtime Livello SPID e ForceAuthn")
    public void testAuthnRequestSpidLevelAndForceAuthn() throws Exception {
        // Validate that the underlying configuration maps to a valid SPID assurance level
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getAuthnContext()).isIn(
            SpidAuthnContext.SPID_L1,
            SpidAuthnContext.SPID_L2,
            SpidAuthnContext.SPID_L3
        );

        // Execute the authentication flow to capture the outbound XML payload
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // AgID strictly requires ForceAuthn="true" for ALL requests to prevent session reuse
        assertThat(xmlRequest).contains("ForceAuthn=\"true\"");

        // Verify that the SP explicitly requests the correct SPID security level context element
        assertThat(xmlRequest).contains("<saml2p:RequestedAuthnContext");
        assertThat(xmlRequest).contains(configmap.getAuthnContext().getValue());
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica la presenza e la corretta configurazione del nodo `<saml2p:NameIDPolicy>` all'interno della richiesta.
     * AgID impone l'uso tassativo del formato "transient" (urn:oasis:names:tc:SAML:2.0:nameid-format:transient)
     * per tutelare rigorosamente la privacy del cittadino. Questa impostazione prescrive all'Identity Provider di non
     * rilasciare un identificatore permanente, bensì un identificativo temporaneo, anonimo e valido per la sola sessione
     * corrente, impedendo qualsiasi attività di tracciamento o correlazione persistente delle abitudini dell'utente.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
    @Test
    @DisplayName("Verifica runtime NameID Policy")
    public void testAuthnRequestNameIdPolicy() throws Exception {
        // Execute the SPID authentication flow to generate the outbound SAML message
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify that the NameIDPolicy tag exists within the AuthnRequest
        assertThat(xmlRequest).contains("<saml2p:NameIDPolicy");

        // SPID strictly requires the NameID format to be 'transient' to protect user privacy
        assertThat(xmlRequest).contains("Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica la presenza e la corretta formattazione degli attributi "Version" e "IssueInstant" nell'elemento radice.
     * AgID impone l'adesione rigida allo standard SAML 2.0 (Version="2.0"). Inoltre, la data/ora di generazione
     * (IssueInstant) deve seguire tassativamente il formato ISO 8601 espresso in UTC (indicato dalla 'Z' finale).
     * Questo controllo garantisce la sincronizzazione temporale e la validità della richiesta all'interno della
     * federazione, permettendo all'Identity Provider di scartare messaggi obsoleti o potenziali attacchi di tipo replay.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
    @Test
    @DisplayName("Verifica runtime Timestamp e Version")
    public void testAuthnRequestTimestampAndVersion() throws Exception {
        // Execute the SPID authentication flow to trigger the generation of the AuthnRequest XML
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // SPID strictly relies on the SAML 2.0 protocol version
        assertThat(xmlRequest).contains("Version=\"2.0\"");

        // Extract and verify the IssueInstant attribute format (Must conform to ISO 8601 in UTC, ending with 'Z')
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("IssueInstant=\"([^\"]+)\"").matcher(xmlRequest);
        assertThat(matcher.find()).isTrue();

        String issueInstant = matcher.group(1);
        assertThat(issueInstant).endsWith("Z");
    }

    // ==========================================================
    // TEST WITH HTTP-POST BINDING
    // ==========================================================

    /**
     * REGOLE TECNICHE SPID: Sezione "Trasmissione dei messaggi (binding)" -> "Binding HTTP-POST".
     * Verifica la corretta applicazione del binding HTTP-POST e la conformità degli algoritmi crittografici della firma inviluppata.
     * A differenza del binding HTTP-Redirect, AgID impone che nel binding HTTP-POST la firma digitale (XML Signature) sia
     * integrata ("enveloped") direttamente all'interno del payload XML della AuthnRequest. Il test garantisce che vengano
     * utilizzati gli algoritmi di sicurezza tassativi: RSA-SHA256 per la firma, SHA-256 per l'hash (digest) e la
     * canonizzazione esclusiva (xml-exc-c14n#) per prevenire attacchi di alterazione o manipolazione del testo XML.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/trasmissione.html#binding-http-post">Regole Tecniche SPID - Binding HTTP-POST</a>
     */
    @Test
    @DisplayName("Verifica runtime Binding (HTTP-POST) e Algoritmi di Firma")
    public void testRuntimeBindingPostAndSignature() throws Exception {
        // Execute the SPID authentication flow by explicitly enforcing the HTTP-POST binding configuration
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost)
            .withSession()
            .withPostBinding(true)
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Verify that the XML payload contains a valid enveloped ds:Signature block inside the request
        assertThat(xmlRequest).contains("<saml2p:AuthnRequest");
        assertThat(xmlRequest).contains("<ds:Signature");

        // Assert strict compliance with AgID cryptographic standards (RSA-SHA256, SHA-256 digest, and Exclusive Canonicalization)
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"");
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"");
        assertThat(xmlRequest).contains("Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"");
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Trasmissione dei messaggi (binding)" -> "Binding HTTP-POST".
     * Verifica che il certificato pubblico X.509 incluso nel blocco `<ds:KeyInfo>` della AuthnRequest corrisponda
     * esattamente a quello configurato per la firma delle richieste (AUTH_REQUEST).
     * AgID impone l'esplicita inclusione del certificato nel payload firmato tramite HTTP-POST; in questo modo
     * l'Identity Provider ricevente può estrarre la chiave pubblica direttamente dal messaggio e validare istantaneamente
     * l'integrità e l'autenticità del mittente, accertando che la richiesta provenga da un Service Provider legittimo.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/trasmissione.html#binding-http-post">Regole Tecniche SPID - Binding HTTP-POST</a>
     */
    @Test
    @DisplayName("Verifica runtime Certificato Binding (HTTP-POST) - AUTH_REQUEST")
    public void testRuntimeSigningCertificatePost() throws Exception {
        // Execute the authentication request flow using the HTTP-POST binding
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdPost)
            .withSession()
            .withPostBinding(true)
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Normalize the XML string by removing all whitespace and formatting characters to avoid comparison mismatches
        String normalizedXml = xmlRequest.replaceAll("\\s+", "");

        // Retrieve and clean the expected signing certificate from the configuration repository
        String signingCertificate = SigningCredentialHelper.signingCredentialList(
                spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
                SigningCredentialHelper.CredentialPurpose.AUTH_REQUEST)
            .get(0).getSigningCertificate()
            .replace(BEGIN_CERT, "")
            .replace(END_CERT, "")
            .replaceAll("\\s+", "");

        // Verify that the certificate selected for signing the AuthRequest is embedded in the XML payload
        assertThat(normalizedXml).contains(signingCertificate);
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica che l'attributo Comparison nel RequestedAuthnContext sia impostato su "minimum".
     * In conformità con gli esempi ufficiali forniti da AgID, il valore "minimum" indica all'Identity
     * Provider che il servizio richiede un livello di sicurezza di partenza (es. SpidL2), ma consente
     * all'IdP di elevare l'autenticazione a un livello superiore (es. SpidL3) qualora l'utente lo utilizzi,
     * garantendo massima flessibilità senza compromettere i requisiti minimi di accesso del servizio.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
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

    /**
     * REGOLE TECNICHE SPID: Sezione "Richiesta di Autenticazione (AuthnRequest)".
     * Verifica l'assenza o la corretta valorizzazione condizionale dell'attributo "Consent" nell'elemento radice AuthnRequest.
     * Nello scenario SPID, la gestione del consenso è demandata all'Identity Provider durante l'interazione con l'utente o regolata
     * a monte dall'infrastruttura di trust. Qualora l'infrastruttura o il framework SAML sottostante (es. Spring Security) decida di
     * includere esplicitamente questo attributo opzionale, esso deve conformarsi tassativamente agli standard OASIS ed essere valorizzato
     * come "unspecified", evitando stringhe personalizzate che comprometterebbero l'esito dei controlli formali di AgID.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#authnrequest">Regole Tecniche SPID - AuthnRequest</a>
     */
    @Test
    @DisplayName("Verifica assenza o corretta formattazione dell'attributo Consent")
    public void testAuthnRequestConsentAttribute() throws Exception {
        // Trigger the outbound SPID authentication request flow
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        String xmlRequest = spidRequest.getXmlRequest();
        assertThat(xmlRequest).isNotNull();

        // Validate the Consent attribute format only if the underlying framework explicitly includes it.
        // For SPID compliance, it should either be omitted completely or set to the standard 'unspecified' URI.
        if (xmlRequest.contains("Consent=")) {
            assertThat(xmlRequest).contains("Consent=\"urn:oasis:names:tc:SAML:2.0:consent:unspecified\"");
        }
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Trasmissione dei messaggi (binding)" -> "Binding HTTP-POST".
     * Verifica la corretta generazione e la struttura formale della pagina HTML Form utilizzata per il binding HTTP-POST.
     * AgID prescrive che, quando si adotta il binding HTTP-POST, la trasmissione del messaggio avvenga tramite una form HTML
     * recapitata al browser dell'utente (User Agent) e solitamente sottomessa in modo automatico via JavaScript. Il test garantisce
     * che la form punti all'URL Single Sign-On esatto dell'Identity Provider impostato come "action", utilizzi rigorosamente il
     * metodo POST e contenga i parametri nascosti obbligatori "SAMLRequest" (la richiesta SAML codificata) e "RelayState"
     * (lo stato della sessione), indispensabili per la continuità del flusso di trust.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/trasmissione.html#binding-http-post">Regole Tecniche SPID - Binding HTTP-POST</a>
     */
    @Test
    @DisplayName("Verifica runtime HTML Form (HTTP-POST)")
    public void testRuntimeHtmlFormStructurePost() throws Exception {
        // 1. Create an active session pre-populated with a protected resource request
        // This ensures Spring Security will automatically generate a valid RelayState
        UserUtils userUtils = new UserUtils();
        MockHttpSession session = userUtils.createSessionWithSavedClientRequest(BASE_URL);

        // 2. Execute the SSO initialization request directly and capture the raw HTML response
        String htmlRequest = mockMvc.perform(post(BASE_URL + AUTHENTICATE_PATH + identityProvider.registrationIdPost)
                .secure(true)
                .session(session))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // 3. Verify the formal structure of the DOM (View)
        assertThat(htmlRequest).isNotBlank();
        assertThat(htmlRequest).contains("<form");
        assertThat(htmlRequest).containsIgnoringCase("method=\"post\"");

        // Retrieve the expected IdP Single Sign-On URL location from the configuration repository
        String action = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
            .getRelyingPartyRegistration(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST)
            .getAssertingPartyDetails()
            .getSingleSignOnServiceLocation();

        // Assert that the HTML form targets the exact SSO endpoint location of the Asserting Party
        assertThat(htmlRequest).contains("action=\"" + action + "\"");

        // 4. Verify the presence of the mandatory hidden inputs required by the IdP for message processing
        assertThat(htmlRequest).contains("name=\"SAMLRequest\"");
        assertThat(htmlRequest).contains("name=\"RelayState\"");
    }
}
