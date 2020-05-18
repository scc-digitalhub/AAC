package it.smartcommunitylab.aac.saml.spid;

import java.util.Collection;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml2.metadata.LocalizedString;
import org.opensaml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.ServiceDescription;
import org.opensaml.saml2.metadata.ServiceName;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.util.Assert;

public class SPIDMetadataGenerator extends MetadataGenerator {

    // define all SPID attributes
    public final static String[] SPID_ATTRIBUTES = {
            "name",
            "fiscalNumber",
            "familyName",
            "spidCode",
            "gender",
            "dateOfBirth",
            "countyOfBirth",
            "idCard",
            "registeredOffice",
            "email",
            "digitalAddress",
            "ivaCode",
            "placeOfBirth",
            "companyName",
            "mobilePhone",
            "address",
            "expirationDate"
    };

    public final static String[] BASIC_ATTRIBUTES = {
            "name",
            "fiscalNumber",
            "familyName",
            "spidCode"
    };

    private final static String SPID_LANG = "it";
    private final static String ATTRNAME_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";

    private String spName;
    private String spDescription;

    public SPIDMetadataGenerator(String name, String description) {
        super();
        Assert.hasText(name, "A non-empty service name is required");
        Assert.hasText(description, "A non-empty service description is required");

        this.spName = name;
        this.spDescription = description;
    }

    @Override
    protected SPSSODescriptor buildSPSSODescriptor(String entityBaseURL, String entityAlias, boolean requestSigned,
            boolean wantAssertionSigned, Collection<String> includedNameID) {
        SPSSODescriptor descriptor = super.buildSPSSODescriptor(entityBaseURL, entityAlias, requestSigned,
                wantAssertionSigned, includedNameID);
        // we need to declare consumer services for attributes
        // TODO declare multiple service for different attributes sets
        descriptor.getAttributeConsumingServices().add(generateConsumingService());
        return descriptor;
    }

    @SuppressWarnings("unchecked")
    private AttributeConsumingService generateConsumingService() {
        SAMLObjectBuilder<AttributeConsumingService> serviceBuilder = (SAMLObjectBuilder<AttributeConsumingService>) builderFactory
                .getBuilder(AttributeConsumingService.DEFAULT_ELEMENT_NAME);
        AttributeConsumingService service = serviceBuilder.buildObject();

        SAMLObjectBuilder<ServiceName> nameBuilder = (SAMLObjectBuilder<ServiceName>) builderFactory
                .getBuilder(ServiceName.DEFAULT_ELEMENT_NAME);
        ServiceName serviceName = nameBuilder.buildObject();
        serviceName.setName(new LocalizedString(spName, SPID_LANG));
        service.getNames().add(serviceName);

        SAMLObjectBuilder<ServiceDescription> descBuilder = (SAMLObjectBuilder<ServiceDescription>) builderFactory
                .getBuilder(ServiceDescription.DEFAULT_ELEMENT_NAME);
        ServiceDescription serviceDescription = descBuilder.buildObject();
        serviceDescription.setDescription(new LocalizedString(spDescription, SPID_LANG));
        service.getDescriptions().add(serviceDescription);

        SAMLObjectBuilder<RequestedAttribute> attrBuilder = (SAMLObjectBuilder<RequestedAttribute>) builderFactory
                .getBuilder(RequestedAttribute.DEFAULT_ELEMENT_NAME);

        // require only basic attributes
        for (String name : BASIC_ATTRIBUTES) {
            RequestedAttribute reqAttr = attrBuilder.buildObject();
            reqAttr.setName(name);
            // every spid attribute has the same format as per spec
            reqAttr.setNameFormat(ATTRNAME_FORMAT);
            service.getRequestAttributes().add(reqAttr);
        }

        service.setIndex(1);

        return service;
    }
}