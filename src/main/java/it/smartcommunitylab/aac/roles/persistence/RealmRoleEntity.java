package it.smartcommunitylab.aac.roles.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "realm_roles", uniqueConstraints = @UniqueConstraint(columnNames = { "realm", "role" }))
public class RealmRoleEntity {

    public static final String ID_PREFIX = "rr_";

    @Id
    @NotNull
    @Column(name = "role_id", length = 128, unique = true)
    private String id;

    @NotNull
    @Column(length = 128)
    private String realm;

    @NotNull
    @Column(length = 128)
    private String role;

    private String name;
    private String description;

    public RealmRoleEntity() {}

    public RealmRoleEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
