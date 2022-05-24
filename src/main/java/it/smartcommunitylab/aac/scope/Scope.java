package it.smartcommunitylab.aac.scope;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.ScopeType;

@Valid
public class Scope {

    @Pattern(regexp = SystemKeys.SCOPE_PATTERN)
    @NotBlank
    protected String scope;

    protected String name;
    protected String description;
    protected String resourceId;

    protected ScopeType type;

    // additional audience connected to this scope
    protected Set<String> audience;

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

    public ScopeType getType() {
        return type;
    }

    public void setType(ScopeType type) {
        this.type = type;
    }

    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(Set<String> audience) {
        this.audience = audience;
    }

    public boolean isUserScope() {
        return getType() != null && (getType() == ScopeType.USER || getType() == ScopeType.GENERIC);
    }

    public boolean isClientScope() {
        return getType() != null && (getType() == ScopeType.CLIENT || getType() == ScopeType.GENERIC);
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
