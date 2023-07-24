package it.smartcommunitylab.aac.core.persistence;

import it.smartcommunitylab.aac.repository.HashMapConverter;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "realms")
@EntityListeners(AuditingEntityListener.class)
public class RealmEntity {

    @Id
    @NotNull
    @Column(length = 128, unique = true)
    private String slug;

    @NotNull
    private String name;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    // configuration
    @Column(name = "is_editable")
    private boolean editable;

    @Column(name = "is_public")
    private boolean isPublic;

    // TODO move to oauth2ConfigProvider
    @Lob
    @Column(name = "oauth_configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> oauthConfigurationMap;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Map<String, Serializable> getOAuthConfigurationMap() {
        return oauthConfigurationMap;
    }

    public void setOAuthConfigurationMap(Map<String, Serializable> oauthConfigurationMap) {
        this.oauthConfigurationMap = oauthConfigurationMap;
    }
}
