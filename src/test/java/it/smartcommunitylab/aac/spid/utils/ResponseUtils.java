package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.SpidKeys;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility class specialized in generating, modifying, and signing SAML 2.0 Responses
 * tailored for SPID Authentication test flows.
 * It provides DOM manipulation to alter attributes (like InResponseTo, Destination,
 * and SPID Attributes) and uses OpenSAML to securely re-sign the payload to simulate
 * a valid Identity Provider response.
 */
public class ResponseUtils {

    // Factory used to securely parse XML strings into DOM objects (Anti-XXE protected).
    // Declared as static final to instantiate it only once and prevent performance bottlenecks.
    static final DocumentBuilderFactory SECURE_DOC_BUILDER_FACTORY;

    // Factory used to serialize in-memory DOM objects back into standard XML strings.
    // Declared as static final to reduce object creation overhead during heavy loads.
    static final TransformerFactory TRANSFORMER_FACTORY;

    /**
     * Modifies a base SAML Response XML string to adapt it to the current test session context.
     * Updates timestamps, correlation IDs, destinations, issuer EntityIDs, and injects specific
     * SPID user attributes (name, familyName, spidCode, fiscalNumber, email).
     *
     * @param xmlContent              The raw Base64-encoded original XML Response template.
     * @param inResponseToValue       The request ID this response is correlated to.
     * @param ssoDestinationUrl       The Service Provider's Assertion Consumer Service (ACS) URL.
     * @param assertingPartyEntityId  The Entity ID of the mock Identity Provider.
     * @param serviceProviderEntityId The Entity ID of the relying Service Provider.
     * @return A dynamically modified, Base64-encoded SAML Response ready to be signed.
     */
    public static String modifyAndEncodeSamlResponse(
        String xmlContent,
        String inResponseToValue,
        String ssoDestinationUrl,
        String assertingPartyEntityId,
        String serviceProviderEntityId,
        Set<SpidAttribute> spidAttributes)
    {
        try {

            DocumentBuilder db = SECURE_DOC_BUILDER_FACTORY.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            Instant now = Instant.now();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

            // Update root Response element attributes
            NodeList responseNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "Response");
            if (responseNodes.getLength() > 0) {
                Element responseElement = (Element) responseNodes.item(0);
                responseElement.setAttribute("InResponseTo", inResponseToValue);
                responseElement.setAttribute("IssueInstant", formatter.format(now));
                responseElement.setAttribute("Destination", ssoDestinationUrl);
            }

            // Update SubjectConfirmationData to ensure the SP accepts the bearer token
            NodeList subjectConfirmationDataNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "SubjectConfirmationData");
            if (subjectConfirmationDataNodes.getLength() > 0) {
                Element subjectConfirmationDataElement = (Element) subjectConfirmationDataNodes.item(0);
                subjectConfirmationDataElement.setAttribute("InResponseTo", inResponseToValue);
                subjectConfirmationDataElement.setAttribute("NotOnOrAfter", formatter.format(now.plusSeconds(SpidKeys.SPID_CLOCK_SKEW * 10)));
                subjectConfirmationDataElement.setAttribute("Recipient", ssoDestinationUrl);
            }

            // Update Issuers to reflect the mock IdP
            NodeList issuerNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
            if (issuerNodes.getLength() > 0) {
                for (int i = 0; i < issuerNodes.getLength(); i++) {
                    Element issuerElement = (Element) issuerNodes.item(i);
                    issuerElement.setTextContent(assertingPartyEntityId);
                }
            }

            // Update AuthnStatement timestamps
            NodeList authnStatementNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "AuthnStatement");
            if (authnStatementNodes.getLength() > 0) {
                Element authnStatementElement = (Element) authnStatementNodes.item(0);
                authnStatementElement.setAttribute("AuthnInstant", formatter.format(now));
            }

            // Update NameID Qualifier
            NodeList nameIDNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
            if (nameIDNodes.getLength() > 0) {
                Element nameIDElement = (Element) nameIDNodes.item(0);
                nameIDElement.setAttribute("NameQualifier", assertingPartyEntityId);
            }

            // Process the main Assertion block
            NodeList assertionNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion");
            if (assertionNodes.getLength() > 0) {
                Element assertionElement = (Element) assertionNodes.item(0);
                assertionElement.setAttribute("IssueInstant", formatter.format(now));

                // REMOVE ALL EXISTING SIGNATURES (both from Response and Assertion) before re-signing.
                // Failing to do so will result in cryptographic validation errors on the SP side.
                NodeList allSignatures = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
                while (allSignatures.getLength() > 0) {
                    Node sig = allSignatures.item(0);
                    sig.getParentNode().removeChild(sig);
                }

                // Force Destination value (Critical for Spring Security validation)
                Element responseElement = (Element) doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "Response").item(0);
                responseElement.setAttribute("Destination", ssoDestinationUrl);

                // Enforce Audience Restriction Coherence
                Element audience = (Element) doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Audience").item(0);
                if (audience != null) {
                    audience.setTextContent(serviceProviderEntityId);
                }

                // Update validity timeframe conditions
                NodeList conditionsNodes = assertionElement.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Conditions");
                if (conditionsNodes.getLength() > 0) {
                    Element conditionsElement = (Element) conditionsNodes.item(0);
                    conditionsElement.setAttribute("NotBefore", formatter.format(now.minusSeconds(60)));
                    conditionsElement.setAttribute("NotOnOrAfter", formatter.format(now.plusSeconds(SpidKeys.SPID_CLOCK_SKEW * 10)));

                    NodeList audienceRestrictionNodes = conditionsElement.getElementsByTagNameNS(
                            "urn:oasis:names:tc:SAML:2.0:assertion",
                            "AudienceRestriction"
                    );
                    if (audienceRestrictionNodes.getLength() > 0) {
                        Element audienceRestrictionElement = (Element) audienceRestrictionNodes.item(0);
                        NodeList audienceNodes = audienceRestrictionElement.getElementsByTagNameNS(
                                "urn:oasis:names:tc:SAML:2.0:assertion",
                                "Audience"
                        );

                        if (audienceNodes.getLength() > 0) {
                            Element audienceElement = (Element) audienceNodes.item(0);
                            audienceElement.setTextContent(serviceProviderEntityId);
                        }
                    }
                }

                // Inject Mock SPID Attributes into the AttributeStatement
                NodeList attributeStatementNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "AttributeStatement");
                Element attributeStatementElement = (attributeStatementNodes.getLength() > 0) ? (Element) attributeStatementNodes.item(0) : null;

                if (attributeStatementElement != null) {
                    // Clear the existing 'name' attribute if present to avoid duplication
                    NodeList nameAttributes = attributeStatementElement.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Attribute");
                    for (int i = 0; i < nameAttributes.getLength(); i++) {
                        Element attr = (Element) nameAttributes.item(i);
                        if ("name".equals(attr.getAttribute("Name"))) {
                            attributeStatementElement.removeChild(attr);
                            break;
                        }
                    }

                    // Create the list of SpidAttributes
                    List<Element> spidAttributesSelected = buildSpidAttributes(spidAttributes, doc);

                    // Append on AttributeStatement
                    for (Element attr : spidAttributesSelected) {
                        attributeStatementElement.appendChild(attr);
                    }
                }
            }

            // Serialize the modified DOM back into an XML string
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            String modifiedXmlString = out.toString(StandardCharsets.UTF_8);

            return Base64.getEncoder().encodeToString(modifiedXmlString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to modify and encode the customized SPID SAML Response", e);
        }
    }

    /**
     * Dynamically builds a list of SAML Attributes with mock user data based on the requested SPID attributes.
     *
     * @param requestedSpidAttributes The set of {@link SpidAttribute}s explicitly requested by the Service Provider.
     * @param doc               The active XML {@link Document} being built.
     * @return A list of populated SAML {@code <saml2:Attribute>} Elements ready to be appended.
     */
    private static List<Element> buildSpidAttributes(Set<SpidAttribute> requestedSpidAttributes, Document doc) {
        try {
            List<Element> attributesList = new ArrayList<>();

            // Prepare the complete mock user profile
            Map<String, String> mockUser = createMockUser();

            Set<String> attributesToInject = new HashSet<>();

            if (requestedSpidAttributes != null && !requestedSpidAttributes.isEmpty()) {
                // Use explicitly requested attributes
                for (SpidAttribute requestedAttr : requestedSpidAttributes) {
                    attributesToInject.add(requestedAttr.getValue());
                }
            } else {
                // Fallback to default SPID attributes
                attributesToInject.addAll(Arrays.asList(
                    "name", "email", "fiscalNumber", "spidCode", "familyName"
                ));
            }

            // Create element
            for (String attributeName : attributesToInject) {
                if (mockUser.containsKey(attributeName)) {
                    Element attribute = buildSamlAttribute(doc, attributeName, mockUser.get(attributeName));
                    attributesList.add(attribute);
                }
            }
            return attributesList;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build the mock SPID SAML attributes", e);
        }
    }

    /**
     * Provides a fictitious SPID user profile (attribute name to sample value) used as test data
     * for building mock SAML assertions. All values are fake and for testing only.
     */
    private static @NonNull Map<String, String> createMockUser() {
        try {
            Map<String, String> mockUser = new HashMap<>();
            mockUser.put("name", "FEDERICO, Giacomo");
            mockUser.put("familyName", "ROSSI");
            mockUser.put("spidCode", "MOKIDP80A01H501C");
            mockUser.put("fiscalNumber", "MOKIDP80A01H501C");
            mockUser.put("email", "federico.rossi@invalid.mock");
            mockUser.put("mobilePhone", "+393331234567");
            mockUser.put("ivaCode", "IT12345678901");
            mockUser.put("idCard", "cartaIdentita CA00000AA comuneRoma 2025-01-01 2040-01-01");
            return mockUser;
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct the mock SPID user profile", e);
        }
    }

    /**
     * Helper method to dynamically construct and return a standard SAML 2.0
     * string attribute (saml2:Attribute and saml2:AttributeValue).
     *
     * @param doc   The active XML Document.
     * @param name  The name of the SPID attribute (e.g., 'fiscalNumber').
     * @param value The value to assign to the attribute.
     * @return The constructed {@code <saml2:Attribute>} Element.
     */
    private static Element buildSamlAttribute(Document doc, String name, String value) {
        try {
            Element attribute = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:Attribute");
            attribute.setAttribute("Name", name);

            Element attributeValue = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:AttributeValue");
            attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
            attributeValue.setTextContent(value);

            attribute.appendChild(attributeValue);
            return attribute;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to construct the SAML attribute element for '%s'", name), e);
        }
    }

    /* =========================================================================
     * Cryptographic and OpenSAML Signing Methods
     * ========================================================================= */

    /**
     * Decodes the mock keys, parses the Base64 XML, and applies SPID-compliant signatures.
     *
     * @param xmlContentBase64   The unsigned Base64 SAML Response string.
     * @param privateKeyBase64   The Base64 string of the private signing key.
     * @param certificateBase64  The Base64 string of the public X.509 certificate.
     * @return A Base64-encoded string of the fully signed SAML Response.
     */
    public static String createSignedSamlResponse(
        String xmlContentBase64,
        String privateKeyBase64,
        String certificateBase64)
    {
        try {
            BasicX509Credential credential = createCredentialFromPem(privateKeyBase64, certificateBase64);
            String samlResponseXml = new String(Base64.getDecoder().decode(xmlContentBase64), StandardCharsets.UTF_8);

            String signedSamlResponse = signSamlResponse(samlResponseXml, credential);

            return Base64.getEncoder().encodeToString(signedSamlResponse.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign and encode the SAML Response", e);
        }
    }

    /**
     * Implements the AgID SPID technical rule requiring a digital signature on BOTH
     * the inner SAML Assertion and the outer enclosing SAML Response using OpenSAML APIs.
     *
     * @param samlResponseXml The plain-text unsigned SAML Response XML.
     * @param signingCredential The OpenSAML credential used for signing.
     * @return The signed XML string.
     */
    private static String signSamlResponse(String samlResponseXml, Credential signingCredential) {
        try {
            DocumentBuilder db = SECURE_DOC_BUILDER_FACTORY.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(samlResponseXml.getBytes(StandardCharsets.UTF_8)));

            Element rootElement = doc.getDocumentElement();
            Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(rootElement);
            assert unmarshaller != null;
            Response response = (Response) unmarshaller.unmarshall(rootElement);

            // 1. Sign the inner Assertion
            if (!response.getAssertions().isEmpty()) {
                org.opensaml.saml.saml2.core.Assertion assertion = response.getAssertions().get(0);
                Signature assertionSignature = buildSignatureObject(signingCredential);
                assertion.setSignature(assertionSignature);

                // Marshalling is strictly required by OpenSAML before actual signing occurs
                Objects.requireNonNull(XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion)).marshall(assertion);
                Signer.signObject(assertionSignature);
            }

            // 2. Sign the outer Response container
            Signature responseSignature = buildSignatureObject(signingCredential);
            response.setSignature(responseSignature);

            Objects.requireNonNull(XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(response)).marshall(response);
            Signer.signObject(responseSignature);

            // Transform the final signed DOM back into an XML String
            StringWriter writer = new StringWriter();
            TRANSFORMER_FACTORY.newTransformer().transform(new DOMSource(response.getDOM()), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply cryptographic signatures to the SAML Assertion and Response", e);
        }
    }

    /**
     * Constructs an OpenSAML Signature object configured with the algorithms
     * mandated by SPID (RSA_SHA256 and exclusive canonicalization).
     */
    private static Signature buildSignatureObject(Credential credential) {
        try {
            Signature signature = (Signature) XMLObjectSupport.buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSigningCredential(credential);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            appendKeyInfoToSignature(signature, credential);
            return signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build the OpenSAML Signature object with SPID parameters", e);
        }
    }

    /**
     * Appends the X.509 Certificate to the XML Signature's KeyInfo block,
     * allowing the Relying Party to verify the signature against published metadata.
     */
    private static void appendKeyInfoToSignature(Signature signature, Credential signingCredential) {
        if (!(signingCredential instanceof X509Credential)) {
            throw new IllegalArgumentException("Signing credential must be an X509Credential.");
        }

        try {
            java.security.cert.X509Certificate cert = ((X509Credential) signingCredential).getEntityCertificate();

            KeyInfo keyInfo = (KeyInfo) XMLObjectSupport.buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
            X509Data x509Data = (X509Data) XMLObjectSupport.buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
            X509Certificate x509Certificate =
                    (X509Certificate) XMLObjectSupport.buildXMLObject(X509Certificate.DEFAULT_ELEMENT_NAME);

            String certValue = Base64.getEncoder().encodeToString(cert.getEncoded());
            x509Certificate.setValue(certValue);

            x509Data.getX509Certificates().add(x509Certificate);
            keyInfo.getX509Datas().add(x509Data);
            signature.setKeyInfo(keyInfo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to append the X.509 KeyInfo block to the SAML signature", e);
        }
    }

    /* =========================================================================
     * Credential Parsing Utility
     * ========================================================================= */

    /**
     * Parses raw Base64 strings representing PEM encoded keys and certificates
     * into a usable OpenSAML BasicX509Credential.
     * Strips PEM headers, footers, and whitespace before decoding.
     */
    private static BasicX509Credential createCredentialFromPem(String privateKeyBase64, String certificateBase64) {
        try {
            Function<String, byte[]> decodePem = (base64String) -> {
                String cleanString = base64String
                        .replaceAll("(?m)^-----.*-----.*\\n", "")
                        .replaceAll("\\s+", "");

                return Base64.getDecoder().decode(cleanString);
            };

            byte[] privateKeyBytes = decodePem.apply(privateKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            byte[] certBytes = decodePem.apply(certificateBase64);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            java.security.cert.X509Certificate certificate = (java.security.cert.X509Certificate) certFactory.generateCertificate(
                    new ByteArrayInputStream(certBytes)
            );

            BasicX509Credential credential = new BasicX509Credential(certificate);
            credential.setPrivateKey(privateKey);

            return credential;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create the OpenSAML X.509 credential from the provided PEM strings", e);
        }
    }

    static {
        try {
            // 1. Initialize the OpenSAML library engine
            InitializationService.initialize();

            // 2. Create a secure XML DocumentBuilderFactory (Anti-XXE protection)
            SECURE_DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
            SECURE_DOC_BUILDER_FACTORY.setNamespaceAware(true);
            SECURE_DOC_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SECURE_DOC_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-general-entities", false);
            SECURE_DOC_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            // 3. Create the Factory for XML serialization
            TRANSFORMER_FACTORY = TransformerFactory.newInstance();

        } catch (Exception e) {
            throw new RuntimeException("Critical initialization error in ResponseUtils class", e);
        }
    }
}
