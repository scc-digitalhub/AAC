package it.smartcommunitylab.aac.spid.utils;

import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for handling and validating SPID SAML Metadata.
 * Provides helper methods to parse XML EntityDescriptors from MockMvc HTTP responses,
 * decode X.509 certificates, and inspect Identity Provider configurations during tests.
 */
public class MetadataUtils {

    /**
     * Parses and unmarshals the XML content of an MvcResult into an OpenSAML EntityDescriptor.
     * This method verifies that the mocked endpoint correctly returns valid SAML metadata.
     *
     * @param res        The MockMvc result containing the raw XML response.
     * @return A fully parsed and validated {@link EntityDescriptor} object representing the metadata.
     * @throws Exception If XML parsing, OpenSAML unmarshalling, or AssertJ validations fail.
     */
    public EntityDescriptor extractEntityDescriptorFromMvcResult(MvcResult res) throws Exception {

        String xml = res.getResponse().getContentAsString();
        XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
        ParserPool parserPool = registry.getParserPool();

        // Parse the raw XML string into a DOM element
        Document document = parserPool.parse(new ByteArrayInputStream(xml.getBytes()));
        Element rootElement = document.getDocumentElement();

        // Retrieve the appropriate OpenSAML unmarshaller for the root element
        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(rootElement);
        assertThat(unmarshaller).isNotNull();

        // Unmarshal the DOM element into an OpenSAML XMLObject
        XMLObject xmlObject = unmarshaller.unmarshall(rootElement);

        // Ensure the object is specifically an EntityDescriptor
        assertThat(xmlObject).isInstanceOf(EntityDescriptor.class);
        EntityDescriptor descriptor = (EntityDescriptor) xmlObject;

        // Basic validation: the entityID attribute must be present
        assertThat(descriptor.getEntityID()).isNotNull();

        return descriptor;
    }

    /**
     * Decodes a Base64-encoded string into a valid Java {@link X509Certificate} object.
     *
     * @param certificate The Base64 encoded X.509 certificate string.
     * @return The parsed X509Certificate instance, or null if the parsing fails.
     * @throws Exception If the provided certificate string is null or decoding critically fails.
     */
    public X509Certificate decodeBase64ToX509Certificate(String certificate) throws Exception {
        assertThat(certificate).isNotNull();

        byte[] decodedBytes = Base64.getDecoder().decode(certificate.getBytes());

        try (ByteArrayInputStream stream = new ByteArrayInputStream(decodedBytes)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(stream);
        } catch (Exception e) {
            // Log fallback for invalid certificates
            System.err.println("Certificato non è valido");
            return null;
        }
    }

    /**
     * Extracts the original DigestValue directly from the XML DOM.
     */
    public String extractDigestValueFromXml(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        NodeList digestNodes = doc.getElementsByTagNameNS(SignatureConstants.XMLSIG_NS, "DigestValue");
        if (digestNodes.getLength() == 0) {
            throw new IllegalArgumentException("No DigestValue node found in the XML.");
        }
        return digestNodes.item(0).getTextContent();
    }

    /**
     * Strips the existing signature, resets the DOM to bypass serialization artifacts (e.g., pretty-printing),
     * and resigns the XML in memory to extract the pure cryptographic digest.
     */
    public String resignAndExtractDigest(String originalXml, SigningCredential signingCredential) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8)));

        // 1. Remove the old signature
        NodeList signatureNodes = doc.getElementsByTagNameNS(SignatureConstants.XMLSIG_NS, "Signature");
        if (signatureNodes.getLength() > 0) {
            Node signatureNode = signatureNodes.item(0);
            signatureNode.getParentNode().removeChild(signatureNode);
        }

        // 2. Unmarshal into an EntityDescriptor
        Element rootElement = doc.getDocumentElement();
        Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(rootElement);
        EntityDescriptor descriptor = (EntityDescriptor) unmarshaller.unmarshall(rootElement);

        // 3. Clear the DOM cache to force the Marshaller to rebuild a dense, whitespace-free XML matching the original pre-pretty-print state.
        descriptor.releaseDOM();
        descriptor.releaseChildrenDOM(true);

        // 4. Build the new Signature, enforcing SPID parameters (RSA-SHA256)
        BasicX509Credential credential = buildTestCredential(signingCredential.getSigningKey(), signingCredential.getSigningCertificate());
        Signature newSignature = (Signature) XMLObjectSupport.buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
        newSignature.setSigningCredential(credential);
        newSignature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        newSignature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        descriptor.setSignature(newSignature);

        // 5. Marshalling (recreates the clean DOM!) and actual signing
        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(descriptor).marshall(descriptor);
        Signer.signObject(newSignature);

        // 6. Extract the newly calculated digest from the clean DOM
        Element newSignatureDom = newSignature.getDOM();
        NodeList newDigestNodes = newSignatureDom.getElementsByTagNameNS(SignatureConstants.XMLSIG_NS, "DigestValue");
        return newDigestNodes.item(0).getTextContent();
    }

    /**
     * Builds the OpenSAML credential starting from raw PEM/Base64 strings.
     */
    public BasicX509Credential buildTestCredential(String privateKeyPem, String certBase64) throws Exception {
        String cleanKey = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(cleanKey);
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        String cleanCert = certBase64
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s+", "");

        byte[] certBytes = java.util.Base64.getDecoder().decode(cleanCert);
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));

        return new BasicX509Credential(certificate, privateKey);
    }
}
