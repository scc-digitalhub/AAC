package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility for simulating active SAML/SPID security attacks.
 * Used alongside SpidSecurityAttacksTest to test the SP's resilience against malicious tampering.
 */
public class SpidAttackUtils {

    /**
     * Simulates a security level downgrade (e.g., L2 requested, L1 returned) - Privilege Downgrade Attack.
     */
    public String prepareForSimulationNotValidChangeSpidLevelLow(
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

        // Force SpidL1
        plainXml = plainXml.replace("https://www.spid.gov.it/SpidL2", "https://www.spid.gov.it/SpidL1");

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));
        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }
}
