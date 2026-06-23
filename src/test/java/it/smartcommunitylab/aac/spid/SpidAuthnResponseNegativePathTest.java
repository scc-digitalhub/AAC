package it.smartcommunitylab.aac.spid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.setup.MockIdpSpid;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.utils.SpidAuthnNegativeUtils;
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
 * Test suite for validating the SAML protocol compliance of the Service Provider.
 * Verifies that the SP correctly filters out malformed, incorrectly signed, unsolicited, or expired SAML Responses.
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
public class SpidAuthnResponseNegativePathTest extends BaseSpidTest {

    // Inject Redirect WireMock
    @InjectWireMock("idp-server-redirect")
    protected WireMockServer mockIdPServerRedirect;

    // Inject Post WireMock
    @InjectWireMock("idp-server-post")
    protected WireMockServer mockIdPServerPost;

    protected SpidAuthnNegativeUtils spidAuthnNegativeUtils = new SpidAuthnNegativeUtils();
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
                ConfigurableIdentityProvider idp = idps.get(3);

                identityProvider.initRealmByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);
                identityProvider.initRegistrationIdBinding(mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT, mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_POST);
            }
        });
    }

    /**
     * Security Test: Ensures authentication fails if the 'InResponseTo' attribute
     * in the SAML Response does not match the ID of the original AuthnRequest.
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per InResponseTo errato (Risposta non corrispondente alla richiesta)")
    public void testAuthenticationFailsOnInvalidInResponseTo() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // InResponseTo errato (Mismatch Request ID)
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidRequestId(
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));
    }

    /**
     * Security Test: Ensures authentication fails if the SAML Response is unsolicited
     * (missing the InResponseTo attribute that links it to a specific AuthnRequest).
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Unsolicited Response (InResponseTo assente)")
    public void testAuthenticationFailsOnUnsolicitedResponse() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Generates a payload without InResponseTo
        String response = spidAuthnNegativeUtils.prepareForSimulationUnsolicitedResponse(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
    }

    /**
     * Security Test: Ensures authentication fails if the SAML Response Issuer
     * does not match any registered Identity Provider entity ID.
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Issuer Mismatch (EntityID IdP non registrato)")
    public void testAuthenticationFailsOnIssuerMismatch() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Generates a signed payload but originating from an unknown Issuer
        String response = spidAuthnNegativeUtils.prepareForSimulationIssuerMismatch(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
    }

    /**
     * Security Test: Ensures authentication fails if the SAML Assertion
     * has expired (NotOnOrAfter condition is in the past).
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Assertion scaduta")
    public void testAuthenticationFailsOnExpiredAssertion() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Expired Assertion (NotOnOrAfter in the past)
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidNotOnOrAfter(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // Verify that Spring Security rejected the Assertion due to time restrictions
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("is no longer valid");
    }

    /**
     * Security Test: Ensures authentication fails if the Assertion Conditions
     * specify a NotBefore timestamp that is in the future.
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per NotBefore futuro (Assertion non ancora valida)")
    public void testAuthenticationFailsOnNotBeforeFuture() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Generates a response where the Assertion's validity begins in the future
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidNotBeforeFuture(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        // Verifies that Spring Security has raised a time-related authentication exception
        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
        assertThat(sessionException.getMessage()).containsIgnoringCase("is not yet valid");
    }

    /**
     * Security Test: Ensures authentication fails if the SAML Response is intended
     * for a different Service Provider (Audience Restriction validation).
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Audience errata (Risposta destinata a un altro Service Provider)")
    public void testAuthenticationFailsOnInvalidAudience() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Audience errata (Invalid EntityID - Token Substitution)
        String response = spidAuthnNegativeUtils.prepareForSimulationNotValidEntityId(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            identityProvider.signingIdpSsoUrl,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));
    }

    /**
     * Security Test: Ensures authentication fails if the SAML Response Destination
     * does not match the actual ACS endpoint of the Service Provider.
     */
    @Test
    @DisplayName("Protocollo: Fallimento atteso per Recipient Mismatch (Destination errato)")
    public void testAuthenticationFailsOnRecipientMismatch() throws Exception {
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
            .withEndpoints(BASE_URL, USER_DESTINATION_URL, AUTHENTICATE_PATH)
            .withIdpConfig(identityProvider.registrationIdRedirect)
            .withSession()
            .executeRequest();

        // Generates a payload with a fictitious Destination (Recipient Mismatch)
        String response = spidAuthnNegativeUtils.prepareForSimulationRecipientMismatch(
            spidRequest,
            mockIdpSpid.XML_RESPONSE_TEMPLATE,
            mockIdpSpid.ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            identityProvider.signingIdpEntityId,
            mockIdpSpid.IDP_MOCK_PRIVATE_KEY,
            mockIdpSpid.IDP_MOCK_CERTIFICATE
        );

        this.mockMvc.perform(post(identityProvider.signingIdpSsoUrl)
                .secure(true)
                .param("SAMLResponse", response)
                .param("RelayState", spidRequest.getRelayState())
                .session(spidRequest.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(LOGIN_DESTINATION_URL));

        Exception sessionException = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionException).isNotNull();
    }
}
