/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.smartcommunitylab.aac.model.Resource;
import javax.persistence.Transient;

public abstract class AbstractBaseResource implements Resource {

    @JsonInclude
    @Transient
    protected String authority;

    @JsonInclude
    @Transient
    protected String provider;

    @JsonInclude
    protected String realm;

    @JsonInclude
    protected final String id;

    protected AbstractBaseResource(String authority, String provider, String realm, String id) {
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
        this.id = id;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractBaseResource() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    // by default resources are stored in repositories
    // authorityId and provider are transient: implementations should avoid
    // persisting these attributes
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    // //realm is persisted, let actual classes handle
    // public abstract void setRealm(String realm);

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getId() {
        return id;
    }
}
