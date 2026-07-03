package it.smartcommunitylab.aac.spid.steps;

import it.smartcommunitylab.aac.spid.SpidKeys;
import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;
import it.smartcommunitylab.aac.spid.utils.ResponseUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Utility for simulating active SAML/SPID security attacks.
 * Used alongside SpidSecurityAttacksTest to test the SP's resilience against malicious tampering.
 */
public class SpidAttackSimulator {

    /**
     * Simulates a security level downgrade (e.g., L2 requested, L1 returned) - Privilege Downgrade Attack.
     */
    public static String simulatePrivilegeDowngrade(
        SpidRequest ctx,
        String xmlTemplate,
        String idpSsoUrl,
        String assertingPartyId,
        String entityIdSp,
        String privateKey,
        String certificate)
    {
        try {
            String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                    xmlTemplate, ctx.requestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);
            String plainXml = new String(Base64.getDecoder().decode(cleanXmlBase64), StandardCharsets.UTF_8);

            // Force SpidL1
            plainXml = plainXml.replace("https://www.spid.gov.it/SpidL2", "https://www.spid.gov.it/SpidL1");

            String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
            return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to simulate a SPID privilege downgrade attack (L2 to L1)", e);
        }
    }

    /**
     * Simulates an Invalid Signature (Man-In-The-Middle attack / Data Tampering).
     * Creates a perfectly valid and signed response, then tampers with the XML payload
     * without resigning it, breaking the cryptographic digest.
     */
    public static String simulateSignatureTampering(
        SpidRequest ctx,
        String xmlTemplate,
        String idpSsoUrl,
        String assertingPartyId,
        String entityIdSp,
        String privateKey,
        String certificate)
    {
        try {
            // 1. Generate a perfectly valid payload
            String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                    xmlTemplate, ctx.requestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);

            // 2. SIGN the valid payload (Cryptographic digest is calculated and appended here)
            String legitimatelySignedBase64 = ResponseUtils.createSignedSamlResponse(cleanXmlBase64, privateKey, certificate);

            // 3. DECODE the signed payload to tamper with it
            String plainXml = new String(Base64.getDecoder().decode(legitimatelySignedBase64), StandardCharsets.UTF_8);

            // 4. TAMPER with the data (Break the digest but keep XML structurally valid)
            // We replace the valid IssueInstant with another perfectly formatted, but different, dateTime string.
            String tamperedXml = plainXml.replaceFirst("IssueInstant=\"[^\"]+\"", "IssueInstant=\"2099-12-31T23:59:59.999Z\"");

            // 5. Re-encode the tampered payload WITHOUT resigning it. The signature validation will now fail.
            return Base64.getEncoder().encodeToString(tamperedXml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to simulate a SAML data tampering and invalid signature attack", e);
        }
    }

    /**
     * Simulates a classic XML Signature Wrapping (XSW) attack.
     */
    public static String simulateXmlSignatureWrapping(
        SpidRequest ctx,
        String xmlTemplate,
        String idpSsoUrl,
        String assertingPartyId,
        String entityIdSp,
        String privateKey,
        String certificate)
    {
        try {
            // 1. Generate a perfectly valid payload
            String cleanXmlBase64 = ResponseUtils.modifyAndEncodeSamlResponse(
                    xmlTemplate, ctx.requestId(), idpSsoUrl, assertingPartyId, entityIdSp, null);

            // 2. SIGN the valid payload (the genuine assertion is now covered by a valid signature)
            String legitimatelySignedBase64 = ResponseUtils.createSignedSamlResponse(cleanXmlBase64, privateKey, certificate);

            // 3. DECODE the signed payload to wrap it
            String signedXml = new String(Base64.getDecoder().decode(legitimatelySignedBase64), StandardCharsets.UTF_8);

            // 4. Build the forged, UNSIGNED assertion from the template
            String now = Instant.now().toString();
            String notOnOrAfter = Instant.now().plusSeconds(SpidKeys.SPID_CLOCK_SKEW * 10).toString();
            String forgedAssertion = FORGED_ASSERTION_TEMPLATE
                .replace("__NOW__", now)
                .replace("__NOT_ON_OR_AFTER__", notOnOrAfter)
                .replace("__ASSERTING_PARTY__", assertingPartyId)
                .replace("__REQUEST_ID__", ctx.requestId())
                .replace("__SSO_URL__", idpSsoUrl)
                .replace("__ENTITY_ID_SP__", entityIdSp);

            // 5. WRAP: inject the forged assertion right after </saml2p:Status>, before the genuine signed one
            String wrappedXml = signedXml.replaceFirst("(</saml2p:Status>)", "$1" + java.util.regex.Matcher.quoteReplacement(forgedAssertion));

            // 6. Re-encode WITHOUT resigning: the original signature stays valid, the forged assertion is extra
            return Base64.getEncoder().encodeToString(wrappedXml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to simulate an XML Signature Wrapping (XSW) attack", e);
        }
    }

    /**
     * Forged, UNSIGNED assertion used to perform XML Signature Wrapping.
     * Placeholders enclosed in {@code __XXX__} are replaced dynamically at runtime.
     */
    private static final String FORGED_ASSERTION_TEMPLATE = """
    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" \
    ID="_forged_assertion_id" IssueInstant="__NOW__" Version="2.0">
        <saml2:Issuer Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">__ASSERTING_PARTY__</saml2:Issuer>
        <saml2:Subject>
            <saml2:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient"
                          NameQualifier="__ASSERTING_PARTY__">SPID-forged-id</saml2:NameID>
            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                <saml2:SubjectConfirmationData InResponseTo="__REQUEST_ID__"
                                               NotOnOrAfter="__NOT_ON_OR_AFTER__"
                                               Recipient="__SSO_URL__"/>
            </saml2:SubjectConfirmation>
        </saml2:Subject>
        <saml2:Conditions NotBefore="__NOW__" NotOnOrAfter="__NOT_ON_OR_AFTER__">
            <saml2:AudienceRestriction>
                <saml2:Audience>__ENTITY_ID_SP__</saml2:Audience>
            </saml2:AudienceRestriction>
        </saml2:Conditions>
        <saml2:AuthnStatement AuthnInstant="__NOW__">
            <saml2:AuthnContext>
                <saml2:AuthnContextClassRef>https://www.spid.gov.it/SpidL2</saml2:AuthnContextClassRef>
            </saml2:AuthnContext>
        </saml2:AuthnStatement>
        <saml2:AttributeStatement>
            <saml2:Attribute Name="fiscalNumber">
                <saml2:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema"
                                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                      xsi:type="xs:string">TINIT-FORGED00A00H000X</saml2:AttributeValue>
            </saml2:Attribute>
        </saml2:AttributeStatement>
    </saml2:Assertion>""";
}
