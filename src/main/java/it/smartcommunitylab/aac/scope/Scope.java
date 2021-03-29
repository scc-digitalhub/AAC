package it.smartcommunitylab.aac.scope;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
public class Scope {

    public static String TYPE_CLIENT = "client";
    public static String TYPE_USER = "user";

    @Pattern(regexp = SystemKeys.SCOPE_PATTERN)
    private String scope;

    private String name;
    private String description;
    private String resourceId;

    private String type;

    public Scope() {

    }

    public Scope(String scope) {
        Assert.hasText(scope, "scope can not be empty");
        this.scope = scope;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
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
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        return true;
    }

}
