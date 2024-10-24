/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.scope;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.ScopeType;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
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

    public Scope() {}

    public Scope(String scope) {
        Assert.hasText(scope, "scope can not be empty");
        this.scope = scope;
    }

    public String getId() {
        return getScope();
    }

    // public void setId(String id) {
    //     this.scope = id;
    // }

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
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Scope other = (Scope) obj;
        if (getScope() == null) {
            if (other.getScope() != null) return false;
        } else if (!getScope().equals(other.getScope())) return false;
        return true;
    }
}
