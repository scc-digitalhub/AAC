package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for simulating SAML protocol anomalies (e.g., time constraints, missing IDs).
 * Used alongside SpidAuthnResponseNegativePathTest to ensure the SP correctly filters malformed payloads.
 */
public class SpidAuthnNegativeUtils {

    /**
     * Simulates a mismatch between AuthnRequest and Response.
     */
    public String prepareForSimulationNotValidRequestId(
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String fakeRequestId = "ARQ" + UUID.randomUUID();
        String modifiedXmlResponse = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, fakeRequestId, idpSsoUrl, assertingPartyId, entityIdSp, null);

        return ResponseUtils.createSignedSamlResponse(modifiedXmlResponse, privateKey, certificate);
    }

    /**
     * Simulates an expired SAML Assertion (NotOnOrAfter in the past).
     */
    public String prepareForSimulationNotValidNotOnOrAfter(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);
        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        String regexNotOnOrAfter = "NotOnOrAfter=\"([^\"]+)\"";
        Matcher matcher = Pattern.compile(regexNotOnOrAfter).matcher(plainXml);

        if (matcher.find()) {
            String validFutureDate = matcher.group(1);
            String expiredPastDate = Instant.now().minus(1, ChronoUnit.DAYS).toString();
            plainXml = plainXml.replace(validFutureDate, expiredPastDate);
        }

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates an Unsolicited Response attack by stripping out all InResponseTo attributes.
     */
    public String prepareForSimulationUnsolicitedResponse(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);
        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        plainXml = plainXml.replaceAll("InResponseTo=\"[^\"]*\"", "");
        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates an Issuer Mismatch by forging an unknown Identity Provider EntityID.
     */
    public String prepareForSimulationIssuerMismatch(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String fakeIssuerEntityId = "https://unregistered-rogue-idp.invalid/metadata";
        String modifiedXmlResponse = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, fakeIssuerEntityId, entityIdSp, null);

        return ResponseUtils.createSignedSamlResponse(modifiedXmlResponse, privateKey, certificate);
    }

    /**
     * Simulates a future SAML Assertion validation failure (NotBefore condition is in the future).
     */
    public String prepareForSimulationNotValidNotBeforeFuture(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);
        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        String regexNotBefore = "NotBefore=\"([^\"]+)\"";
        Matcher matcher = Pattern.compile(regexNotBefore).matcher(plainXml);

        if (matcher.find()) {
            String validDate = matcher.group(1);
            String futureDate = Instant.now().plus(1, ChronoUnit.DAYS).toString();
            plainXml = plainXml.replace(validDate, futureDate);
        }

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates a response intended for a different Service Provider (Audience Restriction failure / Token Substitution).
     */
    public String prepareForSimulationNotValidEntityId(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String privateKey,
            String certificate) throws Exception {

        String fakeEntityId = "https://hacker-service-provider.invalid/metadata";
        String modifiedXmlResponse = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, fakeEntityId, null);

        return ResponseUtils.createSignedSamlResponse(modifiedXmlResponse, privateKey, certificate);
    }

    /**
     * Simulates a Recipient Mismatch attack by directing the Destination to an invalid ACS.
     */
    public String prepareForSimulationRecipientMismatch(
            SpidRequest ctx,
            String xmlTemplate,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String fakeAcsUrl = "https://hacker-endpoint.invalid/auth/spid/sso/malicious";
        String modifiedXmlResponse = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), fakeAcsUrl, assertingPartyId, entityIdSp, null);

        return ResponseUtils.createSignedSamlResponse(modifiedXmlResponse, privateKey, certificate);
    }

    /**
     * Generates a signed SAML Response payload where the IssueInstant and NotBefore time attributes
     * are manipulated into the future by a specific offset of seconds.
     */
    public String prepareSamlResponseWithClockSkewTooFarInFuture(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        // Calculate the future timestamp based on the requested offset
        Instant futureInstant = Instant.now().plusSeconds(60);
        // Format to standard ISO-8601 UTC format used by SAML (e.g., yyyy-MM-ddTHH:mm:ss.SSSZ)
        String futureTimestamp = futureInstant.toString();

        // Expire time must always be after the issue time to avoid immediate validation crashes
        String futureNotOnOrAfter = futureInstant.plusSeconds(300).toString();

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
            xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);

        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        plainXml = plainXml
            .replaceAll("IssueInstant=\"[^\"]+\"", "IssueInstant=\"" + futureTimestamp + "\"")
            .replaceAll("NotBefore=\"[^\"]+\"", "NotBefore=\"" + futureTimestamp + "\"")
            .replaceAll("NotOnOrAfter=\"[^\"]+\"", "NotOnOrAfter=\"" + futureNotOnOrAfter + "\"");

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));

        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates a response missing the mandatory NameID within the <saml2:Subject> block.
     * The response is properly signed, but lacks the user's specific identity identifier,
     * triggering a SUBJECT_NOT_FOUND error during framework validation.
     */
    public String prepareForSimulationMissingSubject(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);
        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        // Remove ONLY the <saml2:NameID> node inside the Subject, leaving the Subject node intact.
        // This prevents OpenSAML from throwing a NullPointerException on getSubject()
        // and forces the validator to throw the specific SUBJECT_NOT_FOUND (1009) error.
        plainXml = plainXml.replaceAll("<saml2:NameID.*?>.*?</saml2:NameID>", "");

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));

        // We sign it AFTER tampering to ensure the cryptographic signature remains valid
        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates an Invalid Response (1005).
     * Removes the mandatory "ID" attribute from the root <saml2p:Response> element,
     * breaking the SAML 2.0 core protocol schema requirements.
     */
    public String prepareForSimulationInvalidResponse(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);
        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        // Strip the mandatory ID attribute from the Response tag
        plainXml = plainXml.replaceFirst(" ID=\"[^\"]+\"", "");

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates a Decryption Error (1011).
     * Replaces the plaintext Assertion with an EncryptedAssertion containing garbage cipher data.
     * The framework will attempt to decrypt it, fail, and throw a DECRYPTION_ERROR.
     */
    public String prepareForSimulationDecryptionError(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);
        String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

        // Replace the valid Assertion with a corrupted EncryptedAssertion block
        String fakeEncryptedAssertion = "<saml2:EncryptedAssertion><xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:CipherData><xenc:CipherValue>ZmFrZURhdGE=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData></saml2:EncryptedAssertion>";
        plainXml = plainXml.replaceAll("(?s)<saml2:Assertion.*?</saml2:Assertion>", fakeEncryptedAssertion);

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }

    /**
     * Simulates an Unknown Response Class (1001).
     * Alters the root element to <saml2p:LogoutResponse>. To avoid crashing the internal
     * test signer (which only accepts Responses), we return the tampered base64 directly.
     */
    public String prepareForSimulationUnknownResponseClass(
            SpidRequest ctx,
            String xmlTemplate,
            String idpSsoUrl,
            String assertingPartyId,
            String entityIdSp,
            String privateKey,
            String certificate) throws Exception {

        String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                xmlTemplate, ctx.getRequestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);

        // 1. Sign it while it's still a valid Response so the signature block is created
        String signedBase64 = ResponseUtils.createSignedSamlResponse(cleanXmlBase64, privateKey, certificate);
        String plainXml = new String(Base64.getDecoder().decode(signedBase64), StandardCharsets.UTF_8);

        // 2. AFTER signing, swap the root tags. This will break the signature AND the class.
        plainXml = plainXml.replace("<saml2p:Response", "<saml2p:LogoutResponse")
                .replace("</saml2p:Response>", "</saml2p:LogoutResponse>");

        return Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
    }
}
