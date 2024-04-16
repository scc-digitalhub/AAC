package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpidIdpMetadataModel {

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("logo_uri")
    private String logoUri;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_hash")
    private String fileHash;

    @JsonProperty("code")
    private String code;

    @JsonProperty("signing_certificate_x509")
    private List<String> signingCertificateX509;

    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("organization_display_name")
    private String organizationDisplayName;

    @JsonProperty("single_logout_service")
    private List<SpidMetadataSloLocationModel> singleLogoutService;

    @JsonProperty("single_sign_on_service")
    private List<SpidMetadataSsoLocationModel> singleSignOnService;

    @JsonProperty("attribute")
    private List<String> attribute;

    @JsonProperty("extensions")
    private SpidMetadataExtensionsModel extensions;

    @JsonProperty("create_date")
    private String createDate;

    @JsonProperty("lastupdate_date")
    private String lastupdateDate;

    @JsonProperty("delete_date")
    private String deleteDate;

    @JsonProperty("registry_link")
    private String registryLink;

    public SpidIdpMetadataModel() {}

    public SpidIdpMetadataModel(
        String entityId,
        String logoUri,
        String fileName,
        String fileHash,
        String code,
        List<String> signingCertificateX509,
        String organizationName,
        String organizationDisplayName,
        List<SpidMetadataSloLocationModel> singleLogoutService,
        List<SpidMetadataSsoLocationModel> singleSignOnService,
        List<String> attribute,
        SpidMetadataExtensionsModel extensions,
        String createDate,
        String lastupdateDate,
        String deleteDate,
        String registryLink
    ) {
        this.entityId = entityId;
        this.logoUri = logoUri;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.code = code;
        this.signingCertificateX509 = signingCertificateX509;
        this.organizationName = organizationName;
        this.organizationDisplayName = organizationDisplayName;
        this.singleLogoutService = singleLogoutService;
        this.singleSignOnService = singleSignOnService;
        this.attribute = attribute;
        this.extensions = extensions;
        this.createDate = createDate;
        this.lastupdateDate = lastupdateDate;
        this.deleteDate = deleteDate;
        this.registryLink = registryLink;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getCode() {
        return code;
    }

    public List<String> getSigningCertificateX509() {
        return signingCertificateX509;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public List<SpidMetadataSloLocationModel> getSingleLogoutService() {
        return singleLogoutService;
    }

    public List<SpidMetadataSsoLocationModel> getSingleSignOnService() {
        return singleSignOnService;
    }

    public List<String> getAttribute() {
        return attribute;
    }

    public SpidMetadataExtensionsModel getExtensions() {
        return extensions;
    }

    public String getCreateDate() {
        return createDate;
    }

    public String getLastupdateDate() {
        return lastupdateDate;
    }

    public String getDeleteDate() {
        return deleteDate;
    }

    public String getRegistryLink() {
        return registryLink;
    }

    @Override
    public String toString() {
        return (
            "SpidIdpModel{" +
            "entityId='" +
            entityId +
            '\'' +
            ", logoUri='" +
            logoUri +
            '\'' +
            ", fileName='" +
            fileName +
            '\'' +
            ", fileHash='" +
            fileHash +
            '\'' +
            ", code='" +
            code +
            '\'' +
            ", signingCertificateX509=" +
            signingCertificateX509 +
            ", organizationName='" +
            organizationName +
            '\'' +
            ", organizationDisplayName='" +
            organizationDisplayName +
            '\'' +
            ", singleLogoutService=" +
            singleLogoutService +
            ", singleSignOnService=" +
            singleSignOnService +
            ", attribute=" +
            attribute +
            ", extensions=" +
            extensions +
            ", createDate=" +
            createDate +
            ", lastupdateDate=" +
            lastupdateDate +
            ", deleteDate=" +
            deleteDate +
            ", registryLink='" +
            registryLink +
            '\'' +
            '}'
        );
    }
}
