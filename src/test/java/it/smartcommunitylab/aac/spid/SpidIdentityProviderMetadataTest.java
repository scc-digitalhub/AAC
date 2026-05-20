package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.AbstractIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.provider.FirstIdentityProvider;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.utils.MetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.OrganizationName;
import org.opensaml.saml.saml2.metadata.OrganizationDisplayName;
import org.opensaml.saml.saml2.metadata.OrganizationURL;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for verifying the correct generation and exposure of the
 * Service Provider (SP) Metadata.
 * It ensures that the metadata XML strictly conforms to the SPID technical rules,
 * including valid endpoints, correct bindings (e.g., POST for ACS/SLO),
 * signed payloads, and required SPID attributes.
 */
@SpringBootTest
@AutoConfigureMockMvc
// Loads the base profile ("test") and then applies SPID overrides ("test-spid")
@ActiveProfiles({"test", "test-spid"})
// Add @Transactional to clean up the DB automatically between @Test methods within this class
@Transactional
public class SpidIdentityProviderMetadataTest extends BaseSpidTest {

    @Autowired
    private Environment env;

    @Autowired
    private BootstrapConfig config;

    /* =========================================================================
     * SPID Utility Components
     * ========================================================================= */
    protected MetadataUtils metadataUtils = new MetadataUtils();
    protected FirstIdentityProvider firstIdentityProvider = new FirstIdentityProvider();

    @BeforeEach
    public void setupConfiguration() throws Exception {
        initMockMvc();

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
                ConfigurableIdentityProvider idp = idps.get(0);

                firstIdentityProvider.initReamlByBoostrap(idp, BASE_URL, METADATA_PATH, SSO_PATH);

                firstIdentityProvider.signingIdpAuthority = idp.getAuthority();
                firstIdentityProvider.signingIdpProvider = idp.getProvider();
                firstIdentityProvider.signingIdpSloUrl = BASE_URL + SLO_PATH + AbstractIdentityProvider.encodeRegistrationId(firstIdentityProvider.signingIdpProvider);

                SpidIdentityProviderConfigMap configmap = new SpidIdentityProviderConfigMap();
                configmap.setConfiguration(idp.getConfiguration());

                firstIdentityProvider.signingSetSpidAttributes = configmap.getSpidAttributes();
                firstIdentityProvider.signingCredentials = configmap.getSigningCredentials();
                firstIdentityProvider.signingActiveSigningCredentialId = configmap.getActiveAuthRequestSigningCredentialId();
                firstIdentityProvider.signingIdpSigningKey = configmap.getSigningCredentials().get(1).getSigningKey();
                // Extract clean Base64 string from PEM
                firstIdentityProvider.signingIdpSigningCertificate = configmap.getSigningCredentials().get(1).getSigningCertificate()
                    .replace(BEGIN_CERT, "")
                    .replace(END_CERT, "")
                    .replace("\n", "");
            }
        });
    }

    /**
     * Confirms the metadata endpoint is accessible and returns a non-empty payload.
     */
    @Test
    @DisplayName("Verifica disponibilità e raggiungibilità dell'endpoint Metadata")
    public void testMetadataEndpointIsReachableAndPopulated() throws Exception {
        MvcResult res = this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(res.getResponse().getContentAsString()).isNotBlank();
        assertThat(res.getResponse().getContentAsString()).isNotEmpty();
        assertThat(res.getResponse().getContentAsString()).isNotNull();
    }

    /**
     * Checks if the HTTP response Content-Type is strictly set to XML,
     * as required by SAML standards.
     */
    @Test
    @DisplayName("Verifica che il Content-Type della risposta sia application/xml")
    public void testMetadataContentTypeIsXml() throws Exception {
        MvcResult res = this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(res.getResponse().getContentType(), "application/xml;charset=UTF-8");
    }

    /**
     * Validates the overall structure of the XML using OpenSAML parsers,
     * ensuring that SPSSODescriptor and AssertionConsumerService elements are present.
     */
    @Test
    @DisplayName("Verifica che la struttura XML (EntityDescriptor) sia ben formattata")
    public void testMetadataXmlStructureIsWellFormed() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        SPSSODescriptor spssoDescriptor = descriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        assertThat(spssoDescriptor).isNotNull();

        List<AssertionConsumerService> assertionConsumerServices = spssoDescriptor.getAssertionConsumerServices();
        assertThat(assertionConsumerServices.size()).isGreaterThanOrEqualTo(1);

        assertionConsumerServices.forEach(assertionConsumerService -> {
            assertThat(assertionConsumerService.getBinding()).isNotNull();
            assertThat(assertionConsumerService.getLocation()).isNotNull();
        });
    }

    /**
     * Ensures the EntityID published in the metadata exactly matches the configured one.
     */
    @Test
    @DisplayName("Verifica corrispondenza del valore EntityID")
    public void testEntityIdMatchesConfiguration() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        assertThat(descriptor.getEntityID()).isEqualTo(firstIdentityProvider.signingIdpEntityId);
    }

    /**
     * Verifies that the ACS endpoint uses the required HTTP-POST binding and
     * points to the correct absolute URL for processing SPID responses.
     */
    @Test
    @DisplayName("Verifica binding e URL dell'Assertion Consumer Service (POST)")
    public void testAssertionConsumerServiceLocationAndBinding() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<AssertionConsumerService> assertionConsumerServices = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getAssertionConsumerServices();

        assertThat(assertionConsumerServices.size()).isEqualTo(1);
        assertThat(assertionConsumerServices.get(0).getBinding()).isEqualTo(Saml2MessageBinding.POST.getUrn());
        assertThat(assertionConsumerServices.get(0).getLocation()).isEqualTo(firstIdentityProvider.signingIdpSsoUrl);
    }

    /**
     * Verifies that the SLO endpoint uses the required HTTP-POST binding and
     * points to the correct absolute URL.
     */
    @Test
    @DisplayName("Verifica binding e URL del Single Logout Service (POST)")
    public void testSingleLogoutServiceLocationAndBinding() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<SingleLogoutService> singleLogoutServices = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getSingleLogoutServices();

        assertThat(singleLogoutServices.size()).isEqualTo(1);
        assertThat(singleLogoutServices.get(0).getBinding()).isEqualTo(Saml2MessageBinding.POST.getUrn());
        assertThat(singleLogoutServices.get(0).getLocation()).isEqualTo(firstIdentityProvider.signingIdpSloUrl);
    }

    /**
     * Parses the AttributeConsumingService block to guarantee that all expected
     * SPID attributes (e.g., name, fiscalNumber) are formally requested.
     */
    @Test
    @DisplayName("Verifica che gli attributi SPID richiesti siano esposti nel Metadata")
    public void testRequestedSpidAttributesAreExposed() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<AttributeConsumingService> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getAttributeConsumingServices();

        assertThat(keyDescriptors.size()).isEqualTo(1);

        Set<SpidAttribute> attributes = new HashSet<>();
        for(RequestedAttribute attribute: keyDescriptors.get(0).getRequestedAttributes()){
            attributes.add(SpidAttribute.parse(attribute.getName()));
        }

        assertThat(attributes.size()).isEqualTo(5);
        assertThat(attributes).isEqualTo(firstIdentityProvider.signingSetSpidAttributes);
    }

    /**
     * Confirms that the main XML root signature is present, correctly formatted,
     * and corresponds to the configured Service Provider private key.
     */
    @Test
    @DisplayName("Verifica presenza e validità del certificato di firma root - METADATA_SIGNATURE")
    public void testRootSignatureCertificateIsPresentAndValid() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<X509Data> keyDescriptors = descriptor
            .getSignature()
            .getKeyInfo()
            .getX509Datas();

        assertThat(keyDescriptors.size()).isEqualTo(1);

        List<X509Certificate> certificates = keyDescriptors.get(0).getX509Certificates();
        assertThat(certificates.size()).isEqualTo(1);

        String metadataSignatureCertificate = certificates.get(0).getValue().replace("\n", "");
        assertThat(metadataSignatureCertificate).isNotNull();
        assertThat(metadataSignatureCertificate).isEqualTo(firstIdentityProvider.signingIdpSigningCertificate);

        assertThat(metadataUtils.decodeBase64ToX509Certificate(metadataSignatureCertificate))
            .isEqualTo(metadataUtils.decodeBase64ToX509Certificate(firstIdentityProvider.signingIdpSigningCertificate));
    }

    /**
     * Inspects the SPSSODescriptor block within the generated SAML metadata
     * to ensure all configured X.509 certificates are properly exposed.
     * This guarantees the METADATA_EXPOSURE property is respected.
     */
    @Test
    @DisplayName("Verifica presenza di tutti i cerificati - METADATA_EXPOSURE")
    public void testAllCertificatesExposure() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<KeyDescriptor> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getKeyDescriptors();

        assertThat(keyDescriptors.size()).isEqualTo(3);
    }

    /**
     * Checks the KeyDescriptor block within the SPSSODescriptor to ensure
     * a signing certificate is explicitly provided for establishing trust with the IdP.
     */
    @Test
    @DisplayName("Verifica presenza del KeyDescriptor di tipo SIGNING")
    public void testSigningKeyDescriptorIsPresent() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<KeyDescriptor> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getKeyDescriptors();

        assertThat(keyDescriptors.size()).isEqualTo(3);

        KeyDescriptor keyDescriptor = keyDescriptors.get(1);
        assertThat(keyDescriptor.getUse()).isEqualTo(UsageType.SIGNING);

        List<X509Certificate> certificates = keyDescriptor
            .getKeyInfo()
            .getX509Datas()
            .get(0)
            .getX509Certificates();

        assertThat(certificates.size()).isEqualTo(1);

        String metadataSigningCertificate = certificates.get(0).getValue();
        assertThat(metadataSigningCertificate).isNotNull();

        // Assert equality by fully parsing the X509 certificates
        assertThat(metadataUtils.decodeBase64ToX509Certificate(metadataSigningCertificate))
            .isEqualTo(metadataUtils.decodeBase64ToX509Certificate(firstIdentityProvider.signingIdpSigningCertificate));
    }

    /**
     * Verifies the cryptographic robustness of the metadata signature according to SPID technical rules.
     * Specifically, this test ensures that:
     * 1. The signature algorithm is RSA-SHA256 or higher (e.g., SHA384, SHA512).
     * 2. The digest algorithm is SHA-256 or higher (verified by directly inspecting the XML DOM).
     * 3. The RSA public key has a modulus length of at least 2048 bits.
     */
    @Test
    @DisplayName("Verifica firma: RSA >= 2048 bit, Signature e Digest SHA-256 o superiore")
    public void testMetadataSignatureAlgorithmAndKeySize() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
                this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        descriptor.getSignature().getSignatureAlgorithm();
        Signature signature = descriptor.getSignature();
        assertThat(signature).isNotNull();

        String sigAlgorithm = signature.getSignatureAlgorithm();
        assertThat(sigAlgorithm).isIn(
            SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256,
            SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384,
            SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512
        );
        assertThat(sigAlgorithm).isEqualTo(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);

        Element signatureDom = signature.getDOM();
        assertThat(signatureDom).isNotNull();
        NodeList digestMethods = signatureDom.getElementsByTagNameNS(SignatureConstants.XMLSIG_NS, "DigestMethod");

        assertThat(digestMethods.getLength()).isGreaterThan(0);
        String digestAlgorithm = digestMethods.item(0).getAttributes().getNamedItem("Algorithm").getNodeValue();

        assertThat(digestAlgorithm).isIn(
            SignatureConstants.ALGO_ID_DIGEST_SHA256,
            SignatureConstants.ALGO_ID_DIGEST_SHA384,
            SignatureConstants.ALGO_ID_DIGEST_SHA512
        );
        assertThat(digestAlgorithm).isEqualTo(SignatureConstants.ALGO_ID_DIGEST_SHA256);

        java.security.cert.X509Certificate cert = metadataUtils.decodeBase64ToX509Certificate(firstIdentityProvider.signingIdpSigningCertificate);
        assertThat(cert.getPublicKey()).isInstanceOf(RSAPublicKey.class);
        RSAPublicKey rsaPublicKey = (RSAPublicKey) cert.getPublicKey();
        assertThat(rsaPublicKey.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
    }

    /**
     * Verifies the mathematical validity of the signature by bypassing serialization
     * alterations (such as pretty-printing). It extracts the payload, removes the original
     * signature, restores a clean DOM, and resigns it in memory using the private key.
     * The two cryptographic digests must match exactly.
     */
    @Test
    @DisplayName("Verifica validità crittografica: estrazione, rifirma (SHA-256) e confronto Digest")
    public void testMetadataRefirmSignatureAlgorithmNew() throws Exception {
        // Retrieve the original XML
        MvcResult res = this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl))
                .andExpect(status().isOk())
                .andReturn();
        String originalXml = res.getResponse().getContentAsString();

        // Use extractDigestValueFromXml to read the original one
        String originalDigest = metadataUtils.extractDigestValueFromXml(originalXml);

        // Use resignAndExtractDigest to recalculate it
        String resignedDigest = metadataUtils.resignAndExtractDigest(
            originalXml,
            firstIdentityProvider.signingIdpSigningKey,
            firstIdentityProvider.signingIdpSigningCertificate
        );

        assertThat(resignedDigest).isEqualTo(originalDigest);
    }

    /**
     * Verifies that the metadata contains exactly one SPSSODescriptor,
     * as strictly required by SPID rules, and ensures it explicitly
     * supports the SAML 2.0 protocol.
     */
    @Test
    @DisplayName("Verifica che esista un SOLO SPSSODescriptor e supporti SAML 2.0")
    public void testSpssoDescriptorIsUniqueAndValid() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<RoleDescriptor> allRoleDescriptors = descriptor.getRoleDescriptors();

        List<SPSSODescriptor> spssoDescriptors = allRoleDescriptors.stream()
            .filter(rd -> rd instanceof SPSSODescriptor)
            .map(rd -> (SPSSODescriptor) rd)
            .collect(Collectors.toList());

        assertThat(spssoDescriptors).hasSize(1);

        SPSSODescriptor spssoDescriptor = spssoDescriptors.get(0);
        assertThat(spssoDescriptor.getSupportedProtocols()).containsExactly("urn:oasis:names:tc:SAML:2.0:protocol");
    }

    /**
     * Checks the SPSSODescriptor to ensure the AuthnRequestsSigned
     * attribute is explicitly set to true, enforcing that all
     * authentication requests sent by the Service Provider are signed.
     */
    @Test
    @DisplayName("Verifica che AuthnRequestsSigned sia true nello SPSSODescriptor")
    public void testAuthnRequestsSignedIsTrue() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        SPSSODescriptor spssoDescriptor = descriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");

        assertThat(spssoDescriptor.isAuthnRequestsSigned()).isNotNull();
        assertThat(spssoDescriptor.isAuthnRequestsSigned()).isTrue();
    }

    /**
     * Validates the AssertionConsumerService (ACS) configuration, ensuring
     * the primary (or only) endpoint has its index set to 0 and
     * the isDefault attribute explicitly set to true.
     */
    @Test
    @DisplayName("Verifica attributi index e isDefault dell'Assertion Consumer Service")
    public void testAssertionConsumerServiceIndexAndDefault() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<AssertionConsumerService> assertionConsumerServices = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getAssertionConsumerServices();

        assertThat(assertionConsumerServices).isNotEmpty();

        AssertionConsumerService primaryAcs = assertionConsumerServices.get(0);
        assertThat(primaryAcs.getIndex()).isEqualTo(0);
        assertThat(primaryAcs.isDefault()).isNotNull();
        assertThat(primaryAcs.isDefault()).isTrue();
    }

    /**
     * Confirms the presence of the Organization element and its required
     * children (Name, DisplayName, URL), verifying that each is properly
     * localized in Italian (xml:lang="it") as per SPID guidelines.
     */
    @Test
    @DisplayName("Verifica la presenza dell'elemento Organization e la localizzazione in italiano")
    public void testOrganizationElementsArePresentAndCorrect() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        Organization organization = descriptor.getOrganization();
        assertThat(organization).isNotNull();

        List<OrganizationName> orgNames = organization.getOrganizationNames();
        assertThat(orgNames).isNotEmpty();
        assertThat(orgNames.stream().anyMatch(name -> "it".equals(name.getXMLLang()))).isTrue();

        List<OrganizationDisplayName> orgDisplayNames = organization.getDisplayNames();
        assertThat(orgDisplayNames).isNotEmpty();
        assertThat(orgDisplayNames.stream().anyMatch(name -> "it".equals(name.getXMLLang()))).isTrue();

        List<OrganizationURL> orgUrls = organization.getURLs();
        assertThat(orgUrls).isNotEmpty();
        assertThat(orgUrls.stream().anyMatch(url -> "it".equals(url.getXMLLang()))).isTrue();
    }

    /**
     * Validates the ContactPerson (type "other") and its SPID-specific Extensions,
     * ensuring an email address is present and verifying the inclusion of mandatory
     * legal entity tags (Public/Private and IPACode/VATNumber/FiscalCode).
     */
    @Test
    @DisplayName("Verifica la presenza e configurazione rigorosa del ContactPerson (other)")
    public void testContactPersonOtherIsPresentAndCorrect() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(firstIdentityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<ContactPerson> contactPersons = descriptor.getContactPersons();
        assertThat(contactPersons).isNotEmpty();
        assertThat(contactPersons.size()).isLessThanOrEqualTo(2);

        ContactPerson otherContact = contactPersons.stream()
            .filter(cp -> ContactPersonTypeEnumeration.OTHER.equals(cp.getType()))
            .findFirst()
            .orElse(null);

        assertThat(otherContact).isNotNull();
        assertThat(otherContact.getEmailAddresses()).isNotEmpty();

        String emailValue = otherContact.getEmailAddresses().get(0).getURI();
        assertThat(emailValue).isNotBlank();

        assertThat(otherContact.getExtensions()).isNotNull();

        List<String> extensionTags = otherContact.getExtensions().getUnknownXMLObjects().stream()
            .map(xmlObj -> xmlObj.getElementQName().getLocalPart())
            .toList();

        assertThat(extensionTags).anyMatch(tag ->
            tag.equals("IPACode") || tag.equals("VATNumber") || tag.equals("FiscalCode")
        );
        assertThat(extensionTags).anyMatch(tag ->
            tag.equals("Public") || tag.equals("Private")
        );
    }
}
