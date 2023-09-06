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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;

/*
 * Abstract class for user credentials
 *
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = WebAuthnUserCredential.class, name = WebAuthnUserCredential.RESOURCE_TYPE),
        @Type(value = InternalUserPassword.class, name = InternalUserPassword.RESOURCE_TYPE),
    }
)
public abstract class AbstractUserCredentials extends AbstractBaseUserResource implements UserCredentials {

    protected AbstractUserCredentials(String authority, String provider) {
        super(authority, provider);
    }

    @Override
    public String getId() {
        // use uuid from persisted model
        return getUuid();
    }

    @Override
    public String getResourceId() {
        return getCredentialsId();
    }

    // uuid is mandatory
    public abstract String getUuid();

    // repositoryId is always available
    public abstract String getRepositoryId();

    // credentials status is manageable
    public abstract String getStatus();

    public abstract void setStatus(String status);
}
