package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.setup.SpidAgidAnomalyScenario;

import java.util.Set;

/**
 * Orchestrates the SPID SAML 2.0 response generation and signing flow for testing.
 * This builder simplifies test setup by dynamically building, modifying,
 * and signing mock IdP Responses, including AgID anomaly scenarios.
 */
public class SpidResponseBuilder {

    // Core dependencies
    private final String xmlResponseTemplate;
    private final String requestId;
    private String xmlResponseAgidErrorTemplate;

    /* --- SAML Entities & Configurations ---
     * Identifiers for the Audience/Issuer restrictions and the ACS URL.
     */
    private String signingIdpSsoUrl;
    private String assertingPartyEntityId;
    private String entityIdAac;
    private Set<SpidAttribute> setSpidAttributes;

    /* --- Cryptographic Materials ---
     * PEM keys required to apply AgID-compliant digital signatures.
     */
    private String idpPrivateKey;
    private String idpCertificate;

    // Internal utilities to keep the builder logic clean
    private final ResponseUtils responseUtils = new ResponseUtils();
    private final AgidAnomalyFactory anomalyFactory = new AgidAnomalyFactory();

    // Flow execution flags and state
    private boolean applySignature = false;
    private SpidAgidAnomalyScenario anomalyScenario = null;

    /**
     * Internal constructor to initialize the Builder with the required dependencies.
     * @param xmlResponseTemplate The base unsigned XML Response to modify and send.
     * @param requestId The ID of the original AuthnRequest to correlate the response.
     */
    public SpidResponseBuilder(String xmlResponseTemplate, String requestId) {
        this.xmlResponseTemplate = xmlResponseTemplate;
        this.requestId = requestId;
    }

    /**
     * Injects the Identity Provider SSO URL where the response is destined.
     * @param signingIdpSsoUrl Assertion Consumer Service (ACS) URL.
     * @return The current builder instance.
     */
    public SpidResponseBuilder withIdpConfig(String signingIdpSsoUrl) {
        this.signingIdpSsoUrl = signingIdpSsoUrl;
        return this;
    }

    /**
     * Injects the SAML Entity IDs used to validate the Issuer and Audience Restriction.
     * @param assertingPartyEntityId The EntityID of the Mock Identity Provider (Issuer).
     * @param entityIdAac The EntityID of the Service Provider (Audience).
     * @return The current builder instance.
     */
    public SpidResponseBuilder withEntityIds(String assertingPartyEntityId, String entityIdAac) {
        this.assertingPartyEntityId = assertingPartyEntityId;
        this.entityIdAac = entityIdAac;
        return this;
    }

    /**
     * Injects the X.509 cryptographic materials required to digitally sign the SAML Response.
     * @param idpPrivateKey The Base64 encoded PEM private key of the IdP.
     * @param idpCertificate The Base64 encoded PEM public certificate of the IdP.
     * @return The current builder instance.
     */
    public SpidResponseBuilder withCertificates(String idpPrivateKey, String idpCertificate) {
        this.idpPrivateKey = idpPrivateKey;
        this.idpCertificate = idpCertificate;
        return this;
    }

    /** @return The current builder instance instructed to apply a digital signature of idp mocked. */
    public SpidResponseBuilder withSignature() {
        this.applySignature = true;
        return this;
    }

    /**
     * Instructs the builder to generate a specific AgID anomaly SAML Response instead of a success one.
     * @param scenario The AgID Anomaly scenario to simulate.
     * @param xmlResponseAgidErrorTemplate The base unsigned XML Response Agid Error to modify and send.
     * @return The current builder instance.
     */
    public SpidResponseBuilder withAnomaly(SpidAgidAnomalyScenario scenario, String xmlResponseAgidErrorTemplate) {
        this.anomalyScenario = scenario;
        this.xmlResponseAgidErrorTemplate = xmlResponseAgidErrorTemplate;
        return this;
    }

    /**
     * Sets the specific collection of SPID attributes to be included in the SAML Response.
     * * @param setSpidAttributes The set of {@link SpidAttribute}s explicitly requested by the Service Provider.
     * @return The current builder instance.
     */
    public SpidResponseBuilder withSetSpidAttributes(Set<SpidAttribute> setSpidAttributes) {
        this.setSpidAttributes = setSpidAttributes;
        return this;
    }

    /**
     * Executes the requested flow to build and optionally sign the SAML Response.
     * @return The final XML string of the SAML Response.
     * @throws Exception If XML manipulation or signing fails.
     */
    public String buildResponse() throws Exception {
        String response = this.xmlResponseTemplate;

        if (this.requestId != null) {
            if (this.anomalyScenario != null) {
                // Generate an AgID Anomaly Response
                response = anomalyFactory.buildErrorSamlResponse(
                        this.xmlResponseAgidErrorTemplate,
                        this.anomalyScenario,
                        this.requestId,
                        this.signingIdpSsoUrl,
                        this.assertingPartyEntityId
                );
            } else {
                // Generate a standard Success Response
                response = responseUtils.modifyAndEncodeSamlResponse(
                        this.xmlResponseTemplate,
                        this.requestId,
                        this.signingIdpSsoUrl,
                        this.assertingPartyEntityId,
                        this.entityIdAac,
                        this.setSpidAttributes
                );
            }
        }

        // Cryptographically sign the Response if required
        if (this.applySignature) {
            response = responseUtils.createSignedSamlResponse(
                    response,
                    this.idpPrivateKey,
                    this.idpCertificate
            );
        }

        return response;
    }
}
