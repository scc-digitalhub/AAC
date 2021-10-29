package it.smartcommunitylab.aac.model;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealmRole {

    private String roleId;

//    @NotBlank
    private String realm;

    @NotBlank
    private String role;

    private String name;
    private String description;

    // permissions are scopes
    private Set<String> permissions;

    public RealmRole() {
    }

    public RealmRole(String realm, String role) {
        this.realm = realm;
        this.role = role;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getAuthority() {
        return realm + ":" + role;
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
        return StringUtils.hasText(name) ? name : role;
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

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

}
