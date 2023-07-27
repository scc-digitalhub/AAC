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

package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.smartcommunitylab.aac.core.model.UserResource;
import java.io.Serializable;
import javax.persistence.Transient;

public abstract class AbstractBaseUserResource implements UserResource, Serializable {

    @JsonInclude
    @Transient
    private String authority;

    @JsonInclude
    @Transient
    private String provider;

    protected AbstractBaseUserResource(String authority, String provider) {
        this.authority = authority;
        this.provider = provider;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractBaseUserResource() {
        this((String) null, (String) null);
    }

    public abstract void setUserId(String userId);

    public abstract void setRealm(String realm);

    // by default resources are associated to repositories, not providers
    // authorityId and provider are transient: implementations should avoid
    // persisting these attributes
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getProvider() {
        return provider;
    }
}
