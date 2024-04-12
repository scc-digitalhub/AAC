/*
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.saml.saml2.metadata.impl.*;
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

/*
 * Partial references:
 *   [1] https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider
 *   [2] https://docs.oasis-open.org/security/saml/v2.0/saml-metadata-2.0-os.pdf
 */
public class SpidMetadataResolver implements Saml2MetadataResolver {
    static {
        OpenSamlInitializationService.initialize();
    }

    public static final String SPID_NAMESPACE_URI = "https://spid.gov.it/saml-extensions";

    public static final String SPID_NAMESPACE_PREFIX = "spid";
    private static final String DEFAULT_SERVICE_NAME = "Set0";
    private static final char PATH_DELIMITER = '/';

    private final ProviderConfigRepository<SpidIdentityProviderConfig> configRepository;
    private final EntityDescriptorMarshaller edMarshaller;

    // XML builder(s)
    private final EntityDescriptorBuilder entityDescriptorBuilder;
    private final SPSSODescriptorBuilder spSsoDescriptorBuilder;
    private final KeyDescriptorBuilder keyDescriptorBuilder;
    private final KeyInfoBuilder keyInfoBuilder;
    private final X509CertificateBuilder x509CertificateBuilder;
    private final X509DataBuilder x509DataBuilder;
    private final AssertionConsumerServiceBuilder assertionConsumerServiceBuilder;
    private final AttributeConsumingServiceBuilder attributeConsumingServiceBuilder;
    private final SingleLogoutServiceBuilder singleLogoutServiceBuilder;
    private final ServiceNameBuilder serviceNameBuilder;
    private final RequestedAttributeBuilder requestedAttributeBuilder;
    private final OrganizationBuilder organizationBuilder;
    private final OrganizationNameBuilder organizationNameBuilder;
    private final OrganizationDisplayNameBuilder organizationDisplayNameBuilder;
    private final OrganizationURLBuilder organizationURLBuilder;
    private final ContactPersonBuilder contactPersonBuilder;
    private final EmailAddressBuilder emailAddressBuilder;
    private final TelephoneNumberBuilder telephoneNumberBuilder;
    private final ExtensionsBuilder extensionsBuilder;
    private final XSAnyBuilder xsAnyBuilder;

    public SpidMetadataResolver(ProviderConfigRepository<SpidIdentityProviderConfig> configRepository) {
        this.configRepository = configRepository;
        // init builders
        XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
        this.entityDescriptorBuilder =
            (EntityDescriptorBuilder) registry.getBuilderFactory().getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        this.spSsoDescriptorBuilder =
            (SPSSODescriptorBuilder) registry.getBuilderFactory().getBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        this.keyDescriptorBuilder =
            (KeyDescriptorBuilder) registry.getBuilderFactory().getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME);
        this.keyInfoBuilder = (KeyInfoBuilder) registry.getBuilderFactory().getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME);
        this.x509CertificateBuilder =
            (X509CertificateBuilder) registry.getBuilderFactory().getBuilder(X509Certificate.DEFAULT_ELEMENT_NAME);
        this.x509DataBuilder = (X509DataBuilder) registry.getBuilderFactory().getBuilder(X509Data.DEFAULT_ELEMENT_NAME);
        this.assertionConsumerServiceBuilder =
            (AssertionConsumerServiceBuilder) registry
                .getBuilderFactory()
                .getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        this.attributeConsumingServiceBuilder =
            (AttributeConsumingServiceBuilder) registry
                .getBuilderFactory()
                .getBuilder(AttributeConsumingService.DEFAULT_ELEMENT_NAME);
        this.singleLogoutServiceBuilder =
            (SingleLogoutServiceBuilder) registry
                .getBuilderFactory()
                .getBuilder(SingleLogoutService.DEFAULT_ELEMENT_NAME);
        this.serviceNameBuilder =
            (ServiceNameBuilder) registry.getBuilderFactory().getBuilder(ServiceName.DEFAULT_ELEMENT_NAME);
        this.requestedAttributeBuilder =
            (RequestedAttributeBuilder) registry
                .getBuilderFactory()
                .getBuilder(RequestedAttribute.DEFAULT_ELEMENT_NAME);
        this.organizationBuilder =
            (OrganizationBuilder) registry.getBuilderFactory().getBuilder(Organization.DEFAULT_ELEMENT_NAME);
        this.organizationNameBuilder =
            (OrganizationNameBuilder) registry.getBuilderFactory().getBuilder(OrganizationName.DEFAULT_ELEMENT_NAME);
        this.organizationDisplayNameBuilder =
            (OrganizationDisplayNameBuilder) registry
                .getBuilderFactory()
                .getBuilder(OrganizationDisplayName.DEFAULT_ELEMENT_NAME);
        this.organizationURLBuilder =
            (OrganizationURLBuilder) registry.getBuilderFactory().getBuilder(OrganizationURL.DEFAULT_ELEMENT_NAME);
        this.contactPersonBuilder =
            (ContactPersonBuilder) registry.getBuilderFactory().getBuilder(ContactPerson.DEFAULT_ELEMENT_NAME);
        this.emailAddressBuilder =
            (EmailAddressBuilder) registry.getBuilderFactory().getBuilder(EmailAddress.DEFAULT_ELEMENT_NAME);
        this.telephoneNumberBuilder =
            (TelephoneNumberBuilder) registry.getBuilderFactory().getBuilder(TelephoneNumber.DEFAULT_ELEMENT_NAME);
        this.extensionsBuilder = new ExtensionsBuilder();
        this.xsAnyBuilder = new XSAnyBuilder();

        this.edMarshaller =
            (EntityDescriptorMarshaller) registry
                .getMarshallerFactory()
                .getMarshaller(EntityDescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Override
    public String resolve(RelyingPartyRegistration relyingPartyRegistration) {
        String registrationId = relyingPartyRegistration.getRegistrationId();
        SpidIdentityProviderConfig providerConfig = configRepository.findByProviderId(registrationId);
        if (providerConfig == null) {
            Saml2Error err = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found for " + registrationId
            );
            throw new Saml2AuthenticationException(err);
        }
        // The root element must be a _single_ <EntityDescriptor> as per SPID specs
        EntityDescriptor ed = build(relyingPartyRegistration, providerConfig);

        // Generate and add <Signature> to <EntityDescriptor>
        SignatureSigningParameters signingParams = getSigningParams(providerConfig);
        signingParams.setKeyInfoGenerator(x509KeyInfoGenerator());
        try {
            SignatureSupport.signObject(ed, signingParams);
        } catch (SecurityException | MarshallingException | SignatureException e) {
            throw new Saml2Exception(e);
        }

        // marshall to xml string
        try {
            Element element = this.edMarshaller.marshall(ed);
            // NOTE: pretty-print will invalidate signature (as the document is not the same byte-by-byte)
            return SerializeSupport.nodeToString(element);
        } catch (MarshallingException e) {
            throw new Saml2Exception(e);
        }
    }

    // build an the Metadata root element, which is a single Entity Descriptor, except for signature
    private EntityDescriptor build(RelyingPartyRegistration registration, SpidIdentityProviderConfig config) {
        // define <EntityDescriptor> base attributes
        EntityDescriptor ed = entityDescriptorBuilder.buildObject();
        ed.setEntityID(registration.getEntityId()); // REQUIRED // TODO: secondo me si può sostituire con config.getEntityId() ...
        ed.setID(generateId()); // NOTE: flagged as optional in [2], but always present in [1]
        ed.getNamespaceManager().registerNamespaceDeclaration(new Namespace(SPID_NAMESPACE_URI, SPID_NAMESPACE_PREFIX));

        SPSSODescriptor spssoDescriptor = buildSPSSO(registration, config);
        ed.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME).add(spssoDescriptor);

        Organization organization = buildOrganization(config);
        ed.setOrganization(organization);

        ContactPerson contactPerson = buildContactPerson(config);
        ed.getContactPersons().add(contactPerson);

        return ed;
    }

    // build the <SPSSODescriptor>
    private SPSSODescriptor buildSPSSO(RelyingPartyRegistration registration, SpidIdentityProviderConfig config) {
        SPSSODescriptor spSsoDescriptor = spSsoDescriptorBuilder.buildObject();
        spSsoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        spSsoDescriptor.setAuthnRequestsSigned(true);
        spSsoDescriptor.setWantAssertionsSigned(true);

        // Add signing key (only one supported) to <SPSSODescriptor>
        Saml2X509Credential credential = registration.getSigningX509Credentials().stream().findFirst().orElse(null);
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

        // Add assertion consumer service to <SPSSODescriptor>
        AssertionConsumerService assertionConsumerService = assertionConsumerServiceBuilder.buildObject();
        assertionConsumerService.setLocation(registration.getAssertionConsumerServiceLocation());
        assertionConsumerService.setBinding(registration.getAssertionConsumerServiceBinding().getUrn());
        assertionConsumerService.setIndex(0);
        assertionConsumerService.setIsDefault(true);
        spSsoDescriptor.getAssertionConsumerServices().add(assertionConsumerService);

        // Add single attribute consuming service to <SPSSODescriptor>
        AttributeConsumingService attributeConsumingService = attributeConsumingServiceBuilder.buildObject();
        // TODO: support multiple <AttributeConsumingService>, one per requested attribute set
        //  - idea: default (index 0) is ask everything, additional sets might be asked from config map
        attributeConsumingService.setIndex(0);
        attributeConsumingService.setIsDefault(true);

        //        // TODO: below we support default set that asks for everything SPID related: support for more index sets to be added
        //        ServiceName defaultServiceName = serviceNameBuilder.buildObject();
        //        defaultServiceName.setValue(DEFAULT_SERVICE_NAME);
        //        defaultServiceName.setXMLLang("it");
        //        attributeConsumingService.getNames().add(defaultServiceName);
        //        for (SpidAttribute attr : SpidAttribute.values()) {
        //            RequestedAttribute reqAttribute = requestedAttributeBuilder.buildObject();
        //            reqAttribute.setName(attr.getValue());
        //            attributeConsumingService.getRequestedAttributes().add(reqAttribute);
        //        }

        // TODO: below we support default set that asks for what is configured in the provider config map: support for more index sets to be added
        ServiceName defaultServiceName = serviceNameBuilder.buildObject();
        defaultServiceName.setValue(DEFAULT_SERVICE_NAME);
        defaultServiceName.setXMLLang("it");
        attributeConsumingService.getNames().add(defaultServiceName);
        config
            .getSpidAttributes()
            .forEach(attr -> {
                RequestedAttribute attribute = requestedAttributeBuilder.buildObject();
                attribute.setName(attr.getValue());
                attributeConsumingService.getRequestedAttributes().add(attribute);
            });

        spSsoDescriptor.getAttributeConsumingServices().add(attributeConsumingService);

        // Add single logout service to <SPSSODescriptor>
        // extract baseUrl
        Map<String, String> acsVariables = new HashMap<>();
        acsVariables.put("baseUrl", "");
        acsVariables.put("registrationId", registration.getRegistrationId());
        UriComponents acsComponents = UriComponentsBuilder
            .fromUriString(config.getConsumerUrl())
            .replaceQuery(null)
            .fragment(null)
            .buildAndExpand(acsVariables);
        UriComponents uriComponents = UriComponentsBuilder
            .fromHttpUrl(registration.getAssertionConsumerServiceLocation())
            .replacePath(null)
            .replaceQuery(null)
            .fragment(null)
            .build();
        String baseUrl = uriComponents.toUriString();
        String sloLocation = resolveUrlTemplate(config.getLogoutUrl(), baseUrl, registration);
        SingleLogoutService sloService = singleLogoutServiceBuilder.buildObject();
        sloService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        sloService.setLocation(sloLocation);
        spSsoDescriptor.getSingleLogoutServices().add(sloService);

        return spSsoDescriptor;
    }

    private Organization buildOrganization(SpidIdentityProviderConfig config) {
        SpidIdentityProviderConfigMap configMap = config.getConfigMap();
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
        organizationUrl.setURI(configMap.getOrganizationUrl());
        organization.getURLs().add(organizationUrl);
        return organization;
    }

    private ContactPerson buildContactPerson(SpidIdentityProviderConfig config) {
        SpidIdentityProviderConfigMap configMap = config.getConfigMap();
        boolean isPublic = true;
        // TODO: consider removal as private is currently not supported
        //        if (configMap.getContactPerson_Public() != null) {
        //            isPublic = configMap.getContactPerson_Public();
        //        } else {
        //            isPublic = true;
        //        }

        ContactPerson contactPerson = contactPersonBuilder.buildObject();
        contactPerson.setType(ContactPersonTypeEnumeration.OTHER);

        EmailAddress emailAddress = emailAddressBuilder.buildObject();
        emailAddress.setURI(configMap.getContactPerson_EmailAddress());
        contactPerson.getEmailAddresses().add(emailAddress);

        //        OPTIONAL
        //        if (StringUtils.hasText(configMap.getContactPerson_TelephoneNumber())) {
        //            TelephoneNumber telephoneNumber = telephoneNumberBuilder.buildObject();
        //            telephoneNumber.setNumber(configMap.getContactPerson_TelephoneNumber());
        //            contactPerson.getTelephoneNumbers().add(telephoneNumber);
        //        }

        Extensions contactExtension = extensionsBuilder.buildObject();
        List<XMLObject> contactExtensions = new ArrayList<>();
        //        OPTIONAL FOR PUBLIC
        //        if (StringUtils.hasText(configMap.getContactPerson_VATNumber())) {
        //            XSAny vatNumber = xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "VATNumber", SPID_NAMESPACE_PREFIX);
        //            vatNumber.setTextContent(configMap.getContactPerson_VATNumber());
        //            contactExtensions.add(vatNumber);
        //        }

        if (StringUtils.hasText(configMap.getContactPerson_IPACode()) && isPublic) {
            XSAny ipaCode = xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "IPACode", SPID_NAMESPACE_PREFIX);
            ipaCode.setTextContent(configMap.getContactPerson_IPACode());
            contactExtensions.add(ipaCode);
        }
        //        OPTIONAL FOR PUBLIC
        //        if (StringUtils.hasText(configMap.getContactPerson_FiscalCode()) && !isPublic) {
        //            XSAny fiscalCode = xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "FiscalCode", SPID_NAMESPACE_PREFIX);
        //            fiscalCode.setTextContent(configMap.getContactPerson_FiscalCode());
        //            contactExtensions.add(fiscalCode);
        //        }

        if (isPublic) {
            contactExtensions.add(xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "Public", SPID_NAMESPACE_PREFIX));
        } else {
            contactExtensions.add(xsAnyBuilder.buildObject(SPID_NAMESPACE_URI, "Private", SPID_NAMESPACE_PREFIX));
        }
        contactExtension.getUnknownXMLObjects().addAll(contactExtensions);
        contactPerson.setExtensions(contactExtension);
        return contactPerson;
    }

    private String generateId() {
        return "_".concat(RandomStringUtils.randomAlphanumeric(39)).toLowerCase();
    }

    private SignatureSigningParameters getSigningParams(SpidIdentityProviderConfig providerConfig) {
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

    private KeyInfoGenerator x509KeyInfoGenerator() {
        X509KeyInfoGeneratorFactory generator = new X509KeyInfoGeneratorFactory();
        generator.setEmitEntityCertificate(true);
        generator.setEmitEntityCertificateChain(true);
        return generator.newInstance();
    }

    private static String resolveUrlTemplate(String template, String baseUrl, RelyingPartyRegistration relyingParty) {
        String entityId = relyingParty.getAssertingPartyDetails().getEntityId();
        String registrationId = relyingParty.getRegistrationId();
        Map<String, String> uriVariables = new HashMap<>();
        UriComponents uriComponents = UriComponentsBuilder
            .fromHttpUrl(baseUrl)
            .replaceQuery(null)
            .fragment(null)
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
}
