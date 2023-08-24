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

package it.smartcommunitylab.aac.oauth.persistence;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ClientResource;
import java.io.Serializable;

public abstract class AbstractOAuth2ClientResource implements ClientResource, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private final String realm;
    protected String clientId;

    protected AbstractOAuth2ClientResource(String realm) {
        this.realm = realm;
    }

    protected AbstractOAuth2ClientResource(String realm, String clientId) {
        this.realm = realm;
        this.clientId = clientId;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractOAuth2ClientResource() {
        this((String) null, (String) null);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_OAUTH2;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    // // resource is globally unique and addressable
    // // ie given to an external actor he should be able to find the authority and
    // // then the provider to request this resource
    // @Override
    // public String getResourceId() {
    //     StringBuilder sb = new StringBuilder();
    //     sb.append(getAuthority()).append(SystemKeys.ID_SEPARATOR);
    //     sb.append(getId());

    //     return sb.toString();
    // }

    // @Override
    // public String getUrn() {
    //     StringBuilder sb = new StringBuilder();
    //     sb.append(SystemKeys.URN_PROTOCOL).append(SystemKeys.URN_SEPARATOR);
    //     sb.append(getType()).append(SystemKeys.URN_SEPARATOR);
    //     sb.append(getResourceId());

    //     return sb.toString();
    // }
}
