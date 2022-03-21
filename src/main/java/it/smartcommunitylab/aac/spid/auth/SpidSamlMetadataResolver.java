package it.smartcommunitylab.aac.spid.auth;

import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.RandomStringUtils;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EmailAddress;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.OrganizationDisplayName;
import org.opensaml.saml.saml2.metadata.OrganizationName;
import org.opensaml.saml.saml2.metadata.OrganizationURL;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.ServiceName;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.TelephoneNumber;
import org.opensaml.saml.saml2.metadata.impl.AssertionConsumerServiceBuilder;
import org.opensaml.saml.saml2.metadata.impl.AttributeConsumingServiceBuilder;
import org.opensaml.saml.saml2.metadata.impl.ContactPersonBuilder;
import org.opensaml.saml.saml2.metadata.impl.EmailAddressBuilder;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorMarshaller;
import org.opensaml.saml.saml2.metadata.impl.ExtensionsBuilder;
import org.opensaml.saml.saml2.metadata.impl.KeyDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.OrganizationBuilder;
import org.opensaml.saml.saml2.metadata.impl.OrganizationDisplayNameBuilder;
import org.opensaml.saml.saml2.metadata.impl.OrganizationNameBuilder;
import org.opensaml.saml.saml2.metadata.impl.OrganizationURLBuilder;
import org.opensaml.saml.saml2.metadata.impl.RequestedAttributeBuilder;
import org.opensaml.saml.saml2.metadata.impl.SPSSODescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.ServiceNameBuilder;
import org.opensaml.saml.saml2.metadata.impl.SingleLogoutServiceBuilder;
import org.opensaml.saml.saml2.metadata.impl.TelephoneNumberBuilder;
import org.opensaml.saml.security.impl.SAMLMetadataSignatureSigningParametersResolver;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.SignatureSigningParametersResolver;
import org.opensaml.xmlsec.criterion.SignatureSigningConfigurationCriterion;
import org.opensaml.xmlsec.impl.BasicSignatureSigningConfiguration;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.impl.KeyInfoBuilder;
import org.opensaml.xmlsec.signature.impl.X509CertificateBuilder;
import org.opensaml.xmlsec.signature.impl.X509DataBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureSupport;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.metadata.Saml2MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

public class SpidSamlMetadataResolver implements Saml2MetadataResolver {

    public static final String SPID_NAMESPACE_URI = "https://spid.gov.it/saml-extensions";
    public static final String SPID_NAMESPACE_PREFIX = "spid";
    private static final String DEFAULT_SERVICE_NAME = "Set0";

    static {
        OpenSamlInitializationService.initialize();
    }

    private final ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository;

    private final EntityDescriptorMarshaller marshaller;

    // init all builders
    private final EntityDescriptorBuilder entityDescriptorBuilder;
    private final SPSSODescriptorBuilder spssoDescriptorBuilder;
    private final AssertionConsumerServiceBuilder assertionConsumerServiceBuilder;
    private final AttributeConsumingServiceBuilder attributeConsumingServiceBuilder;
    private final SingleLogoutServiceBuilder singleLogoutServiceBuilder;

    private final KeyDescriptorBuilder keyDescriptorBuilder;
    private final KeyInfoBuilder keyInfoBuilder;
    private final X509CertificateBuilder x509CertificateBuilder;
    private final X509DataBuilder x509DataBuilder;

    private final OrganizationBuilder organizationBuilder;
    private final OrganizationNameBuilder organizationNameBuilder;
    private final OrganizationDisplayNameBuilder organizationDisplayNameBuilder;
    private final OrganizationURLBuilder organizationURLBuilder;

    private final ContactPersonBuilder contactPersonBuilder;
    private final EmailAddressBuilder emailAddressBuilder;
    private final TelephoneNumberBuilder telephoneNumberBuilder;

    private final ServiceNameBuilder serviceNameBuilder;
    private final RequestedAttributeBuilder requestedAttributeBuilder;
//    private final NamespaceBuilder namespaceBuilder;
    private final ExtensionsBuilder extensionsBuilder;
    private final XSAnyBuilder xsAnyBuilder;
//    private final XMLObjectBuilder xmlObjectBuilder;

    public SpidSamlMetadataResolver(
            ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository) {
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        this.registrationRepository = registrationRepository;

        // fetch opensaml builders
        XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
        this.marshaller = (EntityDescriptorMarshaller) registry
                .getMarshallerFactory().getMarshaller(EntityDescriptor.DEFAULT_ELEMENT_NAME);

        this.entityDescriptorBuilder = (EntityDescriptorBuilder) registry.getBuilderFactory()
                .getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        this.spssoDescriptorBuilder = (SPSSODescriptorBuilder) registry.getBuilderFactory()
                .getBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        this.assertionConsumerServiceBuilder = (AssertionConsumerServiceBuilder) registry.getBuilderFactory()
                .getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        this.attributeConsumingServiceBuilder = getBuilder(registry, AttributeConsumingService.DEFAULT_ELEMENT_NAME);
        this.singleLogoutServiceBuilder = getBuilder(registry, SingleLogoutService.DEFAULT_ELEMENT_NAME);

        this.keyDescriptorBuilder = (KeyDescriptorBuilder) registry.getBuilderFactory()
                .getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME);
        this.keyInfoBuilder = (KeyInfoBuilder) registry.getBuilderFactory()
                .getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME);
        this.x509CertificateBuilder = (X509CertificateBuilder) registry.getBuilderFactory()
                .getBuilder(X509Certificate.DEFAULT_ELEMENT_NAME);
        this.x509DataBuilder = (X509DataBuilder) registry.getBuilderFactory()
                .getBuilder(X509Data.DEFAULT_ELEMENT_NAME);

        this.organizationBuilder = (OrganizationBuilder) registry.getBuilderFactory()
                .getBuilder(Organization.DEFAULT_ELEMENT_NAME);
        this.organizationNameBuilder = (OrganizationNameBuilder) registry.getBuilderFactory()
                .getBuilder(OrganizationName.DEFAULT_ELEMENT_NAME);
        this.organizationDisplayNameBuilder = (OrganizationDisplayNameBuilder) registry.getBuilderFactory()
                .getBuilder(OrganizationDisplayName.DEFAULT_ELEMENT_NAME);
        this.organizationURLBuilder = (OrganizationURLBuilder) registry.getBuilderFactory()
                .getBuilder(OrganizationURL.DEFAULT_ELEMENT_NAME);

        this.contactPersonBuilder = (ContactPersonBuilder) registry.getBuilderFactory()
                .getBuilder(ContactPerson.DEFAULT_ELEMENT_NAME);
        this.emailAddressBuilder = (EmailAddressBuilder) registry.getBuilderFactory()
                .getBuilder(EmailAddress.DEFAULT_ELEMENT_NAME);
        this.telephoneNumberBuilder = (TelephoneNumberBuilder) registry.getBuilderFactory()
                .getBuilder(TelephoneNumber.DEFAULT_ELEMENT_NAME);

        this.serviceNameBuilder = getBuilder(registry, ServiceName.DEFAULT_ELEMENT_NAME);
        this.requestedAttributeBuilder = getBuilder(registry, RequestedAttribute.DEFAULT_ELEMENT_NAME);
        this.extensionsBuilder = new ExtensionsBuilder();
        this.xsAnyBuilder = new XSAnyBuilder();

    }

    @SuppressWarnings("unchecked")
    private <T> T getBuilder(XMLObjectProviderRegistry registry, QName elementName) {
        XMLObjectBuilder<?> builder = registry.getBuilderFactory().getBuilder(elementName);
        if (builder == null) {
            throw new Saml2Exception("Unable to resolve Builder for " + elementName);
        }
        return (T) builder;
    }

    @Override
    public String resolve(RelyingPartyRegistration relyingPartyRegistration) {
        // fetch registration id and complete configuration
        String registrationId = relyingPartyRegistration.getRegistrationId();

        // registrationId is providerId for metadata
        String providerId = registrationId;
//        String[] kp = registrationId.split("-");
//        if (kp.length < 2) {
//            Saml2Error saml2Error = new Saml2Error(Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
//                    "No relying party registration found");
//            throw new Saml2AuthenticationException(saml2Error);
//        }
//        String providerId = kp[0];
//        String idpKey = registrationId.substring(providerId.length() + 1);

        SpidIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);
        if (providerConfig == null) {
            Saml2Error saml2Error = new Saml2Error(Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                    "No relying party registration found for " + String.valueOf(registrationId));
            throw new Saml2AuthenticationException(saml2Error);
        }

        EntityDescriptor entityDescriptor = build(relyingPartyRegistration, providerConfig);

        // add signature
        SignatureSigningParameters signParameters = getSigningParameters(providerConfig, relyingPartyRegistration);
        // register keyinfo in signature as per spec
        signParameters.setKeyInfoGenerator(x509KeyInfoGenerator());

        try {
            SignatureSupport.signObject(entityDescriptor, signParameters);
        } catch (SecurityException | MarshallingException | SignatureException e) {
            throw new Saml2Exception(e);
        }

        try {
            // marshall to xml
            Element element = this.marshaller.marshall(entityDescriptor);
            // note: we need to export the same string as the one used for signing
            // pretty-print will invalidate signature
            return SerializeSupport.nodeToString(element);
        } catch (MarshallingException e) {
            throw new Saml2Exception(e);
        }
    }

    private KeyInfoGenerator x509KeyInfoGenerator() {
        X509KeyInfoGeneratorFactory generator = new X509KeyInfoGeneratorFactory();
        generator.setEmitEntityCertificate(true);
        generator.setEmitEntityCertificateChain(true);
        return generator.newInstance();
    }

    private SignatureSigningParameters getSigningParameters(SpidIdentityProviderConfig providerConfig,
            RelyingPartyRegistration relyingPartyRegistration) {
        List<Credential> credentials = providerConfig.getRelyingPartySigningCredentials();
        List<String> algorithms = Collections.singletonList(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        List<String> digests = Collections.singletonList(SignatureConstants.ALGO_ID_DIGEST_SHA256);
        String canonicalization = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;
        SignatureSigningParametersResolver resolver = new SAMLMetadataSignatureSigningParametersResolver();
        CriteriaSet criteria = new CriteriaSet();
        BasicSignatureSigningConfiguration signingConfiguration = new BasicSignatureSigningConfiguration();
        signingConfiguration.setSigningCredentials(credentials);
        signingConfiguration.setSignatureAlgorithms(algorithms);
        signingConfiguration.setSignatureReferenceDigestMethods(digests);
        signingConfiguration.setSignatureCanonicalizationAlgorithm(canonicalization);
        criteria.add(new SignatureSigningConfigurationCriterion(signingConfiguration));
        try {
            SignatureSigningParameters parameters = resolver.resolveSingle(criteria);
            Assert.notNull(parameters, "Failed to resolve any signing credential");
            return parameters;
        } catch (Exception ex) {
            throw new Saml2Exception(ex);
        }
    }

    private EntityDescriptor build(RelyingPartyRegistration relyingPartyRegistration,
            SpidIdentityProviderConfig providerConfig) {

        String registrationId = relyingPartyRegistration.getRegistrationId();
        SpidIdentityProviderConfigMap configMap = providerConfig.getConfigMap();

        // extract baseUrl
        Map<String, String> acsVariables = new HashMap<>();
        acsVariables.put("baseUrl", "");
        acsVariables.put("registrationId", registrationId);
        UriComponents acsComponents = UriComponentsBuilder
                .fromUriString(SpidIdentityProviderConfig.DEFAULT_CONSUMER_URL)
                .replaceQuery(null).fragment(null)
                .buildAndExpand(acsVariables);
        UriComponents uriComponents = UriComponentsBuilder
                .fromHttpUrl(relyingPartyRegistration.getAssertionConsumerServiceLocation())
                .replacePath(acsComponents.getPath()).replaceQuery(null).fragment(null).build();
        String baseUrl = uriComponents.toUriString();

        // build descriptor as per spid requirements
        EntityDescriptor entityDescriptor = entityDescriptorBuilder.buildObject();
        entityDescriptor.setEntityID(relyingPartyRegistration.getEntityId());
        entityDescriptor.setID(generateId());

        // add namespaces
        Namespace spidNamespace = new Namespace(SPID_NAMESPACE_URI, SPID_NAMESPACE_PREFIX);
        entityDescriptor.getNamespaceManager().registerNamespaceDeclaration(spidNamespace);
        Set<Namespace> nss = entityDescriptor.getNamespaces();

        SPSSODescriptor spSsoDescriptor = spssoDescriptorBuilder.buildObject();
        spSsoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        spSsoDescriptor.setAuthnRequestsSigned(true);
        spSsoDescriptor.setWantAssertionsSigned(true);

        // signing key, only one supported
        Saml2X509Credential credential = relyingPartyRegistration.getSigningX509Credentials().stream()
                .findFirst().orElse(null);
        KeyDescriptor signingKey = keyDescriptorBuilder.buildObject();
        KeyInfo keyInfo = keyInfoBuilder.buildObject();
        X509Certificate x509Certificate = x509CertificateBuilder.buildObject();
        X509Data x509Data = x509DataBuilder.buildObject();
        try {
            x509Certificate.setValue(new String(Base64.getEncoder().encode(credential.getCertificate().getEncoded())));
        } catch (CertificateEncodingException ex) {
            throw new Saml2Exception("Cannot encode certificate " + credential.getCertificate().toString());
        }
        x509Data.getX509Certificates().add(x509Certificate);
        keyInfo.getX509Datas().add(x509Data);
        signingKey.setUse(UsageType.SIGNING);
        signingKey.setKeyInfo(keyInfo);
        spSsoDescriptor.getKeyDescriptors().add(signingKey);

        // assertion consumer service
        AssertionConsumerService assertionConsumerService = assertionConsumerServiceBuilder.buildObject();
        assertionConsumerService.setLocation(relyingPartyRegistration.getAssertionConsumerServiceLocation());
        assertionConsumerService.setBinding(relyingPartyRegistration.getAssertionConsumerServiceBinding().getUrn());
        assertionConsumerService.setIndex(0);
        assertionConsumerService.setIsDefault(true);
        spSsoDescriptor.getAssertionConsumerServices().add(assertionConsumerService);

        // single attribute consuming service
        AttributeConsumingService attributeConsumingService = attributeConsumingServiceBuilder.buildObject();
        attributeConsumingService.setIndex(0);
        attributeConsumingService.setIsDefault(true);

        ServiceName attributeConsumingServiceName = serviceNameBuilder.buildObject();
        attributeConsumingServiceName.setXMLLang("it");
        attributeConsumingServiceName.setValue(DEFAULT_SERVICE_NAME);
        attributeConsumingService.getNames().add(attributeConsumingServiceName);

        configMap.getSpidAttributes().stream().forEach(attr -> {
            RequestedAttribute attribute = requestedAttributeBuilder.buildObject();
            attribute.setName(attr.getValue());
            attributeConsumingService.getRequestAttributes().add(attribute);
        });
        spSsoDescriptor.getAttributeConsumingServices().add(attributeConsumingService);

        // single logout
        String sloLocation = resolveUrlTemplate(
                providerConfig.getRelyingPartyRegistrationSingleLogoutConsumerServiceLocation(),
                baseUrl, relyingPartyRegistration);
        SingleLogoutService sloService = singleLogoutServiceBuilder.buildObject();
        sloService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        sloService.setLocation(sloLocation);
        spSsoDescriptor.getSingleLogoutServices().add(sloService);

        entityDescriptor.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME).add(spSsoDescriptor);

        // spid organization info
        Organization organization = organizationBuilder.buildObject();

        OrganizationName organizationName = organizationNameBuilder.buildObject();
        organizationName.setXMLLang("it");
        organizationName.setValue(configMap.getOrganizationName());
        organization.getOrganizationNames().add(organizationName);

        OrganizationDisplayName organizationDisplayName = organizationDisplayNameBuilder.buildObject();
        organizationDisplayName.setXMLLang("it");
        organizationDisplayName.setValue(configMap.getOrganizationDisplayName());
        organization.getDisplayNames().add(organizationDisplayName);

        OrganizationURL organizationUrl = organizationURLBuilder.buildObject();
        organizationUrl.setXMLLang("it");
        organizationUrl.setValue(configMap.getOrganizationUrl());
        organization.getURLs().add(organizationUrl);

        entityDescriptor.setOrganization(organization);

        // spid contactperson
        boolean isPublic = true;
        if (configMap.getContactPersonPublic() != null) {
            isPublic = configMap.getContactPersonPublic().booleanValue();
        }

        ContactPerson contactPerson = contactPersonBuilder.buildObject();
        contactPerson.setType(ContactPersonTypeEnumeration.OTHER);

        EmailAddress emailAddress = emailAddressBuilder.buildObject();
        emailAddress.setAddress(configMap.getContactPersonEmailAddress());
        contactPerson.getEmailAddresses().add(emailAddress);

        if (StringUtils.hasText(configMap.getContactPersonTelephoneNumber())) {
            TelephoneNumber telephoneNumber = telephoneNumberBuilder.buildObject();
            telephoneNumber.setNumber(configMap.getContactPersonTelephoneNumber());
            contactPerson.getTelephoneNumbers().add(telephoneNumber);
        }

        Extensions contactExtension = extensionsBuilder.buildObject();
        List<XMLObject> contactExtensions = new ArrayList<>();
        if (StringUtils.hasText(configMap.getContactPersonVATNumber())) {
            XSAny vatNumber = xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "VATNumber", SPID_NAMESPACE_PREFIX);
            vatNumber.setTextContent(configMap.getContactPersonVATNumber());
            contactExtensions.add(vatNumber);
        }
        if (StringUtils.hasText(configMap.getContactPersonIPACode()) && isPublic) {
            XSAny ipaCode = xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "IPACode", SPID_NAMESPACE_PREFIX);
            ipaCode.setTextContent(configMap.getContactPersonIPACode());
            contactExtensions.add(ipaCode);
        }
        if (StringUtils.hasText(configMap.getContactPersonFiscalCode()) && !isPublic) {
            XSAny fiscalCode = xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "FiscalCode", SPID_NAMESPACE_PREFIX);
            fiscalCode.setTextContent(configMap.getContactPersonFiscalCode());
            contactExtensions.add(fiscalCode);
        }

        if (isPublic) {
            contactExtensions.add(xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "Public", SPID_NAMESPACE_PREFIX));
        } else {
            contactExtensions.add(xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "Private", SPID_NAMESPACE_PREFIX));
        }
        contactExtension.getUnknownXMLObjects().addAll(contactExtensions);
        contactPerson.setExtensions(contactExtension);

        entityDescriptor.getContactPersons().add(contactPerson);

        return entityDescriptor;
    }

    private String generateId() {
        return "_".concat(RandomStringUtils.randomAlphanumeric(39)).toLowerCase();
    }

    private static String resolveUrlTemplate(String template, String baseUrl, RelyingPartyRegistration relyingParty) {
        String entityId = relyingParty.getAssertingPartyDetails().getEntityId();
        String registrationId = relyingParty.getRegistrationId();
        Map<String, String> uriVariables = new HashMap<>();
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(baseUrl).replaceQuery(null).fragment(null)
                .build();
        String scheme = uriComponents.getScheme();
        uriVariables.put("baseScheme", (scheme != null) ? scheme : "");
        String host = uriComponents.getHost();
        uriVariables.put("baseHost", (host != null) ? host : "");
        // following logic is based on HierarchicalUriComponents#toUriString()
        int port = uriComponents.getPort();
        uriVariables.put("basePort", (port == -1) ? "" : ":" + port);
        String path = uriComponents.getPath();
        if (StringUtils.hasLength(path) && path.charAt(0) != PATH_DELIMITER) {
            path = PATH_DELIMITER + path;
        }
        uriVariables.put("basePath", (path != null) ? path : "");
        uriVariables.put("baseUrl", uriComponents.toUriString());
        uriVariables.put("entityId", StringUtils.hasText(entityId) ? entityId : "");
        uriVariables.put("registrationId", StringUtils.hasText(registrationId) ? registrationId : "");
        return UriComponentsBuilder.fromUriString(template).buildAndExpand(uriVariables).toUriString();
    }

    private static final char PATH_DELIMITER = '/';

}
