package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.setupflow.SpidRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility for manipulating SAML/SPID payloads for standard authentication flows and valid edge cases.
 */
public class SpidAuthnPositiveUtils {

    /**
     * Simulates an IdP returning a higher security level (e.g., L2 requested, L3 returned).
     * This is a legitimate scenario and should result in successful authentication.
     * * @param ctx The current SPID test context.
     * @param xmlTemplate The base XML response template.
     * @param idpSsoUrl The mock IdP SSO endpoint.
     * @param assertingPartyId The EntityID of the IdP.
     * @param entityIdSp The EntityID of the Service Provider.
     * @param privateKey The private key for signing.
     * @param certificate The public certificate for the signature.
     * @return A signed SAML Response containing a higher AuthnContextClassRef.
     * @throws Exception if XML manipulation or signing fails.
     */
    public String prepareForSimulationNotValidChangeSpidLevelHigh(
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

        // RAW SUBSTITUTION: The original IdP sent SpidL2, we force SpidL3.
        plainXml = plainXml.replace("https://www.spid.gov.it/SpidL2", "https://www.spid.gov.it/SpidL3");

        String tamperedXmlBase64 = Base64.getEncoder().encodeToString(plainXml.getBytes(StandardCharsets.UTF_8));

        return ResponseUtils.createSignedSamlResponse(tamperedXmlBase64, privateKey, certificate);
    }
}
