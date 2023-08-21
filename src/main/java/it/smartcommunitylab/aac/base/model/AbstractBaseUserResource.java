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

package it.smartcommunitylab.aac.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.smartcommunitylab.aac.core.model.UserResource;

public abstract class AbstractBaseUserResource extends AbstractBaseResource implements UserResource {

    @JsonInclude
    protected String userId;

    protected AbstractBaseUserResource(String authority, String provider, String realm, String id, String userId) {
        super(authority, provider, realm, id);
        this.userId = userId;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractBaseUserResource() {
        this((String) null, (String) null, (String) null, (String) null, (String) null);
    }

    //userId is persisted, let actual classes handle
    // public abstract void setUserId(String userId);

    @Override
    public String getUserId() {
        return userId;
    }
}
