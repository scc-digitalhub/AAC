package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.setupflow.SpidAgidAnomalyScenario;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Specialized factory utility for generating mocked SPID SAML Error Responses.
 * This class uses a pre-configured XML template and performs DOM manipulation
 * to inject specific AgID error codes (Status, Sub-Status, and StatusMessage)
 * while dynamically updating session-specific attributes like IDs and timestamps.
 */
public class AgidAnomalyUtils extends ResponseUtils {

    /**
     * Constructs a tailored SAML Error Response based on the provided AgID scenario.
     *
     * @param scenario             The specific AgID error scenario to simulate (e.g., CODE_25).
     * @param inResponseToValue    The ID of the original AuthnRequest this response is answering.
     * @param destinationUrl       The SP endpoint URL where this response will be sent.
     * @param issuerEntityId       The Entity ID of the mock Identity Provider.
     * @return A Base64-encoded string containing the modified XML SAML Response, ready to be signed.
     * @throws Exception If DOM parsing or transformation fails.
     */
    public String buildErrorSamlResponse(String xmlResponseErrorTemplate, SpidAgidAnomalyScenario scenario, String inResponseToValue, String destinationUrl, String issuerEntityId) throws Exception {

        // 1. Decode and parse the base template into a manipulable DOM Document
        DocumentBuilder db = ResponseUtils.SECURE_DBF.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xmlResponseErrorTemplate.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        Instant now = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

        // 2. Update the root Response element attributes (ID, Destination, IssueInstant, InResponseTo)
        Element responseElement = (Element) doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "Response").item(0);
        if (responseElement != null) {
            responseElement.setAttribute("ID", "_" + UUID.randomUUID().toString()); // Generate a fresh unique ID
            responseElement.setAttribute("InResponseTo", inResponseToValue);
            responseElement.setAttribute("Destination", destinationUrl);
            responseElement.setAttribute("IssueInstant", formatter.format(now));
        }

        // 3. Update the Issuer to match the mock IdP.
        // If missing, dynamically create and insert it as the FIRST child to comply with SAML 2.0 schema.
        NodeList issuerNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
        if (issuerNodes.getLength() > 0) {
            for (int i = 0; i < issuerNodes.getLength(); i++) {
                issuerNodes.item(i).setTextContent(issuerEntityId);
            }
        } else if (responseElement != null) {
            Element newIssuer = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:Issuer");
            newIssuer.setTextContent(issuerEntityId);
            responseElement.insertBefore(newIssuer, responseElement.getFirstChild());
        }

        // 4. STRIP EXISTING SIGNATURE: Crucial step. Since the document payload has been altered,
        // the old signature is invalid and must be removed entirely before re-signing to avoid conflicts.
        NodeList signatureNodes = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
        while (signatureNodes.getLength() > 0) {
            Node signatureNode = signatureNodes.item(0);
            signatureNode.getParentNode().removeChild(signatureNode);
        }

        // 5. Update the Status node with the values mapped from the provided Enum
        NodeList statusNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "Status");
        if (statusNodes.getLength() > 0) {
            Element statusElement = (Element) statusNodes.item(0);

            // Clear any existing children from the Status node
            while (statusElement.hasChildNodes()) {
                statusElement.removeChild(statusElement.getFirstChild());
            }

            // Create the primary <saml2p:StatusCode Value="...">
            Element mainStatusCodeElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "saml2p:StatusCode");
            mainStatusCodeElement.setAttribute("Value", scenario.getSamlTopLevelStatus());

            // Create and append the secondary Sub StatusCode, if specified by the scenario
            if (scenario.getSamlSubStatus() != null && !scenario.getSamlSubStatus().isEmpty()) {
                Element subStatusCodeElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "saml2p:StatusCode");
                subStatusCodeElement.setAttribute("Value", scenario.getSamlSubStatus());
                mainStatusCodeElement.appendChild(subStatusCodeElement);
            }

            // Create and append the <saml2p:StatusMessage>
            Element statusMessageElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "saml2p:StatusMessage");
            statusMessageElement.setTextContent(scenario.getSamlStatusMessage());

            // Reconstruct the Status hierarchy
            statusElement.appendChild(mainStatusCodeElement);
            statusElement.appendChild(statusMessageElement);
        }

        // 6. Serialize the modified DOM back into an XML string and encode it to Base64
        Transformer transformer = ResponseUtils.TRANSFORMER_FACTORY.newTransformer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        String modifiedXmlString = out.toString(StandardCharsets.UTF_8.name());

        return Base64.getEncoder().encodeToString(modifiedXmlString.getBytes(StandardCharsets.UTF_8));
    }
}
