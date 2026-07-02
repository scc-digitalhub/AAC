package it.smartcommunitylab.aac.spid.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.zip.Inflater;

/**
 * Utility class for processing and validating SAML AuthnRequests within the SPID flow.
 * Specifically, it handles the extraction, decoding, and decompression of requests
 * transmitted via the SAML 2.0 HTTP-Redirect Binding.
 */
public class RequestUtils {

    /**
     * Shared, namespace-aware {@link DocumentBuilderFactory} hardened against XXE attacks.
     * Created once and reused to avoid per-call setup overhead.
     */
    private static final DocumentBuilderFactory SECURE_DBF;

    /**
     * Parses a raw SAML AuthnRequest XML string and extracts its unique ID attribute.
     * This ID is crucial for establishing the 'InResponseTo' correlation during the SAML response phase.
     *
     * @param xmlContent The raw XML string of the SAML AuthnRequest.
     * @return The extracted 'ID' attribute, or null if parsing fails or the element is not found.
     */
    public static String extractAuthnRequestId(String xmlContent) {
        try {
            Document doc = SECURE_DBF.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();
            // namespace-aware: does not depend on the saml2p: prefix
            Element authnRequest = (Element) doc
                .getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "AuthnRequest")
                .item(0);
            return authnRequest != null ? authnRequest.getAttribute("ID") : null;
        } catch (Exception e) {
            throw new RuntimeException("Unable to extract ID from AuthnRequest: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the raw 'SAMLRequest' query parameter from a full HTTP-Redirect URL.
     * The extracted parameter is typically Base64-encoded and Deflate-compressed.
     *
     * @param redirectedUrl The complete URL where the SP redirected the user for authentication.
     * @return The URL-decoded string value of the 'SAMLRequest' parameter, or null if not found.
     */
    public static String extractSamlRequestParameter(String redirectedUrl) {
        return extractQueryParameter(redirectedUrl, "SAMLRequest");
    }

    /**
     * Extracts the 'RelayState' query parameter from a full HTTP-Redirect URL.
     * The RelayState is an opaque identifier used by the SP to maintain application state
     * across the SAML SSO flow.
     *
     * @param redirectedUrl The complete URL where the SP redirected the user for authentication.
     * @return The URL-decoded string value of the 'RelayState' parameter, or null if not found.
     */
    public static String extractRelayStateParameter(String redirectedUrl) {
        return extractQueryParameter(redirectedUrl, "RelayState");
    }

    /** Extracts and URL-decodes a single query parameter from a redirect URL; null if absent. */
    private static String extractQueryParameter(String redirectedUrl, String paramName) {
        try {
            String query = new URL(redirectedUrl).getQuery();
            if (query == null) {
                return null;
            }
            for (String pair : query.split("&")) {
                int idx = pair.indexOf('=');
                if (idx == -1) {
                    continue;
                }
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                if (paramName.equals(key)) {
                    return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to extract query parameter '%s' from the redirected URL", paramName), e);
        }
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
    public static String decodeAndInflateSamlRequest(String samlRequestEncoded) {
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
            throw new RuntimeException("Unable to decode/inflate SAMLRequest: " + e.getMessage(), e);
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
    public static String extractHtmlInputValue(String html, String inputName) {
        try {
            Matcher matcher = java.util.regex.Pattern
                    .compile("name=\"" + inputName + "\"\\s+value=\"([^\"]+)\"")
                    .matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not find the input field '" + inputName + "' in the generated HTML for POST Binding");
        }
        return null;
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

    static {
        SECURE_DBF = DocumentBuilderFactory.newInstance();
        // Namespace-aware parsing so elements are matched by namespace, not by literal prefix
        SECURE_DBF.setNamespaceAware(true);
        try {
            // XXE hardening: forbid DOCTYPE and disable external entity resolution
            SECURE_DBF.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SECURE_DBF.setFeature("http://xml.org/sax/features/external-general-entities", false);
            SECURE_DBF.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception e) {
            // A parser that can't be secured must fail fast at class load
            throw new ExceptionInInitializerError(e);
        }
    }
}
