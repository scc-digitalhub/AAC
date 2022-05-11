package it.smartcommunitylab.aac.spid.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpidIdentityProviderConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private String entityId;

    private String signingKey;
    private String signingCertificate;
    // encryption is not supported by SPID
//    private String cryptKey;
//    private String cryptCertificate;

    // ap autoconfiguration
    // either idps as list of entityIds or single url
    // when empty all idps will be registered
    private Set<String> idps;
    private String idpMetadataUrl;

    // advanced
    private String ssoServiceBinding;
    private SpidAttribute userNameAttributeName = SpidAttribute.SPID_CODE;
    private Boolean useSpidCodeAsNameId;
    private Set<SpidAttribute> spidAttributes;
    private SpidUserAttribute idAttribute;

    // note: comparison is hardcoded as minimum
    private SpidAuthnContext authnContext;

    // organization
    private String organizationName;
    private String organizationDisplayName;
    private String organizationUrl;

    // contact person mandatory (type=other)
    private String contactPersonType;
    private String contactPersonIPACode;
    private String contactPersonVATNumber;
    private String contactPersonFiscalCode;
    private String contactPersonEmailAddress;
    private String contactPersonTelephoneNumber;
    private Boolean contactPersonPublic;

    // not editable
    private String metadataUrl;
    private String assertionConsumerServiceUrl;
    private String singleLogoutUrl;

    public SpidIdentityProviderConfigMap() {
//        this.ssoServiceBinding = "HTTP-POST";
//        this.contactPersonType = "other";
//        this.authnContext = SpidAuthnContext.SPID_L1;
//        this.useSpidCodeAsNameId = true;
//        this.idAttribute = SpidUserAttribute.SPID_CODE;
//
//        this.spidAttributes = new HashSet<>();
//        this.spidAttributes.add(SpidAttribute.SPID_CODE);
    }

    public String getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(String signingKey) {
        this.signingKey = signingKey;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

//    public String getCryptKey() {
//        return cryptKey;
//    }
//
//    public void setCryptKey(String cryptKey) {
//        this.cryptKey = cryptKey;
//    }
//
//    public String getCryptCertificate() {
//        return cryptCertificate;
//    }
//
//    public void setCryptCertificate(String cryptCertificate) {
//        this.cryptCertificate = cryptCertificate;
//    }

    public Boolean getSignAuthNRequest() {
        // always sign requests
        return true;
    }

    public String getSsoServiceBinding() {
        return ssoServiceBinding;
    }

    public void setSsoServiceBinding(String ssoServiceBinding) {
        this.ssoServiceBinding = ssoServiceBinding;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
    }

    public String getSingleLogoutUrl() {
        return singleLogoutUrl;
    }

    public void setSingleLogoutUrl(String singleLogoutUrl) {
        this.singleLogoutUrl = singleLogoutUrl;
    }

    public Set<String> getIdps() {
        return idps;
    }

    public void setIdps(Set<String> idps) {
        this.idps = idps;
    }

    public String getIdpMetadataUrl() {
        return idpMetadataUrl;
    }

    public void setIdpMetadataUrl(String idpMetadataUrl) {
        this.idpMetadataUrl = idpMetadataUrl;
    }

    public SpidAttribute getUserNameAttributeName() {
        return userNameAttributeName;
    }

    public void setUserNameAttributeName(SpidAttribute userNameAttributeName) {
        this.userNameAttributeName = userNameAttributeName;
    }

    public Boolean getUseSpidCodeAsNameId() {
        return useSpidCodeAsNameId;
    }

    public void setUseSpidCodeAsNameId(Boolean useSpidCodeAsNameId) {
        this.useSpidCodeAsNameId = useSpidCodeAsNameId;
    }

    public SpidUserAttribute getIdAttribute() {
        return idAttribute;
    }

    public void setIdAttribute(SpidUserAttribute idAttribute) {
        this.idAttribute = idAttribute;
    }

    public Set<SpidAttribute> getSpidAttributes() {
        return spidAttributes;
    }

    public void setSpidAttributes(Set<SpidAttribute> spidAttributes) {
        this.spidAttributes = spidAttributes;
    }

    public SpidAuthnContext getAuthnContext() {
        return authnContext;
    }

    public void setAuthnContext(SpidAuthnContext authnContext) {
        this.authnContext = authnContext;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public void setOrganizationDisplayName(String organizationDisplayName) {
        this.organizationDisplayName = organizationDisplayName;
    }

    public String getOrganizationUrl() {
        return organizationUrl;
    }

    public void setOrganizationUrl(String organizationUrl) {
        this.organizationUrl = organizationUrl;
    }

    public String getContactPersonType() {
        return contactPersonType;
    }

    public void setContactPersonType(String contactPersonType) {
        this.contactPersonType = contactPersonType;
    }

    public String getContactPersonIPACode() {
        return contactPersonIPACode;
    }

    public void setContactPersonIPACode(String contactPersonIPACode) {
        this.contactPersonIPACode = contactPersonIPACode;
    }

    public String getContactPersonVATNumber() {
        return contactPersonVATNumber;
    }

    public void setContactPersonVATNumber(String contactPersonVATNumber) {
        this.contactPersonVATNumber = contactPersonVATNumber;
    }

    public String getContactPersonFiscalCode() {
        return contactPersonFiscalCode;
    }

    public void setContactPersonFiscalCode(String contactPersonFiscalCode) {
        this.contactPersonFiscalCode = contactPersonFiscalCode;
    }

    public String getContactPersonEmailAddress() {
        return contactPersonEmailAddress;
    }

    public void setContactPersonEmailAddress(String contactPersonEmailAddress) {
        this.contactPersonEmailAddress = contactPersonEmailAddress;
    }

    public String getContactPersonTelephoneNumber() {
        return contactPersonTelephoneNumber;
    }

    public void setContactPersonTelephoneNumber(String contactPersonTelephoneNumber) {
        this.contactPersonTelephoneNumber = contactPersonTelephoneNumber;
    }

    public Boolean getContactPersonPublic() {
        return contactPersonPublic;
    }

    public void setContactPersonPublic(Boolean contactPersonPublic) {
        this.contactPersonPublic = contactPersonPublic;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        SpidIdentityProviderConfigMap map = mapper.convertValue(props, SpidIdentityProviderConfigMap.class);

        this.signingKey = map.getSigningKey();
        this.signingCertificate = map.getSigningCertificate();
//        this.cryptKey = map.getCryptKey();
//        this.cryptCertificate = map.getCryptCertificate();

        this.idps = map.getIdps();
        this.idpMetadataUrl = map.getIdpMetadataUrl();
        this.ssoServiceBinding = map.getSsoServiceBinding();
        this.useSpidCodeAsNameId = map.getUseSpidCodeAsNameId();
        this.userNameAttributeName = map.getUserNameAttributeName();
        this.idAttribute = map.getIdAttribute();
        this.spidAttributes = map.getSpidAttributes();
        this.authnContext = map.getAuthnContext();

        this.organizationName = map.getOrganizationName();
        this.organizationDisplayName = map.getOrganizationDisplayName();
        this.organizationUrl = map.getOrganizationUrl();

        this.contactPersonType = map.getContactPersonType();
        this.contactPersonIPACode = map.getContactPersonIPACode();
        this.contactPersonVATNumber = map.getContactPersonVATNumber();
        this.contactPersonFiscalCode = map.getContactPersonFiscalCode();
        this.contactPersonEmailAddress = map.getContactPersonEmailAddress();
        this.contactPersonTelephoneNumber = map.getContactPersonTelephoneNumber();
        this.contactPersonPublic = map.getContactPersonPublic();

        this.entityId = map.getEntityId();

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(SpidIdentityProviderConfigMap.class);
    }
}
