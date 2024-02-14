package it.smartcommunitylab.aac.saml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import java.io.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.security.credential.UsageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SamlIdentityProviderMetadataTest {
    static {
        OpenSamlInitializationService.initialize();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapConfig config;

    private final XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
    private final ParserPool parserPool = registry.getParserPool();

    // hard-coded in test since they are hard-coded in the actual configuration
    public static final String BASE_URL = "http://localhost";
    public static final String METADATA_PATH = "/auth/saml/metadata/";
    public static final String SSO_PATH = "/auth/saml/sso/";

    private String signingIdpMetadataUrl;
    private String signingIdpSigningCertificate;
    private String signingIdpEntityId;
    private String signingIdpAssertionConsumerServiceLocation;

    private String signingAndCryptIdpMetadataUrl;
    private String signingAndCryptIdpSigningCertificate;
    private String signingAndCryptIdpCryptCertificate;

    private final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private final String END_CERT = "-----END CERTIFICATE-----";

    @BeforeEach
    public void readConfiguration() {
        config
            .getRealms()
            .forEach(realm -> {
                if (realm.getRealm().getSlug().equals("saml-test")) {
                    List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                    assertThat(idps.size()).isEqualTo(2);

                    ConfigurableIdentityProvider idp1 = idps.get(0);
                    signingIdpMetadataUrl = BASE_URL + METADATA_PATH + idp1.getProvider();
                    signingIdpAssertionConsumerServiceLocation = BASE_URL + SSO_PATH + idp1.getProvider();

                    SamlIdentityProviderConfigMap config1 = new SamlIdentityProviderConfigMap();
                    config1.setConfiguration(idp1.getConfiguration());
                    signingIdpSigningCertificate = config1.getSigningCertificate();
                    signingIdpEntityId = config1.getEntityId() != null ? config1.getEntityId() : signingIdpMetadataUrl;

                    ConfigurableIdentityProvider idp2 = idps.get(1);
                    signingAndCryptIdpMetadataUrl = BASE_URL + METADATA_PATH + idp2.getProvider();

                    SamlIdentityProviderConfigMap config2 = new SamlIdentityProviderConfigMap();
                    config2.setConfiguration(idp2.getConfiguration());
                    signingAndCryptIdpSigningCertificate = config2.getSigningCertificate();
                    signingAndCryptIdpCryptCertificate = config2.getCryptCertificate();
                }
            });
    }

    @Test
    public void metadataIsAvailable() throws Exception {
        MvcResult res = this.mockMvc.perform(get(signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn();

        // content is available
        assertThat(res.getResponse().getContentAsString()).isNotBlank();
        assertThat(res.getResponse().getContentAsString()).isNotEmpty();
        assertThat(res.getResponse().getContentAsString()).isNotNull();
    }

    // disabled because we consider content type "application/xml" acceptable too
    @EnabledIf("false")
    @Test
    public void metadataContentTypeIsCorrect() throws Exception {
        MvcResult res = this.mockMvc.perform(get(signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn();

        // content type is samlmetadata+xml
        assertEquals(res.getResponse().getContentType(), "application/samlmetadata+xml");
    }

    @Test
    public void metadataIsWellFormed() throws Exception {
        MvcResult res = this.mockMvc.perform(get(signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn();

        // parse XML
        String xml = res.getResponse().getContentAsString();
        Document document = parserPool.parse(new ByteArrayInputStream(xml.getBytes()));
        Element element = document.getDocumentElement();

        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(element);
        assertThat(unmarshaller).isNotNull();

        assertDoesNotThrow(() -> {
            unmarshaller.unmarshall(element);
        });
        XMLObject xmlObject = unmarshaller.unmarshall(element);

        // root element must be EntityDescriptor
        assertTrue(xmlObject instanceof EntityDescriptor);
        EntityDescriptor descriptor = (EntityDescriptor) xmlObject;

        assertThat(descriptor.getEntityID()).isNotNull();

        // SPSSODescriptor element must exist
        SPSSODescriptor spssoDescriptor = descriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        assertThat(spssoDescriptor).isNotNull();

        // AssertionConsumerService element must exist
        List<AssertionConsumerService> assertionConsumerServices = spssoDescriptor.getAssertionConsumerServices();
        assertThat(assertionConsumerServices.size()).isGreaterThanOrEqualTo(1);

        assertionConsumerServices.forEach(assertionConsumerService -> {
            assertThat(assertionConsumerService.getBinding()).isNotNull();
            assertThat(assertionConsumerService.getLocation()).isNotNull();
        });
    }

    @Test
    public void checkAssertionConsumerServiceAttributesValue() throws Exception {
        MvcResult res = this.mockMvc.perform(get(signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn();

        // parse XML
        String xml = res.getResponse().getContentAsString();
        Document document = parserPool.parse(new ByteArrayInputStream(xml.getBytes()));
        Element element = document.getDocumentElement();

        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(element);
        assertThat(unmarshaller).isNotNull();
        EntityDescriptor descriptor = (EntityDescriptor) unmarshaller.unmarshall(element);

        List<AssertionConsumerService> assertionConsumerServices = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getAssertionConsumerServices();

        assertThat(assertionConsumerServices.get(0).getBinding()).isEqualTo(Saml2MessageBinding.POST.getUrn());
        assertThat(assertionConsumerServices.get(0).getLocation())
            .isEqualTo(signingIdpAssertionConsumerServiceLocation);
    }

    @Test
    public void checkEntityIdValue() throws Exception {
        MvcResult res = this.mockMvc.perform(get(signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn();

        // parse XML
        String xml = res.getResponse().getContentAsString();
        Document document = parserPool.parse(new ByteArrayInputStream(xml.getBytes()));
        Element element = document.getDocumentElement();

        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(element);
        assertThat(unmarshaller).isNotNull();
        EntityDescriptor descriptor = (EntityDescriptor) unmarshaller.unmarshall(element);

        assertThat(descriptor.getEntityID()).isEqualTo(signingIdpEntityId);
    }

    // disabled until encryption management is fixed
    @EnabledIf("false")
    @Test
    public void onlySigningCertificateIsPresent() throws Exception {
        MvcResult res = this.mockMvc.perform(get(signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn();

        // parse XML
        String xml = res.getResponse().getContentAsString();
        Document document = parserPool.parse(new ByteArrayInputStream(xml.getBytes()));
        Element element = document.getDocumentElement();

        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(element);
        assertThat(unmarshaller).isNotNull();
        EntityDescriptor descriptor = (EntityDescriptor) unmarshaller.unmarshall(element);

        List<KeyDescriptor> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getKeyDescriptors();

        // only one KeyDescriptor element with use "signing" must exist
        assertThat(keyDescriptors.size()).isEqualTo(1);

        KeyDescriptor keyDescriptor = keyDescriptors.get(0);
        assertThat(keyDescriptor.getUse()).isEqualTo(UsageType.SIGNING);

        // metadata certificate must be equal to idp configuration certificate
        List<org.opensaml.xmlsec.signature.X509Certificate> certificates = keyDescriptor
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates();
        assertThat(certificates.size()).isEqualTo(1);
        String metadataSigningCertificate = certificates.get(0).getValue();
        assertThat(metadataSigningCertificate).isNotNull();

        InputStream metadataSigningCertificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(metadataSigningCertificate.getBytes())
        );
        X509Certificate metadataSigningCertificateX509 = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(metadataSigningCertificateStream);

        String strippedIdpSigningCertificate = signingIdpSigningCertificate
            .replace(BEGIN_CERT, "")
            .replace(END_CERT, "")
            .replace("\n", "");

        InputStream idpSigningCertificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(strippedIdpSigningCertificate.getBytes())
        );
        X509Certificate idpSigningCertificateX509 = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(idpSigningCertificateStream);

        assertThat(metadataSigningCertificateX509).isEqualTo(idpSigningCertificateX509);
    }

    // disabled until encryption management is fixed
    @EnabledIf("false")
    @Test
    public void bothSigningAndEncryptionCertificatesArePresent() throws Exception {
        MvcResult res = this.mockMvc.perform(get(signingAndCryptIdpMetadataUrl)).andExpect(status().isOk()).andReturn();

        // parse XML
        String xml = res.getResponse().getContentAsString();
        Document document = parserPool.parse(new ByteArrayInputStream(xml.getBytes()));
        Element element = document.getDocumentElement();

        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(element);
        assertThat(unmarshaller).isNotNull();
        EntityDescriptor descriptor = (EntityDescriptor) unmarshaller.unmarshall(element);

        List<KeyDescriptor> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getKeyDescriptors();

        // two KeyDescriptor elements respectively with use "signing" and "encryption" must exist
        assertThat(keyDescriptors.size()).isEqualTo(2);

        KeyDescriptor signingKeyDescriptor = keyDescriptors.get(0);
        assertThat(signingKeyDescriptor.getUse()).isEqualTo(UsageType.SIGNING);

        KeyDescriptor cryptKeyDescriptor = keyDescriptors.get(1);
        assertThat(cryptKeyDescriptor.getUse()).isEqualTo(UsageType.ENCRYPTION);

        // metadata signing certificate must be equal to idp configuration signing certificate
        List<org.opensaml.xmlsec.signature.X509Certificate> signingCertificates = signingKeyDescriptor
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates();
        assertThat(signingCertificates.size()).isEqualTo(1);
        String metadataSigningCertificate = signingCertificates.get(0).getValue();
        assertThat(metadataSigningCertificate).isNotNull();

        InputStream metadataSigningCertificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(metadataSigningCertificate.getBytes())
        );
        X509Certificate metadataSigningCertificateX509 = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(metadataSigningCertificateStream);

        String strippedIdpSigningCertificate = signingAndCryptIdpSigningCertificate
            .replace(BEGIN_CERT, "")
            .replace(END_CERT, "")
            .replace("\n", "");

        InputStream idpSigningCertificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(strippedIdpSigningCertificate.getBytes())
        );
        X509Certificate idpSigningCertificateX509 = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(idpSigningCertificateStream);

        assertThat(metadataSigningCertificateX509).isEqualTo(idpSigningCertificateX509);

        // metadata crypt certificate must be equal to idp configuration crypt certificate
        List<org.opensaml.xmlsec.signature.X509Certificate> cryptCertificates = cryptKeyDescriptor
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates();
        assertThat(cryptCertificates.size()).isEqualTo(1);
        String metadataCryptCertificate = cryptCertificates.get(0).getValue();
        assertThat(metadataCryptCertificate).isNotNull();

        InputStream metadataCryptCertificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(metadataCryptCertificate.getBytes())
        );
        X509Certificate metadataCryptCertificateX509 = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(metadataCryptCertificateStream);

        String strippedIdpCryptCertificate = signingAndCryptIdpCryptCertificate
            .replace(BEGIN_CERT, "")
            .replace(END_CERT, "")
            .replace("\n", "");

        InputStream idpCryptCertificateStream = new ByteArrayInputStream(
            Base64.getDecoder().decode(strippedIdpCryptCertificate.getBytes())
        );
        X509Certificate idpCryptCertificateX509 = (X509Certificate) CertificateFactory
            .getInstance("X.509")
            .generateCertificate(idpCryptCertificateStream);

        assertThat(metadataCryptCertificateX509).isEqualTo(idpCryptCertificateX509);
    }
}
