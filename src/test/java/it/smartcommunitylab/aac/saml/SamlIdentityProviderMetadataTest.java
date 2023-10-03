package it.smartcommunitylab.aac.saml;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.*;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SamlIdentityProviderMetadataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapConfig config;

    public final static String BASE_URL = "http://localhost";
    public final static String METADATA_PATH = "/auth/saml/metadata/";
    public final static String SSO_PATH = "/auth/saml/sso/";

    private String idp1MetadataUrl;
    private String idp1SigningCertificate;
    private String idp1EntityId;
    private String idp1AssertionConsumerServiceLocation;

    private String idp2MetadataUrl;
    private String idp2SigningCertificate;
    private String idp2CryptCertificate;

    @BeforeEach
    public void readConfiguration() {
        config.getRealms().forEach(realm -> {
            if (realm.getRealm().getSlug().equals("saml-test")) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                assertThat(idps.size()).isEqualTo(2);

                ConfigurableIdentityProvider idp1 = idps.get(0);
                idp1MetadataUrl = BASE_URL + METADATA_PATH + idp1.getProvider();
                idp1AssertionConsumerServiceLocation = BASE_URL + SSO_PATH + idp1.getProvider();

                SamlIdentityProviderConfigMap config1 = new SamlIdentityProviderConfigMap();
                config1.setConfiguration(idp1.getConfiguration());
                idp1SigningCertificate = config1.getSigningCertificate();
                idp1EntityId = config1.getEntityId() != null ? config1.getEntityId() : idp1MetadataUrl;

                ConfigurableIdentityProvider idp2 = idps.get(1);
                idp2MetadataUrl = BASE_URL + METADATA_PATH + idp2.getProvider();

                SamlIdentityProviderConfigMap config2 = new SamlIdentityProviderConfigMap();
                config2.setConfiguration(idp2.getConfiguration());
                idp2SigningCertificate = config2.getSigningCertificate();
                idp2CryptCertificate = config2.getCryptCertificate();
            }
        });
    }

    @Test
    public void metadataIsAvailable() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(idp1MetadataUrl))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

        // content is available
        assertThat(res.getResponse().getContentAsString()).isNotBlank();
        assertThat(res.getResponse().getContentAsString()).isNotEmpty();
        assertThat(res.getResponse().getContentAsString()).isNotNull();
    }

    // disabled because we consider content type "application/xml" acceptable too
    @Disabled
    @Test
    public void metadataContentTypeIsCorrect() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(idp1MetadataUrl))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

        // content type is samlmetadata+xml
        assertEquals(res.getResponse().getContentType(), "application/samlmetadata+xml");
    }

    @Test
    public void metadataIsWellFormed() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(idp1MetadataUrl))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from XML
        String xml = res.getResponse().getContentAsString();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        // root element must be EntityDescriptor
        assertThat(doc.getDocumentElement().getTagName()).isEqualTo("md:EntityDescriptor");

        String namespace = "xmlns:md";
        assertTrue(doc.getDocumentElement().hasAttribute(namespace));
        assertThat(doc.getDocumentElement().getAttributes().getNamedItem(namespace).getNodeValue()).isEqualTo("urn:oasis:names:tc:SAML:2.0:metadata");

        assertTrue(doc.getDocumentElement().hasAttribute("entityID"));

        // SPSSODescriptor element must exist
        String SPSSODescriptor = "md:SPSSODescriptor";
        assertThat(doc.getElementsByTagName(SPSSODescriptor).getLength()).isEqualTo(1);
        Element element = (Element) doc.getElementsByTagName(SPSSODescriptor).item(0);

        String protocolSupportEnumeration = "protocolSupportEnumeration";
        assertTrue(element.hasAttribute(protocolSupportEnumeration));
        assertThat(element.getAttributes().getNamedItem(protocolSupportEnumeration).getNodeValue()).isEqualTo("urn:oasis:names:tc:SAML:2.0:protocol");

        // AssertionConsumerService element must exist
        String AssertionConsumerService = "md:AssertionConsumerService";
        assertThat(doc.getElementsByTagName(AssertionConsumerService).getLength()).isGreaterThanOrEqualTo(1);

        for (int i = 0; i < doc.getElementsByTagName(AssertionConsumerService).getLength(); i++) {
            element = (Element) doc.getElementsByTagName(AssertionConsumerService).item(i);

            assertThat(element.getAttributes().getNamedItem("Binding")).isNotNull();
            assertThat(element.getAttributes().getNamedItem("Location")).isNotNull();
        }
    }

    @Test
    public void checkAssertionConsumerServiceAttributesValue() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(idp1MetadataUrl))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from XML
        String xml = res.getResponse().getContentAsString();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        String AssertionConsumerService = "md:AssertionConsumerService";
        Element element = (Element) doc.getElementsByTagName(AssertionConsumerService).item(0);

        assertThat(element.getAttributes().getNamedItem("Binding").getNodeValue()).isEqualTo(Saml2MessageBinding.POST.getUrn());
        assertThat(element.getAttributes().getNamedItem("Location").getNodeValue()).isEqualTo(idp1AssertionConsumerServiceLocation);
    }

    @Test
    public void checkEntityIdValue() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(idp1MetadataUrl))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from XML
        String xml = res.getResponse().getContentAsString();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        assertThat(doc.getDocumentElement().getAttribute("entityID")).isEqualTo(idp1EntityId);
    }

    // disabled until encryption management is fixed
    @Disabled
    @Test
    public void onlySigningCertificateIsPresent() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(idp1MetadataUrl))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from XML
        String xml = res.getResponse().getContentAsString();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        String KeyDescriptor = "md:KeyDescriptor";
        assertThat(doc.getElementsByTagName(KeyDescriptor).getLength()).isEqualTo(1);

        Element element = (Element) doc.getElementsByTagName(KeyDescriptor).item(0);
        assertTrue(element.hasAttribute("use"));
        assertThat(element.getAttribute("use")).isEqualTo("signing");

        assertThat(element.getElementsByTagName("ds:KeyInfo").getLength()).isEqualTo(1);
        assertThat(element.getElementsByTagName("ds:X509Data").getLength()).isEqualTo(1);

        assertThat(element.getElementsByTagName("ds:X509Certificate").getLength()).isEqualTo(1);
        element = (Element) element.getElementsByTagName("ds:X509Certificate").item(0);
        assertThat(cleanAndFixPem(element.getTextContent())).isEqualTo(cleanAndFixPem(idp1SigningCertificate));
    }

    // disabled until encryption management is fixed
    @Disabled
    @Test
    public void bothSigningAndEncryptionCertificatesArePresent() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(idp2MetadataUrl))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from XML
        String xml = res.getResponse().getContentAsString();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        String KeyDescriptor = "md:KeyDescriptor";
        assertThat(doc.getElementsByTagName(KeyDescriptor).getLength()).isEqualTo(2);


        Element element = (Element) doc.getElementsByTagName(KeyDescriptor).item(0);
        assertTrue(element.hasAttribute("use"));
        assertThat(element.getAttribute("use")).isEqualTo("signing");

        assertThat(element.getElementsByTagName("ds:KeyInfo").getLength()).isEqualTo(1);
        assertThat(element.getElementsByTagName("ds:X509Data").getLength()).isEqualTo(1);

        assertThat(element.getElementsByTagName("ds:X509Certificate").getLength()).isEqualTo(1);
        element = (Element) element.getElementsByTagName("ds:X509Certificate").item(0);
        assertThat(cleanAndFixPem(element.getTextContent())).isEqualTo(cleanAndFixPem(idp2SigningCertificate));


        element = (Element) doc.getElementsByTagName(KeyDescriptor).item(1);
        assertTrue(element.hasAttribute("use"));
        assertThat(element.getAttribute("use")).isEqualTo("encryption");

        assertThat(element.getElementsByTagName("ds:KeyInfo").getLength()).isEqualTo(1);
        assertThat(element.getElementsByTagName("ds:X509Data").getLength()).isEqualTo(1);

        assertThat(element.getElementsByTagName("ds:X509Certificate").getLength()).isEqualTo(1);
        element = (Element) element.getElementsByTagName("ds:X509Certificate").item(0);
        assertThat(cleanAndFixPem(element.getTextContent())).isEqualTo(cleanAndFixPem(idp2CryptCertificate));
    }

    private String cleanAndFixPem(String value) {
        return fixPem(value, "CERTIFICATE").replace("\n", "");
    }

    private String fixPem(String value, String kind) {
        String sep = "-----";
        String begin = "BEGIN " + kind;
        String end = "END " + kind;

        String header = sep + begin + sep;
        String footer = sep + end + sep;

        String[] lines = value.split("\\R");

        if (lines.length > 2) {
            // headers?
            String headerLine = lines[0];
            String footerLine = lines[lines.length - 1];

            if (headerLine.startsWith(sep) && footerLine.startsWith(sep)) {
                // return unchanged, don't mess with content
                return value;
            }
        }

        // rewrite
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (int c = 0; c < lines.length; c++) {
            sb.append(lines[c].trim()).append("\n");
        }
        sb.append(footer);
        return sb.toString();
    }
}
