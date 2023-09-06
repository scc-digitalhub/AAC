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

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.Client;
import java.io.Serializable;
import org.springframework.util.Assert;

public abstract class AbstractClient implements Client, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String realm;

    protected final String clientId;

    public AbstractClient(String realm, String clientId) {
        Assert.notNull(realm, "realm is mandatory");
        Assert.hasText(clientId, "clientId can not be null or empty");
        this.clientId = clientId;
        this.realm = realm;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractClient() {
        this(null, null);
    }

    @Override
    @JsonIgnore
    public String getRealm() {
        return realm;
    }

    @JsonIgnore
    public String getClientId() {
        return clientId;
    }
}
