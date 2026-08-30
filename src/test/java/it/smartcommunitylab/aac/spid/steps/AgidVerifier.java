package it.smartcommunitylab.aac.spid.steps;

import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import it.smartcommunitylab.aac.spid.model.SpidError;
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
public class AgidVerifier {

    /**
     * Executes a standard successful SAML Authentication flow.
     * @param ctx The context containing all the configuration parameters for the test.
     */
    public static String verifySuccessfulLoginFlow(SpidAgidErrorContextBuilder ctx) {
        try {
            // 1. Build the context for a successful flow
            SpidRequest spidRequest = new SpidRequestFlow(ctx.getMockMvc())
                .withEndpoints(ctx.getBaseUrlAac(), ctx.getUserDestinationUrl(), ctx.getAuthenticatePath())
                .withIdpConfig(ctx.getRegistrationId())
                .withPostBinding(ctx.getActivePostBinding())
                .withSession()
                .executeRequest();

            String response = new SpidResponseBuilder(ctx.getXmlTemplate(), spidRequest.requestId())
                .withIdpConfig(ctx.getSigningIdpSsoUrl())
                .withEntityIds(ctx.getAssertingPartyEntityId(), ctx.getEntityIdAac())
                .withCertificates(ctx.getIdpPrivateKey(), ctx.getIdpCertificate())
                .withSignature()
                .buildResponse();

            // 2. Return Redirected Url
            MvcResult result = performSamlPost(ctx.getMockMvc(), spidRequest, response, ctx.getSigningIdpSsoUrl());
            return result.getResponse().getRedirectedUrl();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute the successful SPID SAML authentication flow", e);
        }
    }

    /**
     * Executes an AgID Anomaly flow and retrieves the resulting security exception.
     * @param scenario The specific AgID anomaly scenario to simulate.
     * @param ctx The context containing all the configuration parameters.
     * @return The SpidAuthenticationException captured in session.
     */
    public static SpidAuthenticationException triggerAnomalyAndExtractException(
        SpidAgidAnomalyScenario scenario,
        SpidAgidErrorContextBuilder ctx)
    {
        try {
            // 1. Build the context for the anomaly flow
            SpidRequest spidRequest = new SpidRequestFlow(ctx.getMockMvc())
                .withEndpoints(ctx.getBaseUrlAac(), ctx.getUserDestinationUrl(), ctx.getAuthenticatePath())
                .withIdpConfig(ctx.getRegistrationId())
                .withSession()
                .executeRequest();

            String response = new SpidResponseBuilder(ctx.getXmlTemplate(), spidRequest.requestId())
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
            Exception sessionEx = (Exception) spidRequest.session().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            assertThat(sessionEx).isNotNull().isInstanceOf(SpidAuthenticationException.class);

            SpidAuthenticationException spidEx = (SpidAuthenticationException) sessionEx;
            assertThat(spidEx.getError()).isNotNull();

            return spidEx;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to execute the AgID anomaly scenario '%s' and extract the security exception", scenario.name()), e);
        }
    }

    /**
     * Simulates the Identity Provider sending the SAML Response back to the Service Provider via HTTP-POST binding.
     * Executes the POST request against the SP's Assertion Consumer Service (ACS) URL and asserts a 3xx redirection.
     *
     * @param mockMvc      The MockMvc instance used to simulate HTTP requests.
     * @param ctx          The current SPID request context containing the active session and RelayState.
     * @param samlResponse The Base64-encoded SAML Response payload.
     * @param ssoUrl       The SP endpoint URL where the SAML Response is posted.
     * @return The resulting MvcResult after a successful redirection.
     */
    private static MvcResult performSamlPost(
        MockMvc mockMvc,
        SpidRequest ctx,
        String samlResponse,
        String ssoUrl)
    {
        try {
            return mockMvc.perform(post(ssoUrl)
                    .secure(true)
                    .param("SAMLResponse", samlResponse)
                    .param("RelayState", ctx.relayState())
                    .session(ctx.session())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                )
                .andExpect(status().is3xxRedirection())
                .andReturn();
        } catch (Exception e) {
            throw new RuntimeException("Error performing the mock SAML POST request to the Service Provider", e);
        }
    }

    /**
     * Validates the handling of Technical and System SPID anomalies (typically AgID error codes in the 1000 range).
     * Ensures that the thrown exception contains the expected error code and that the UI renders the correct
     * localized error message for technical failures.
     *
     * @param spidEx              The captured SpidAuthenticationException thrown during the flow.
     * @param mockMvc             The MockMvc instance used to verify the UI rendering.
     * @param loginDestinationUrl The URL of the login page where the user is redirected upon failure.
     * @param messageSource       The Spring MessageSource used to resolve the expected localized text.
     */
    public static void verifyTechnicalAnomalyLocalization(
        SpidAuthenticationException spidEx,
        MockMvc mockMvc,
        String loginDestinationUrl,
        MessageSource messageSource)
    {
        try {
            String expectedErrorKey = "error.spid_authentication.1xxx";
            assertThat(spidEx.getMessage()).contains(SpidError.SPID_FAILED_RESPONSE_VALIDATION.getErrorCode());
            assertLocalizedErrorMessageOnLoginPage(
                spidEx,
                expectedErrorKey,
                mockMvc,
                loginDestinationUrl,
                messageSource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate the UI localization for a Technical/System SPID anomaly", e);
        }
    }

    /**
     * Validates the handling of User-specific SPID anomalies based on the simulated AgID scenario (e.g., CODE_19).
     * Extracts the numerical code from the scenario, verifies it against the exception, and checks that the
     * corresponding localized error message is properly displayed on the login page.
     *
     * @param scenario            The simulated SPID anomaly scenario enum (must follow the 'CODE_XXX' naming convention).
     * @param spidEx              The captured SpidAuthenticationException thrown during the flow.
     * @param mockMvc             The MockMvc instance used to verify the UI rendering.
     * @param loginDestinationUrl The URL of the login page where the user is redirected upon failure.
     * @param messageSource       The Spring MessageSource used to resolve the expected localized text.
     */
    public static void verifyUserAnomalyLocalization(
        SpidAgidAnomalyScenario scenario,
        SpidAuthenticationException spidEx,
        MockMvc mockMvc,
        String loginDestinationUrl,
        MessageSource messageSource)
    {
        try {
            int code = Integer.parseInt(scenario.name().replace("CODE_", ""));
            String expectedErrorKey = "error.spid_authentication." + code;
            assertThat(spidEx.getMessage()).contains(String.valueOf(code));
            assertLocalizedErrorMessageOnLoginPage(
                spidEx,
                expectedErrorKey,
                mockMvc,
                loginDestinationUrl,
                messageSource);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to validate the UI localization for the User SPID anomaly '%s'", scenario.name()), e);
        }
    }

    /**
     * Core validation helper method that simulates the user being redirected to the login page with an error.
     * It resolves the expected localized message in Italian and asserts that the rendered HTML content
     * contains this exact string via flash attributes.
     *
     * @param spidEx              The captured SpidAuthenticationException containing the error details.
     * @param expectedErrorKey    The localization properties key expected for this specific error.
     * @param mockMvc             The MockMvc instance used to perform the GET request.
     * @param loginDestinationUrl The URL of the login page to be rendered.
     * @param messageSource       The Spring MessageSource used to resolve the localized text.
     */
    private static void assertLocalizedErrorMessageOnLoginPage(
        SpidAuthenticationException spidEx,
        String expectedErrorKey,
        MockMvc mockMvc,
        String loginDestinationUrl,
        MessageSource messageSource)
    {
        try {
            String expectedLocalizedMessage = messageSource.getMessage(expectedErrorKey, null, expectedErrorKey, Locale.ITALIAN);

            mockMvc.perform(get(loginDestinationUrl).flashAttr("error", spidEx.getErrorMessage()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedLocalizedMessage)))
                .andReturn();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error verifying the expected localized error message '%s' in the login page", expectedErrorKey), e);
        }
    }
}
