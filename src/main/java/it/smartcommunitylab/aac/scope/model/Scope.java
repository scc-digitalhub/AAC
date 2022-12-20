package it.smartcommunitylab.aac.scope.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
public class Scope {

    // the scope identifier
    @Pattern(regexp = SystemKeys.SCOPE_PATTERN)
    @NotBlank
    protected String scope;

    // a scope is associated to a resource
    protected String resourceId;

    // a scope is registered in a realm
    protected String realm;

    // TODO i18n
    protected String name;
    protected String description;

    public Scope() {

    }

    public Scope(String scope) {
        Assert.hasText(scope, "scope can not be empty");
        this.scope = scope;
    }

    public String getId() {
        return scope;
    }

    public void setId(String id) {
        Assert.hasText(scope, "scope can not be empty");
        this.scope = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getScope() == null) ? 0 : getScope().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Scope other = (Scope) obj;
        if (getScope() == null) {
            if (other.getScope() != null)
                return false;
        } else if (!getScope().equals(other.getScope()))
            return false;
        return true;
    }

}
