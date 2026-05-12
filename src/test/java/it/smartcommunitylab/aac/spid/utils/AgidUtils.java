package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import it.smartcommunitylab.aac.spid.setup.SpidAgidAnomalyScenario;
import it.smartcommunitylab.aac.spid.setup.SpidRequest;
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
     * * @param mockMvc The Spring MockMvc instance.
     * @param xmlTemplate The base XML response template.
     * @param baseUrlAac Base URL of the Service Provider.
     * @param userDestinationUrl Protected resource URL (Success destination).
     * @param authenticatePath Path to initiate the SPID auth flow.
     * @param signingIdpSsoUrl Assertion Consumer Service (ACS) URL to POST the response.
     * @param registrationId The internal AAC registration ID.
     * @param assertingPartyEntityId The EntityID of the IdP.
     * @param entityIdAac The EntityID of the Service Provider.
     * @param idpPrivateKey The private key for signing.
     * @param idpCertificate The public certificate for the signature.
     * @param activePostBinding The protocol Binding.
     * @throws Exception if flow execution or assertions fail.
     */
    public void executeSuccessfulSamlFlow(
            MockMvc mockMvc,
            String xmlTemplate,
            String baseUrlAac,
            String userDestinationUrl,
            String authenticatePath,
            String signingIdpSsoUrl,
            String registrationId,
            String assertingPartyEntityId,
            String entityIdAac,
            String idpPrivateKey,
            String idpCertificate,
            Boolean activePostBinding) throws Exception {

        // 1. Build the context for a successful flow
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
                .withEndpoints(baseUrlAac, userDestinationUrl, authenticatePath)
                .withIdpConfig(registrationId)
                .withPostBinding(activePostBinding)
                .withSession()
                .executeRequest();

        String response = new SpidResponseBuilder(xmlTemplate, spidRequest.getRequestId())
                .withIdpConfig(signingIdpSsoUrl)
                .withEntityIds(assertingPartyEntityId, entityIdAac)
                .withCertificates(idpPrivateKey, idpCertificate)
                .withSignature()
                .buildResponse();

        // 2. Perform the SAML POST
        MvcResult result = performSamlPost(mockMvc, spidRequest, response, signingIdpSsoUrl);

        // 3. Validation: Ensure redirection to protected resource and no exceptions
        String redirectedUrl = result.getResponse().getRedirectedUrl();
        Exception ex = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        assertThat(ex).isNull();
        assertThat(redirectedUrl).isEqualTo(userDestinationUrl);
    }

    /**
     * Executes an AgID Anomaly flow and retrieves the resulting security exception.
     * * @param scenario The specific AgID anomaly scenario to simulate.
     * @param mockMvc The Spring MockMvc instance.
     * @param xmlTemplate The base XML response template.
     * @param xmlAgidErrorTemplate The base XML Error response template.
     * @param baseUrlAac Base URL of the Service Provider.
     * @param userDestinationUrl Protected resource URL.
     * @param authenticatePath Path to initiate the SPID auth flow.
     * @param loginDestinationUrl Redirection URL in case of failure.
     * @param signingIdpSsoUrl Assertion Consumer Service (ACS) URL to POST the response.
     * @param registrationId The internal AAC registration ID.
     * @param assertingPartyEntityId The EntityID of the IdP.
     * @param entityIdAac The EntityID of the Service Provider.
     * @param idpPrivateKey The private key for signing.
     * @param idpCertificate The public certificate for the signature.
     * @return The SpidAuthenticationException captured in session.
     * @throws Exception if flow execution or assertions fail.
     */
    public SpidAuthenticationException executeAnomalyScenarioAndGetException(
            SpidAgidAnomalyScenario scenario,
            MockMvc mockMvc,
            String xmlTemplate,
            String xmlAgidErrorTemplate,
            String baseUrlAac,
            String userDestinationUrl,
            String authenticatePath,
            String loginDestinationUrl,
            String signingIdpSsoUrl,
            String registrationId,
            String assertingPartyEntityId,
            String entityIdAac,
            String idpPrivateKey,
            String idpCertificate) throws Exception {

        // 1. Build the context for the anomaly flow
        SpidRequest spidRequest = new SpidRequestFlow(mockMvc)
                .withEndpoints(baseUrlAac, userDestinationUrl, authenticatePath)
                .withIdpConfig(registrationId)
                .withSession()
                .executeRequest();

        String response = new SpidResponseBuilder(xmlTemplate, spidRequest.getRequestId())
                .withIdpConfig(signingIdpSsoUrl)
                .withEntityIds(assertingPartyEntityId, entityIdAac)
                .withCertificates(idpPrivateKey, idpCertificate)
                .withAnomaly(scenario, xmlAgidErrorTemplate)
                .withSignature()
                .buildResponse();

        // 2. Perform the SAML POST
        MvcResult result = performSamlPost(mockMvc, spidRequest, response, signingIdpSsoUrl);

        // 3. Security Validation: Ensure user is blocked and redirected to login
        String redirectedUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).startsWith(loginDestinationUrl);
        assertThat(redirectedUrl).isNotEqualTo(userDestinationUrl);

        // 4. Extract and verify the exception
        Exception sessionEx = (Exception) spidRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        assertThat(sessionEx).isNotNull().isInstanceOf(SpidAuthenticationException.class);

        SpidAuthenticationException spidEx = (SpidAuthenticationException) sessionEx;
        assertThat(spidEx.getError()).isNotNull();

        return spidEx;
    }

    /**
     * Internal helper to perform the SAML Post to the Service Provider.
     * Consolidates the MockMvc call logic for code reuse.
     */
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

    /**
     * Validates both the backend exception and the frontend rendering of the SPID error page.
     * Ensures that the correct anomaly code is extracted, propagated, and displayed to the technic and system
     * in accordance with AgID UI/UX guidelines.
     * * @param scenario The specific AgID anomaly scenario being tested.
     * @param spidEx The exception captured during the SAML flow.
     * @param mockMvc The Spring MockMvc instance.
     * @param loginDestinationUrl The login page URL where the user is redirected upon failure.
     * @param messageSource The Spring MessageSource used to resolve the expected localized error message.
     * @throws Exception if the MockMvc request fails or assertions do not match.
     */
    public void validateSpidAnomalyTechnicalAndSystem(
            SpidAgidAnomalyScenario scenario,
            SpidAuthenticationException spidEx,
            MockMvc mockMvc,
            String loginDestinationUrl,
            MessageSource messageSource) throws Exception {

        // Generic error
        String expectedErrorKey = "error.spid_authentication.1xxx";

        // 1. Backend Validation: Verify the exception contains the exact anomaly code
        assertThat(spidEx.getMessage()).contains(String.valueOf(1000));

        validateSpidAnomaly(spidEx, expectedErrorKey, mockMvc, loginDestinationUrl, messageSource);
    }

    /**
     * Validates both the backend exception and the frontend rendering of the SPID error page.
     * Ensures that the correct anomaly code is extracted, propagated, and displayed to the user
     * in accordance with AgID UI/UX guidelines.
     * * @param scenario The specific AgID anomaly scenario being tested.
     * @param spidEx The exception captured during the SAML flow.
     * @param mockMvc The Spring MockMvc instance.
     * @param loginDestinationUrl The login page URL where the user is redirected upon failure.
     * @param messageSource The Spring MessageSource used to resolve the expected localized error message.
     * @throws Exception if the MockMvc request fails or assertions do not match.
     */
    public void validateSpidAnomalyUser(
            SpidAgidAnomalyScenario scenario,
            SpidAuthenticationException spidEx,
            MockMvc mockMvc,
            String loginDestinationUrl,
            MessageSource messageSource) throws Exception {

        // Extract the numeric code from the scenario name (e.g., "CODE_08" -> 8)
        int code = Integer.parseInt(scenario.name().replace("CODE_", ""));
        String expectedErrorKey = "error.spid_authentication." + code;

        // 1. Backend Validation: Verify the exception contains the exact anomaly code
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

        // 2. Frontend Validation & HTML Extraction: Verify the login page correctly renders the SPID anomaly banner
        mockMvc.perform(get(loginDestinationUrl).flashAttr("error", spidEx.getErrorMessage()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedLocalizedMessage)))
                .andReturn();
    }
}
