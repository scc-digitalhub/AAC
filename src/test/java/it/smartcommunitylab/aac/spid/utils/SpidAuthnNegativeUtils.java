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
}
