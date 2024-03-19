package it.smartcommunitylab.aac.spid.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.model.SubjectStatus;
import org.springframework.util.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SpidUserAccount extends AbstractUserAccount {
    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
            SystemKeys.RESOURCE_ACCOUNT + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SPID;

    @NotBlank
    private String repositoryId;
    // subject identifier from external provider
    @NotBlank
    private String subjectId;
    // unique uuid (subject entity)
    @NotBlank
    private String uuid;
    // login
    private String status;
    // attributes
    private String username;
    private String idp;
    private String spidCode;
    private String email;
    private String name;
    private String surname;
    private String phone;
    private String fiscalNumber;
    private String ivaCode;
    private Map<String, Serializable> attributes;

    // audit
    private Date createDate;
    private Date modifiedDate;

    public SpidUserAccount(String provider, String realm, String uuid) {
        super(SystemKeys.AUTHORITY_SPID, provider, realm, uuid);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    protected SpidUserAccount() {
        super();
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }
    @Override
    public String getUuid() {
        return uuid;
    }
    @Override
    public String getUsername() {
        return username;
    }
    @Override
    public String getAccountId() {
        // local id is subject id
        return subjectId;
    }
    @Override
    public String getEmailAddress() {
        return email;
    }
    @Override
    public boolean isEmailVerified() {
        return StringUtils.hasText(email);
    }

    @Override
    public boolean isLocked() {
        // only active users are *not* locked
        if (status == null || SubjectStatus.ACTIVE.getValue().equals(status)) {
            return false;
        }

        // every other condition locks login
        return true;
    }

    /*
     * fields
     */
    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }


    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdp() {
        return idp;
    }

    public void setIdp(String idp) {
        this.idp = idp;
    }

    public String getSpidCode() {
        return spidCode;
    }

    public void setSpidCode(String spidCode) {
        this.spidCode = spidCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFiscalNumber() {
        return fiscalNumber;
    }

    public void setFiscalNumber(String fiscalNumber) {
        this.fiscalNumber = fiscalNumber;
    }

    public String getIvaCode() {
        return ivaCode;
    }

    public void setIvaCode(String ivaCode) {
        this.ivaCode = ivaCode;
    }
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "SpidUserAccount{" +
                "repositoryId='" + repositoryId + '\'' +
                ", subjectId='" + subjectId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", status='" + status + '\'' +
                ", username='" + username + '\'' +
                ", idp='" + idp + '\'' +
                ", spidCode='" + spidCode + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", phone='" + phone + '\'' +
                ", fiscalNumber='" + fiscalNumber + '\'' +
                ", ivaCode='" + ivaCode + '\'' +
                ", attributes=" + attributes +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
