package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.setup.SpidRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility for simulating SAML/SPID security attacks and anomalies.
 * This class allows manipulating signed payloads to test the Service Provider's robustness.
 */
public class HackerUtils {

    // Internal utilities to keep the builder logic clean
    private final ResponseUtils responseUtils = new ResponseUtils();

    /**
     * Simulates an attack with an invalid Request ID (mismatch between AuthnRequest and Response).
     * @param xmlTemplate The base XML response template.
     * @param idpSsoUrl The mock IdP SSO endpoint.
     * @param assertingPartyId The EntityID of the IdP.
     * @param entityIdSp The EntityID of the Service Provider.
     * @param privateKey The private key for signing.
     * @param certificate The public certificate for the signature.
     * @return A signed SAML Response with a forged InResponseTo attribute.
     * @throws Exception if XML manipulation or signing fails.
     */
    public String prepareForSimulationNotValidRequestId(
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        // ATTACK: Inject a fabricated Request ID instead of the legitimate session-bound one
        String fakeRequestId = "ARQ" + UUID.randomUUID().toString();

        String modifiedXmlResponse = responseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, fakeRequestId, idpSsoUrl, assertingPartyId, entityIdSp, null);

        return responseUtils.createSignedSamlResponse(modifiedXmlResponse, privateKey, certificate);
    }

    /**
     * Simulates a response intended for a different Service Provider (Audience Restriction failure).
     * @param ctx The current SPID test context.
     * @param xmlTemplate The base XML response template.
     * @param idpSsoUrl The mock IdP SSO endpoint.
     * @param assertingPartyId The EntityID of the IdP.
     * @param privateKey The private key for signing.
     * @param certificate The public certificate for the signature.
     * @return A signed SAML Response with a malicious Audience Restriction.
     * @throws Exception if XML manipulation or signing fails.
     */
    public String prepareForSimulationNotValidEntityId(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String privateKey,
            String certificate) throws Exception {

        // ATTACK: Simulate the IdP generating the Assertion for an unauthorized or different EntityID
        String fakeEntityId = "https://hacker-service-provider.invalid/metadata";

        String modifiedXmlResponse = responseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, fakeEntityId, null);

        return responseUtils.createSignedSamlResponse(modifiedXmlResponse, privateKey, certificate);
    }

    /**
     * Simulates a security level downgrade (e.g., L2 requested, L1 returned).
     * @param ctx The current SPID test context.
     * @param xmlTemplate The base XML response template.
     * @param idpSsoUrl The mock IdP SSO endpoint.
     * @param assertingPartyId The EntityID of the IdP.
     * @param entityIdSp The EntityID of the Service Provider.
     * @param privateKey The private key for signing.
     * @param certificate The public certificate for the signature.
     * @return A signed SAML Response containing a lower AuthnContextClassRef than required.
     * @throws Exception if XML manipulation or signing fails.
     */
    public String prepareForSimulationNotValidChangeSpidLevel(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = responseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);

        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        // RAW SUBSTITUTION: The original IdP sent SpidL2, we force SpidL1.
        // This happens BEFORE signing, so the resulting cryptographic hash will be valid.
        plainXml = plainXml.replace("https://www.spid.gov.it/SpidL2", "https://www.spid.gov.it/SpidL1");

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));

        // Sign the tampered payload. The signature is valid, but the logical content (SpidL1) should trigger a security block.
        return responseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates an expired SAML Assertion (NotOnOrAfter in the past).
     * @param ctx The current SPID test context.
     * @param xmlTemplate The base XML response template.
     * @param idpSsoUrl The mock IdP SSO endpoint.
     * @param assertingPartyId The EntityID of the IdP.
     * @param entityIdSp The EntityID of the Service Provider.
     * @param privateKey The private key for signing.
     * @param certificate The public certificate for the signature.
     * @return A signed SAML Response where the NotOnOrAfter condition has already expired.
     * @throws Exception if XML manipulation or signing fails.
     */
    public String prepareForSimulationNotValidNotOnOrAfter(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = responseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);

        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        // RAW SUBSTITUTION: Change the expiration date (NotOnOrAfter).
        // Extract the current date used in the clean template and replace it with a date from yesterday.
        String regexNotOnOrAfter = "NotOnOrAfter=\"([^\"]+)\"";
        Matcher matcher = Pattern.compile(regexNotOnOrAfter).matcher(plainXml);

        if (matcher.find()) {
            String validFutureDate = matcher.group(1);
            String expiredPastDate = Instant.now().minus(1, ChronoUnit.DAYS).toString();
            // Replace all occurrences of the future date with the expired one
            plainXml = plainXml.replace(validFutureDate, expiredPastDate);
        }

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));

        // Sign the "expired" document. Again: the signature is valid, but the logical content is illegal.
        return responseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }
}
