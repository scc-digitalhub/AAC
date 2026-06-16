package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import it.smartcommunitylab.aac.spid.setupflow.SpidAgidAnomalyScenario;
import it.smartcommunitylab.aac.spid.setupflow.SpidAgidErrorContextBuilder;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequestFlow;
import it.smartcommunitylab.aac.spid.setupflow.SpidResponseBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

/**
 * Orchestrator for AgID Compliance testing.
 * Provides shared logic to execute both successful and anomalous SPID authentication flows.
 */
public class AgidUtils {

    /**
     * Executes a standard successful SAML Authentication flow.
     * @param ctx Il contesto che contiene tutti i parametri di configurazione del test
     * @throws Exception se l'esecuzione o le asserzioni falliscono.
     */
    public void executeSuccessfulSamlFlow(SpidAgidErrorContextBuilder ctx) throws Exception {

        // 1. Build the context for a successful flow
        SpidRequest spidRequest = new SpidRequestFlow(ctx.getMockMvc())
            .withEndpoints(ctx.getBaseUrlAac(), ctx.getUserDestinationUrl(), ctx.getAuthenticatePath())
            .withIdpConfig(ctx.getRegistrationId())
            .withPostBinding(ctx.getActivePostBinding())
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(ctx.getXmlTemplate(), spidRequest.getRequestId())
            .withIdpConfig(ctx.getSigningIdpSsoUrl())
            .withEntityIds(ctx.getAssertingPartyEntityId(), ctx.getEntityIdAac())
            .withCertificates(ctx.getIdpPrivateKey(), ctx.getIdpCertificate())
            .withSignature()
            .buildResponse();

        // 2. Perform the SAML POST
        MvcResult result = performSamlPost(ctx.getMockMvc(), spidRequest, response, ctx.getSigningIdpSsoUrl());

        // 3. Validation: Ensure redirection to protected resource and no exceptions
        String redirectedUrl = result.getResponse().getRedirectedUrl();
        Exception ex = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        assertThat(ex).isNull();
        assertThat(redirectedUrl).isEqualTo(ctx.getUserDestinationUrl());
    }

    /**
     * Executes an AgID Anomaly flow and retrieves the resulting security exception.
     * @param scenario Lo specifico scenario di anomalia AgID da simulare.
     * @param ctx Il contesto che contiene tutti i parametri di configurazione.
     * @return The SpidAuthenticationException captured in session.
     * @throws Exception se l'esecuzione o le asserzioni falliscono.
     */
    public SpidAuthenticationException executeAnomalyScenarioAndGetException(
            SpidAgidAnomalyScenario scenario,
            SpidAgidErrorContextBuilder ctx) throws Exception {

        // 1. Build the context for the anomaly flow
        SpidRequest spidRequest = new SpidRequestFlow(ctx.getMockMvc())
            .withEndpoints(ctx.getBaseUrlAac(), ctx.getUserDestinationUrl(), ctx.getAuthenticatePath())
            .withIdpConfig(ctx.getRegistrationId())
            .withSession()
            .executeRequest();

        String response = new SpidResponseBuilder(ctx.getXmlTemplate(), spidRequest.getRequestId())
            .withIdpConfig(ctx.getSigningIdpSsoUrl())
            .withEntityIds(ctx.getAssertingPartyEntityId(), ctx.getEntityIdAac())
            .withCertificates(ctx.getIdpPrivateKey(), ctx.getIdpCertificate())
            .withAnomaly(scenario, ctx.getXmlAgidErrorTemplate())
            .withSignature()
            .buildResponse();

        // 2. Perform the SAML POST
        MvcResult result = performSamlPost(ctx.getMockMvc(), spidRequest, response, ctx.getSigningIdpSsoUrl());

        // 3. Security Validation: Ensure user is blocked and redirected to login
        String redirectedUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).startsWith(ctx.getLoginDestinationUrl());
        assertThat(redirectedUrl).isNotEqualTo(ctx.getUserDestinationUrl());

        // 4. Extract and verify the exception
        Exception sessionEx = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionEx).isNotNull().isInstanceOf(SpidAuthenticationException.class);

        SpidAuthenticationException spidEx = (SpidAuthenticationException) sessionEx;
        assertThat(spidEx.getError()).isNotNull();

        return spidEx;
    }

    private MvcResult performSamlPost(MockMvc mockMvc, SpidRequest ctx, String samlResponse, String ssoUrl) throws Exception {
        return mockMvc.perform(post(ssoUrl)
                .secure(true)
                .param("SAMLResponse", samlResponse)
                .param("RelayState", ctx.getRelayState())
                .session(ctx.getSession())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            )
            .andExpect(status().is3xxRedirection())
            .andReturn();
    }

    public void validateSpidAnomalyTechnicalAndSystem(
            SpidAuthenticationException spidEx,
            MockMvc mockMvc,
            String loginDestinationUrl,
            MessageSource messageSource) throws Exception {

        String expectedErrorKey = "error.spid_authentication.1xxx";
        assertThat(spidEx.getMessage()).contains(String.valueOf(1000));
        validateSpidAnomaly(spidEx, expectedErrorKey, mockMvc, loginDestinationUrl, messageSource);
    }

    public void validateSpidAnomalyUser(
            SpidAgidAnomalyScenario scenario,
            SpidAuthenticationException spidEx,
            MockMvc mockMvc,
            String loginDestinationUrl,
            MessageSource messageSource) throws Exception {

        int code = Integer.parseInt(scenario.name().replace("CODE_", ""));
        String expectedErrorKey = "error.spid_authentication." + code;
        assertThat(spidEx.getMessage()).contains(String.valueOf(code));
        validateSpidAnomaly(spidEx, expectedErrorKey, mockMvc, loginDestinationUrl, messageSource);
    }

    private void validateSpidAnomaly(
            SpidAuthenticationException spidEx,
            String expectedErrorKey,
            MockMvc mockMvc,
            String loginDestinationUrl,
            MessageSource messageSource
    ) throws Exception {
        String expectedLocalizedMessage = messageSource.getMessage(expectedErrorKey, null, expectedErrorKey, Locale.ITALIAN);

        mockMvc.perform(get(loginDestinationUrl).flashAttr("error", spidEx.getErrorMessage()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(expectedLocalizedMessage)))
            .andReturn();
    }
}
