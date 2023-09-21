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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.springframework.util.Assert;

@Valid
public class Resource {

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    @NotBlank
    protected String resourceId;

    protected String name;
    protected String description;

    protected String realm;

    private Set<Scope> scopes;

    public Resource() {}

    public Resource(String resourceId) {
        Assert.hasText(resourceId, "resource id can not be empty");
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Collection<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<Scope> scopes) {
        this.scopes = new HashSet<>();
        this.scopes.addAll(scopes);
    }
    
    public String getId() {
    	return this.resourceId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resourceId == null) ? 0 : resourceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Resource other = (Resource) obj;
        if (resourceId == null) {
            if (other.resourceId != null) return false;
        } else if (!resourceId.equals(other.resourceId)) return false;
        return true;
    }
}
