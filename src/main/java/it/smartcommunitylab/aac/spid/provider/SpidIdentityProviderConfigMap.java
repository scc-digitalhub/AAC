package it.smartcommunitylab.aac.spid.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpidIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_SPID;

    // core
    private String entityId;

    // <Signature> options
    private String signingKey;
    private String signingCertificate; // for <KeyDescriptor use="signing"><KeyInfo>

    // <AssertionConsumerService> options - Currently only one supported at index 0, and binding MUST be post according to SPID specs, and must be tagged with isDefault="true"
//    private final Integer ssoServiceIndex = 0;
//    private final String ssoServiceBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
//    private String ssoServiceLocation;

    // <SingleLogoutService> options
//    private String ssoLogout;
//    private String ssoLogoutBinding;
//    private String ssoLogoutResponseLocation; //optional according to specs

    // <Organization> options
    private String organizationDisplayName; // TODO: forse questa dovrebbe essere una map[string]string in cui la chiave è la lingua: per ora lang="it" only
    private String organizationName;
    private String organizationUrl;

    // <ContactPerson> options
    // TODO: second ContactPerson tag for private SP currently not supported
    private String contactPerson_Telephone; // without spaces, includes internation prefix (such as +39 for Italy)
    private String contactPerson_EmailAddress;
    private String contactPerson_IPACode;
    private Boolean contactPerson_Public; // Public/Private
    private String contactPerson_Type; // "other" (mandatory), optionally includes "billing" (unless private SP, in which case "billing" is also mandatory)

    // AAC options
    // ap autoconfiguration
    // either idps as list of entityIds or single url
    // when empty all idps will be registered
    private Set<String> idps;
    private String idpMetadataUrl;
    private Set<SpidAttribute> spidAttributes;
    private SpidAuthnContext authnContext;
    private SpidUserAttribute subAttributeName;
    private SpidUserAttribute usernameAttributeName;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
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

//    public String getSsoServiceLocation() {
//        return ssoServiceLocation;
//    }
//
//    public void setSsoServiceLocation(String ssoServiceLocation) {
//        this.ssoServiceLocation = ssoServiceLocation;
//    }
//
//    public String getSsoLogout() {
//        return ssoLogout;
//    }
//
//    public void setSsoLogout(String ssoLogout) {
//        this.ssoLogout = ssoLogout;
//    }
//
//    public String getSsoLogoutBinding() {
//        return ssoLogoutBinding;
//    }
//
//    public void setSsoLogoutBinding(String ssoLogoutBinding) {
//        this.ssoLogoutBinding = ssoLogoutBinding;
//    }
//
//    public String getSsoLogoutResponseLocation() {
//        return ssoLogoutResponseLocation;
//    }
//
//    public void setSsoLogoutResponseLocation(String ssoLogoutResponseLocation) {
//        this.ssoLogoutResponseLocation = ssoLogoutResponseLocation;
//    }

    public String getContactPerson_Telephone() {
        return contactPerson_Telephone;
    }

    public void setContactPerson_Telephone(String contactPerson_Telephone) {
        this.contactPerson_Telephone = contactPerson_Telephone;
    }

    public String getContactPerson_EmailAddress() {
        return contactPerson_EmailAddress;
    }

    public void setContactPerson_EmailAddress(String contactPerson_EmailAddress) {
        this.contactPerson_EmailAddress = contactPerson_EmailAddress;
    }

    public String getContactPerson_IPACode() {
        return contactPerson_IPACode;
    }

    public void setContactPerson_IPACode(String contactPerson_IPACode) {
        this.contactPerson_IPACode = contactPerson_IPACode;
    }

    public Boolean getContactPerson_Public() {
        return contactPerson_Public;
    }

    public void setContactPerson_Public(Boolean contactPerson_Public) {
        this.contactPerson_Public = contactPerson_Public;
    }

    public String getContactPerson_Type() {
        return contactPerson_Type;
    }

    public void setContactPerson_Type(String contactPerson_Type) {
        this.contactPerson_Type = contactPerson_Type;
    }

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public void setOrganizationDisplayName(String organizationDisplayName) {
        this.organizationDisplayName = organizationDisplayName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationUrl() {
        return organizationUrl;
    }

    public void setOrganizationUrl(String organizationUrl) {
        this.organizationUrl = organizationUrl;
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


    public SpidUserAttribute getSubAttributeName() {
        return this.subAttributeName;
    }

    public void setSubAttributeName(SpidUserAttribute subAttributeName) {
        this.subAttributeName = subAttributeName;
    }

    public SpidUserAttribute getUsernameAttributeName() {
        return usernameAttributeName;
    }

    public void setUsernameAttributeName(SpidUserAttribute usernameAttributeName) {
        this.usernameAttributeName = usernameAttributeName;
    }

    @JsonIgnore
    public void setConfiguration(SpidIdentityProviderConfigMap map) {
        this.entityId = map.getEntityId();
        this.signingKey = map.getSigningKey();
        this.signingCertificate = map.getSigningKey();
//        this.ssoServiceLocation = map.getSsoServiceLocation();
//        this.ssoLogout = map.getSsoLogout();
//        this.ssoLogoutBinding = map.getSsoLogoutBinding();
//        this.ssoLogoutResponseLocation = map.getSsoLogoutResponseLocation();
        this.contactPerson_Telephone = map.getContactPerson_Telephone();
        this.contactPerson_EmailAddress = map.getContactPerson_EmailAddress();
        this.contactPerson_IPACode = map.getContactPerson_IPACode();
        this.contactPerson_Public = map.getContactPerson_Public();
        this.contactPerson_Type = map.getContactPerson_Type();
        this.organizationDisplayName = map.getOrganizationDisplayName();
        this.organizationName = map.getOrganizationName();
        this.organizationUrl = map.getOrganizationUrl();
        this.idps = map.getIdps();
        this.idpMetadataUrl = map.getIdpMetadataUrl();
        this.spidAttributes = map.getSpidAttributes();
        this.authnContext = map.getAuthnContext();
        this.subAttributeName = map.getSubAttributeName();
        this.usernameAttributeName = map.getUsernameAttributeName();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        SpidIdentityProviderConfigMap map = mapper.convertValue(props, SpidIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(SpidIdentityProviderConfigMap.class);
    }
}
