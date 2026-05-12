package it.smartcommunitylab.aac.spid.utils;

import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
}
