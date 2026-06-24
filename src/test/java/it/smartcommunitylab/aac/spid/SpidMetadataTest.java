package it.smartcommunitylab.aac.spid;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.provider.IdentityProvider;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.SigningCredentialHelper;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.setup.BaseSpidTest;
import it.smartcommunitylab.aac.spid.utils.MetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EmailAddress;
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
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for validating the generation and compliance of the SPID SAML 2.0 Metadata (EntityDescriptor).
 * Strictly verifies AgID technical guidelines, including XML structure, cryptographic signatures (RSA/SHA-256),
 * exposed endpoints (ACS/SLO), requested attributes, and mandatory administrative elements (Organization, ContactPerson).
 */
@SpringBootTest
@ActiveProfiles({"test", "test-spid"})
public class SpidMetadataTest extends BaseSpidTest {

    protected MetadataUtils metadataUtils = new MetadataUtils();
    protected IdentityProvider identityProvider = new IdentityProvider();

    @BeforeEach
    public void setupConfiguration() {
        initMockMvc();

        config.getRealms().forEach(realm -> {
            if ("spid-test".equals(realm.getRealm().getSlug())) {
                List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();

                // Retrieve the exhaustive test Identity Provider populated with all metadata fields
                ConfigurableIdentityProvider idpOrganization = idps.stream().filter(
                    idp -> "spid-test-organization".equals(idp.getName()))
                    .findFirst().orElseThrow();

                identityProvider.initRealmByBoostrap(idpOrganization, BASE_URL, METADATA_PATH, SSO_PATH);
            }
        });
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata".
     * Verifica che l'endpoint dei metadata restituisca HTTP 200 e un payload non vuoto.
     * AgID impone la costante raggiungibilità dei metadata all'URL dichiarato per il trust della federazione.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html">Regole Tecniche SPID - Metadata</a>
     */
    @Test
    @DisplayName("Verifica disponibilità e raggiungibilità dell'endpoint Metadata")
    public void testMetadataEndpointIsReachableAndPopulated() throws Exception {
        MvcResult res = this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(res.getResponse().getContentAsString()).isNotBlank();
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata".
     * Verifica che la risposta HTTP dei metadata presenti il Content-Type "application/xml".
     * Le regole AgID stabiliscono che i metadata devono essere documenti XML validi,
     * garantendo il corretto parsing automatico di chiavi e binding tra i nodi SPID.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html">Regole Tecniche SPID - Metadata</a>
     */
    @Test
    @DisplayName("Verifica che il Content-Type della risposta sia application/xml")
    public void testMetadataContentTypeIsXml() throws Exception {
        MvcResult res = this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("application/xml;charset=UTF-8", res.getResponse().getContentType());
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Service Provider".
     * Verifica la presenza e validità di EntityDescriptor, SPSSODescriptor e AssertionConsumerService (ACS).
     * AgID esige che il Service Provider dichiari almeno un ACS con attributi Location e Binding,
     * indispensabili per indicare all'Identity Provider dove inviare la SAML Response di autenticazione.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica che la struttura XML (EntityDescriptor) sia ben formattata")
    public void testMetadataXmlStructureIsWellFormed() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

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
     * REGOLE TECNICHE SPID: Sezione "Metadata".
     * Verifica che l'attributo entityID del metadata corrisponda al valore configurato.
     * AgID richiede che l'entityID sia un identificatore univoco globale (URI) che rappresenta
     * in modo inequivocabile l'entità all'interno della federazione SPID.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html">Regole Tecniche SPID - Metadata</a>
     */
    @Test
    @DisplayName("Verifica corrispondenza del valore EntityID")
    public void testEntityIdMatchesConfiguration() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        assertThat(descriptor.getEntityID()).isEqualTo(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getEntityId()
        );
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Trasmissione dei messaggi (binding)".
     * Verifica che l'Assertion Consumer Service (ACS) sia configurato con URL esatto e binding HTTP-POST.
     * AgID prescrive l'uso esclusivo del binding HTTP-POST per la trasmissione sicura della SAML Response all'SP.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/trasmissione.html#binding-http-post">Regole Tecniche SPID - Binding HTTP-POST</a>
     */
    @Test
    @DisplayName("Verifica binding e URL dell'Assertion Consumer Service (POST)")
    public void testAssertionConsumerServiceLocationAndBinding() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<AssertionConsumerService> assertionConsumerServices = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getAssertionConsumerServices();

        assertThat(assertionConsumerServices.size()).isEqualTo(1);

        assertThat(assertionConsumerServices.get(0).getBinding()).isEqualTo(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
                .getMetadataRelyingPartyRegistration()
                .getAssertionConsumerServiceBinding().getUrn()
        );

        assertThat(assertionConsumerServices.get(0).getLocation()).isEqualTo(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
                .getMetadataRelyingPartyRegistration()
                .getAssertionConsumerServiceLocation().replace("{baseUrl}", BASE_URL)
        );
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Single Logout".
     * Verifica la presenza, il binding (HTTP-POST) e l'URL esatto del Single Logout Service.
     * AgID impone l'esposizione di questo endpoint nei metadata per garantire la terminazione sicura della sessione federata.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-logout.html">Regole Tecniche SPID - Single Logout</a>
     */
    @Test
    @DisplayName("Verifica binding e URL del Single Logout Service (POST)")
    public void testSingleLogoutServiceLocationAndBinding() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<SingleLogoutService> singleLogoutServices = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getSingleLogoutServices();

        assertThat(singleLogoutServices.size()).isEqualTo(1);

        assertThat(singleLogoutServices.get(0).getBinding()).isEqualTo(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
                .getMetadataRelyingPartyRegistration()
                .getSingleLogoutServiceBinding().getUrn()
        );

        assertThat(singleLogoutServices.get(0).getLocation()).isEqualTo(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider)
                .getMetadataRelyingPartyRegistration()
                .getSingleLogoutServiceLocation().replace("{baseUrl}", BASE_URL)
        );
    }

    /**
     * REGOLE TECNICHE SPID: Sezioni "Metadata SP" e "Tabella attributi".
     * Verifica la corretta dichiarazione degli attributi (es. fiscalNumber) nel blocco AttributeConsumingService.
     * AgID richiede che il Service Provider esponga a priori nel metadata il set di attributi necessari al servizio.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica che gli attributi SPID richiesti siano esposti nel Metadata")
    public void testRequestedSpidAttributesAreExposed() throws Exception {
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getSpidAttributes()).isNotEmpty();
        assertThat(configmap.getSpidAttributes())
            .contains(SpidAttribute.SPID_CODE, SpidAttribute.FISCAL_NUMBER);

        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<AttributeConsumingService> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getAttributeConsumingServices();

        assertThat(keyDescriptors.size()).isEqualTo(1);

        Set<SpidAttribute> attributesInMetadata = new HashSet<>();
        for(RequestedAttribute attribute: keyDescriptors.get(0).getRequestedAttributes()){
            attributesInMetadata.add(SpidAttribute.parse(attribute.getName()));
        }

        assertThat(attributesInMetadata).isEqualTo(configmap.getSpidAttributes());
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Certificati".
     * Verifica presenza e validità della firma digitale (XML Signature) sul nodo radice del metadata.
     * AgID esige che il metadata sia firmato con un certificato X.509 per garantirne autenticità e integrità.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#certificati">Regole Tecniche SPID - Certificati Metadata</a>
     * {@code src/test/resources/spid/credential.cnf}
     */
    @Test
    @DisplayName("Verifica presenza e validità del certificato di firma root - METADATA_SIGNATURE")
    public void testRootSignatureCertificateIsPresentAndValid() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<X509Data> keyDescriptors = Objects.requireNonNull(Objects.requireNonNull(descriptor
            .getSignature())
            .getKeyInfo())
            .getX509Datas();

        assertThat(keyDescriptors.size()).isEqualTo(1);

        List<X509Certificate> certificates = keyDescriptors.get(0).getX509Certificates();
        assertThat(certificates.size()).isEqualTo(1);

        String metadataSignatureCertificate = Objects.requireNonNull(certificates.get(0).getValue()).replace("\n", "");
        assertThat(metadataSignatureCertificate).isNotNull();

        String idpSigningCertificate = SigningCredentialHelper.signingCredentialList(
                spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
                SigningCredentialHelper.CredentialPurpose.METADATA_SIGNATURE)
            .get(0).getSigningCertificate()
            .replace(BEGIN_CERT, "")
            .replace(END_CERT, "")
            .replaceAll("\\s+", "");

        assertThat(metadataSignatureCertificate).isEqualTo(idpSigningCertificate);
        assertThat(metadataUtils.decodeBase64ToX509Certificate(metadataSignatureCertificate))
            .isEqualTo(metadataUtils.decodeBase64ToX509Certificate(idpSigningCertificate));
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Certificati".
     * Verifica l'esposizione di tutti i certificati X.509 nel blocco KeyDescriptor (SPSSODescriptor).
     * AgID prescrive che i certificati pubblici (firma/cifratura) siano sempre dichiarati nei metadata
     * per permettere alle controparti di validare le firme o cifrare le comunicazioni SAML.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#certificati">Regole Tecniche SPID - Certificati</a>
     */
    @Test
    @DisplayName("Verifica presenza di tutti i cerificati - METADATA_EXPOSURE")
    public void testAllCertificatesExposure() throws Exception {
        SpidIdentityProviderConfigMap configmap= spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();
        assertThat(configmap.getSigningCredentials()).hasSizeGreaterThanOrEqualTo(2);

        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<KeyDescriptor> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getKeyDescriptors();

        List<SigningCredential> listSigningCredentials = SigningCredentialHelper.signingCredentialList(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
            SigningCredentialHelper.CredentialPurpose.METADATA_EXPOSURE);

        assertThat(listSigningCredentials).hasSizeGreaterThanOrEqualTo(2);

        List<String> expectedCertificates = new ArrayList<>();
        for (SigningCredential signingCredential: listSigningCredentials) {
            if (signingCredential.getSigningCertificate() != null) {
                expectedCertificates.add(signingCredential.getSigningCertificate()
                    .replace(BEGIN_CERT, "")
                    .replace(END_CERT, "")
                    .replaceAll("\\s+", ""));
            }
        }

        List<String> actualCertificates = new ArrayList<>();
        for (KeyDescriptor keyDescriptor : keyDescriptors) {
            String certValue = Objects.requireNonNull(keyDescriptor.getKeyInfo()
                .getX509Datas().get(0)
                .getX509Certificates().get(0)
                .getValue()).replace("\n", "");

            actualCertificates.add(certValue);
        }

        assertThat(actualCertificates).containsExactlyInAnyOrderElementsOf(expectedCertificates);
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Certificati".
     * Verifica che il KeyDescriptor specifichi esplicitamente l'attributo use="signing".
     * AgID lo richiede per indicare univocamente alla controparte il certificato da usare per validare le firme.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#certificati">Regole Tecniche SPID - Certificati</a>
     */
    @Test
    @DisplayName("Verifica presenza del KeyDescriptor di tipo SIGNING")
    public void testSigningKeyDescriptorIsPresent() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<KeyDescriptor> keyDescriptors = descriptor
            .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
            .getKeyDescriptors();

        KeyDescriptor keyDescriptor = keyDescriptors.get(0);
        assertThat(keyDescriptor.getUse()).isEqualTo(UsageType.SIGNING);
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Algoritmi crittografici, di hash e tipologia delle chiavi".
     * Verifica i requisiti minimi di sicurezza crittografica richiesti per la firma del metadata.
     * AgID prescrive standard rigidi per prevenire vulnerabilità nella federazione, imponendo che:
     * 1. L'algoritmo di firma sia almeno RSA-SHA256.
     * 2. L'algoritmo di digest (hash) sia almeno SHA-256.
     * 3. La lunghezza della chiave pubblica RSA sia di almeno 2048 bit.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#algoritmi-crittografici-di-hash-e-tipologia-delle-chiavi">Regole Tecniche SPID - Algoritmi e Chiavi</a>
     */
    @Test
    @DisplayName("Verifica firma: RSA >= 2048 bit, Signature e Digest SHA-256 o superiore")
    public void testMetadataSignatureAlgorithmAndKeySize() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
                this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        Signature signature = descriptor.getSignature();
        assertThat(signature).isNotNull();
        String algorithm = signature.getSignatureAlgorithm();
        assertThat(algorithm).isNotBlank();

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

        String idpSigningCertificate = SigningCredentialHelper.signingCredentialList(
                    spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
                    SigningCredentialHelper.CredentialPurpose.METADATA_SIGNATURE)
            .get(0).getSigningCertificate()
            .replace(BEGIN_CERT, "")
            .replace(END_CERT, "")
            .replace("\n", "");

        java.security.cert.X509Certificate cert = metadataUtils.decodeBase64ToX509Certificate(idpSigningCertificate);
        assertThat(cert.getPublicKey()).isInstanceOf(RSAPublicKey.class);
        RSAPublicKey rsaPublicKey = (RSAPublicKey) cert.getPublicKey();
        assertThat(rsaPublicKey.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Certificati".
     * Verifica la validità matematica della firma digitale XML apposta sul metadata.
     * AgID richiede che la firma garantisca l'integrità del documento; questo test assicura che il
     * digest crittografico calcolato sui dati non alterati corrisponda esattamente a quello originale,
     * confermando che il payload non è stato modificato dopo la firma.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#certificati">Regole Tecniche SPID - Certificati e Firma</a>
     */
    @Test
    @DisplayName("Verifica validità crittografica: estrazione, rifirma (SHA-256) e confronto Digest")
    public void testResignedSignatureDigestMatch() throws Exception {
        // Retrieve the original XML
        MvcResult res = this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl))
                .andExpect(status().isOk())
                .andReturn();
        String originalXml = res.getResponse().getContentAsString();

        // Use extractDigestValueFromXml to read the original one
        String originalDigest = metadataUtils.extractDigestValueFromXml(originalXml);

        SigningCredential signingCertificate = SigningCredentialHelper.signingCredentialList(
            spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap(),
            SigningCredentialHelper.CredentialPurpose.METADATA_SIGNATURE).get(0);

        // Use resignAndExtractDigest to recalculate it
        String resignedDigest = metadataUtils.resignAndExtractDigest(
            originalXml,
            signingCertificate
        );

        assertThat(resignedDigest).isEqualTo(originalDigest);
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Service Provider".
     * Verifica la presenza di un unico SPSSODescriptor e il supporto esclusivo al protocollo SAML 2.0.
     * AgID impone che il Service Provider dichiari esplicitamente la compatibilità con SAML 2.0
     * (urn:oasis:names:tc:SAML:2.0:protocol) per garantire la corretta interazione con gli IdP.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica che esista un SOLO SPSSODescriptor e supporti SAML 2.0")
    public void testSpssoDescriptorIsUniqueAndValid() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

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
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Service Provider".
     * Verifica che l'attributo AuthnRequestsSigned sia esplicitamente impostato a "true".
     * AgID prescrive che il Service Provider dichiari nei propri metadata l'obbligo di
     * firmare digitalmente tutte le richieste di autenticazione (AuthnRequest) inviate agli IdP.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica che AuthnRequestsSigned sia true nello SPSSODescriptor")
    public void testAuthnRequestsSignedIsTrue() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        SPSSODescriptor spssoDescriptor = descriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");

        assertThat(spssoDescriptor.isAuthnRequestsSigned()).isNotNull();
        assertThat(spssoDescriptor.isAuthnRequestsSigned()).isTrue();
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Service Provider".
     * Verifica che l'Assertion Consumer Service principale abbia index="0" e isDefault="true".
     * AgID richiede questi attributi per identificare in modo non ambiguo l'endpoint predefinito
     * a cui l'Identity Provider dovrà inviare la SAML Response dopo l'autenticazione dell'utente.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica attributi index e isDefault dell'Assertion Consumer Service")
    public void testAssertionConsumerServiceIndexAndDefault() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

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
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Service Provider".
     * Verifica presenza, localizzazione in italiano e correttezza dei dati del nodo Organization.
     * AgID richiede che Name, DisplayName e URL riflettano fedelmente i dati dell'ente erogatore.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica la presenza dell'elemento Organization, localizzazione in italiano e corrispondenza dati")
    public void testOrganizationElementsArePresentAndCorrect() throws Exception {
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();

        assertThat(configmap.getOrganizationName()).isNotBlank();
        assertThat(configmap.getOrganizationDisplayName()).isNotBlank();
        assertThat(configmap.getOrganizationUrl()).isNotBlank();
        assertThat(configmap.getContactPersonEmailAddress()).isNotBlank();
        assertThat(configmap.getContactPersonIPACode()).isNotBlank();

        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        Organization organization = descriptor.getOrganization();
        assertThat(organization).isNotNull();

        // OrganizationName
        List<OrganizationName> orgNames = organization.getOrganizationNames();
        assertThat(orgNames).isNotEmpty();
        assertThat(orgNames.stream().anyMatch(name -> "it".equals(name.getXMLLang()))).isTrue();
        assertThat(orgNames).extracting(OrganizationName::getValue)
            .containsExactly(configmap.getOrganizationName());

        // OrganizationDisplayName
        List<OrganizationDisplayName> orgDisplayNames = organization.getDisplayNames();
        assertThat(orgDisplayNames).isNotEmpty();
        assertThat(orgDisplayNames.stream().anyMatch(name -> "it".equals(name.getXMLLang()))).isTrue();
        assertThat(orgDisplayNames).extracting(OrganizationDisplayName::getValue)
            .containsExactly(configmap.getOrganizationDisplayName());

        // OrganizationURL
        List<OrganizationURL> orgUrls = organization.getURLs();
        assertThat(orgUrls).isNotEmpty();
        assertThat(orgUrls.stream().anyMatch(url -> "it".equals(url.getXMLLang()))).isTrue();
        assertThat(orgUrls).extracting(OrganizationURL::getURI)
            .containsExactly(configmap.getOrganizationUrl());
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Service Provider".
     * Verifica la presenza e la corretta formattazione del nodo ContactPerson di tipo "other".
     * AgID richiede esplicitamente questo nodo per fornire i riferimenti tecnici e amministrativi
     * dell'ente (es. per la fatturazione o il supporto). Oltre all'indirizzo email, le regole
     * impongono l'uso di specifiche estensioni XML (namespace fpa) per definire inequivocabilmente
     * la natura giuridica del soggetto (Public/Private) e il suo identificativo primario
     * (IPACode, VATNumber o FiscalCode). Il test assicura l'esistenza di tali tag e la loro
     * perfetta aderenza ai valori dichiarati nel file di configurazione.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica la presenza e configurazione rigorosa del ContactPerson (other)")
    public void testContactPersonOtherIsPresentAndCorrect() throws Exception {
        SpidIdentityProviderConfigMap configmap = spidProviderConfigRepository.findByProviderId(identityProvider.signingIdpProvider).getConfigMap();

        assertThat(configmap.getContactPersonEmailAddress()).isNotBlank();
        assertThat(configmap.getContactPersonIPACode()).isNotBlank();

        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<ContactPerson> contactPersons = descriptor.getContactPersons();
        assertThat(contactPersons).isNotEmpty();
        assertThat(contactPersons.size()).isLessThanOrEqualTo(2);

        ContactPerson otherContact = contactPersons.stream()
            .filter(cp -> ContactPersonTypeEnumeration.OTHER.equals(cp.getType()))
            .findFirst()
            .orElse(null);

        assertThat(otherContact).isNotNull();

        // Verify Email: presence and exact match
        assertThat(otherContact.getEmailAddresses()).isNotEmpty();

        // Normalize the expected email by removing the "mailto:" prefix if present in the config
        String expectedEmail = configmap.getContactPersonEmailAddress().replace("mailto:", "");

        assertThat(otherContact.getEmailAddresses())
            .extracting(EmailAddress::getURI)
            .map(email -> email.replace("mailto:", "")) // Normalize the actual value as well for safety
            .containsExactly(expectedEmail);

        // Verify Extensions: presence of mandatory tags
        assertThat(otherContact.getExtensions()).isNotNull();
        List<XMLObject> extensions = otherContact.getExtensions().getUnknownXMLObjects();
        List<String> extensionTags = extensions.stream()
            .map(xmlObj -> xmlObj.getElementQName().getLocalPart())
            .toList();

        assertThat(extensionTags).anyMatch(tag ->
            tag.equals("IPACode") || tag.equals("VATNumber") || tag.equals("FiscalCode")
        );
        assertThat(extensionTags).anyMatch(tag ->
            tag.equals("Public") || tag.equals("Private")
        );

        // Verify Extensions: exact value match against configuration (e.g., IPACode)
        Optional<XMLObject> ipaCodeElement = extensions.stream()
            .filter(xmlObj -> "IPACode".equals(xmlObj.getElementQName().getLocalPart()))
            .findFirst();

        assertThat(ipaCodeElement).isPresent();
        assertThat(ipaCodeElement.get().getDOM().getTextContent()).isEqualTo(configmap.getContactPersonIPACode());
    }

    /**
     * REGOLE TECNICHE SPID: Sezione "Metadata" -> "Service Provider".
     * Verifica la conformità dei formati NameID dichiarati dallo SPSSODescriptor.
     * In ambito SPID, il formato ammesso per le persone fisiche è esclusivamente "transient".
     * AgID e gli Identity Provider accettano l'omissione totale del tag (assumendolo come default implicito),
     * ma qualora il Service Provider decida di esplicitarlo, il test garantisce che sia impostato
     * tassativamente su "urn:oasis:names:tc:SAML:2.0:nameid-format:transient".
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica conformità del NameIDFormat (se presente, deve essere transient)")
    public void testNameIdFormatIsTransient() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
            this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        SPSSODescriptor spssoDescriptor = descriptor.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        assertThat(spssoDescriptor).isNotNull();

        // Extract all declared NameIDFormat elements
        List<String> nameIdFormats = spssoDescriptor.getNameIDFormats().stream()
            .map(format -> format.getURI()) // Or getValue() depending on your OpenSAML version
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // Real-world compliance: if the list is not empty, it MUST contain the transient format
        if (!nameIdFormats.isEmpty()) {
            assertThat(nameIdFormats)
                .as("If NameIDFormat is explicitly declared, it must be 'transient' for SPID compliance")
                .contains("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
        }
    }

    /**
     * REGOLE TECNICHE SPID: Sezioni "Metadata SP" e "Tabella attributi".
     * Verifica la corretta indicizzazione dell'AttributeConsumingService.
     * Le regole SAML e il profilo SPID richiedono che il blocco contenente gli attributi
     * esibisca l'attributo "index", fondamentale affinché la successiva AuthnRequest possa
     * referenziare in modo univoco il set di attributi (AttributeConsumingServiceIndex) richiesto.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider">Regole Tecniche SPID - Metadata SP</a>
     */
    @Test
    @DisplayName("Verifica l'attributo index dell'AttributeConsumingService")
    public void testAttributeConsumingServiceIndex() throws Exception {
        EntityDescriptor descriptor = metadataUtils.extractEntityDescriptorFromMvcResult(
                this.mockMvc.perform(get(identityProvider.signingIdpMetadataUrl)).andExpect(status().isOk()).andReturn());

        List<AttributeConsumingService> attributeConsumingServices = descriptor
                .getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
                .getAttributeConsumingServices();

        assertThat(attributeConsumingServices).isNotEmpty();

        AttributeConsumingService primaryAttributeService = attributeConsumingServices.get(0);

        // AgID only requires the index to be present as a non-negative identifying integer
        assertThat(primaryAttributeService.getIndex()).isNotNull();
        assertThat(primaryAttributeService.getIndex()).isGreaterThanOrEqualTo(0);
    }
}
