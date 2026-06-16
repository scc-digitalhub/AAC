package it.smartcommunitylab.aac.spid.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Inflater;

/**
 * Utility class for processing and validating SAML AuthnRequests within the SPID flow.
 * Specifically, it handles the extraction, decoding, and decompression of requests
 * transmitted via the SAML 2.0 HTTP-Redirect Binding.
 */
public class RequestUtils {

    /**
     * Parses a raw SAML AuthnRequest XML string and extracts its unique ID attribute.
     * This ID is crucial for establishing the 'InResponseTo' correlation during the SAML response phase.
     *
     * @param xmlContent The raw XML string of the SAML AuthnRequest.
     * @return The extracted 'ID' attribute, or null if parsing fails or the element is not found.
     */
    public String extractAuthnRequestId(String xmlContent) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            Element authnRequestElement = (Element) doc.getElementsByTagName("saml2p:AuthnRequest").item(0);

            if (authnRequestElement != null) {
                return authnRequestElement.getAttribute("ID");
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible extract AuthnRequestId from AuthnRequest: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Extracts the raw 'SAMLRequest' query parameter from a full HTTP-Redirect URL.
     * The extracted parameter is typically Base64-encoded and Deflate-compressed.
     *
     * @param redirectedUrl The complete URL where the SP redirected the user for authentication.
     * @return The URL-decoded string value of the 'SAMLRequest' parameter, or null if not found.
     * @throws Exception If URL parsing or UTF-8 decoding fails.
     */
    public String extractSamlRequestParameter(String redirectedUrl) throws Exception {
        URL url = new URL(redirectedUrl);
        String query = url.getQuery();
        String samlRequestEncoded = null;

        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx == -1) continue;

                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);

                if ("SAMLRequest".equals(key)) {
                    samlRequestEncoded = value;
                }
            }
        }
        return samlRequestEncoded;
    }

    /**
     * Extracts the 'RelayState' query parameter from a full HTTP-Redirect URL.
     * The RelayState is an opaque identifier used by the SP to maintain application state
     * across the SAML SSO flow.
     *
     * @param redirectedUrl The complete URL where the SP redirected the user for authentication.
     * @return The URL-decoded string value of the 'RelayState' parameter, or null if not found.
     * @throws Exception If URL parsing or UTF-8 decoding fails.
     */
    public String extractRelayStateParameter(String redirectedUrl) throws Exception {
        URL url = new URL(redirectedUrl);
        String query = url.getQuery();
        String relayState = null;

        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx == -1) continue;

                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);

                if ("RelayState".equals(key)) {
                    relayState = value;
                }
            }
        }
        return relayState;
    }

    /**
     * Reverses the HTTP-Redirect Binding encoding process for a SAMLRequest.
     * It decodes the Base64 string and then inflates (decompresses) the resulting bytes
     * using the DEFLATE algorithm (RFC 1951) without the zlib wrapper (nowrap=true).
     *
     * @param samlRequestEncoded The Base64 encoded and deflated SAMLRequest string.
     * @return The raw, plain-text XML string of the SAML AuthnRequest.
     * @throws RuntimeException if the decoding or decompression process fails.
     */
    public String decodeAndInflateSamlRequest(String samlRequestEncoded) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(samlRequestEncoded);

            // Use Inflater with nowrap=true to handle raw DEFLATE streams (standard for SAML bindings)
            Inflater inflater = new Inflater(true);
            inflater.setInput(decodedBytes);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            inflater.end();

            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Impossibile decodificare/decomprimere SAMLRequest: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the value of a hidden input field (e.g., SAMLRequest or RelayState)
     * from an HTML page generated for the SAML 2.0 HTTP-POST Binding.
     * * Unlike the Redirect binding where data is in the URL, the POST binding
     * embeds the payload within an HTML form as hidden input fields.
     *
     * @param html The raw HTML content returned by the server.
     * @param inputName The 'name' attribute of the target input (e.g., "SAMLRequest").
     * @return The string value contained within the 'value' attribute.
     * @throws IllegalStateException if the specified input field is not found in the HTML.
     */
    public String extractHtmlInputValue(String html, String inputName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("name=\"" + inputName + "\"\\s+value=\"([^\"]+)\"")
                .matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Could not find the input field '" + inputName + "' in the generated HTML for POST Binding");
    }

    /**
     * Decodes a SAMLRequest transmitted via the HTTP-POST Binding.
     * * According to SAML 2.0 specifications, requests sent via POST are only
     * Base64-encoded, WITHOUT the DEFLATE compression used in the HTTP-Redirect binding.
     *
     * @param samlRequestEncoded The Base64 encoded string extracted from the HTML form.
     * @return The plain-text XML string of the SAML AuthnRequest.
     */
    public String decodePostSamlRequest(String samlRequestEncoded) {
        byte[] decodedBytes = java.util.Base64.getMimeDecoder().decode(samlRequestEncoded);
        return new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
