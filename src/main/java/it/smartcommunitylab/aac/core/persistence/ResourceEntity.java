package it.smartcommunitylab.aac.core.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "resources", uniqueConstraints = @UniqueConstraint(columnNames = { "type", "authority", "provider_id",
        "resource_id" }))
public class ResourceEntity {

    @Id
    @NotNull
    @Column(length = 128, unique = true)
    private String id;

    @NotNull
    @Column(length = 128)
    private String type;

    @NotNull
    @Column(length = 128)
    private String authority;

    @NotNull
    @Column(name = "provider_id", length = 128)
    private String provider;

    @NotNull
    @Column(name = "resource_id", length = 128)
    private String resourceId;

    public ResourceEntity() {
    }

    public ResourceEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
