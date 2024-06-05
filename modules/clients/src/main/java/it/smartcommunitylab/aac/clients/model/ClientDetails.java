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

package it.smartcommunitylab.aac.clients.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.auth.model.AuthenticatedDetails;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;

/*
 * Client details descriptor
 *
 * This model should be used to describe and manage the AAC client, in relation to the realm
 * which "owns" the registrations. Its usage is relevant for the auth/securityContext.
 *
 * Services and controllers should adopt the Client model.
 */
@Getter
@Setter
@ToString
public class ClientDetails extends AuthenticatedDetails {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // support enabled/disabled
    //TODO
    private final boolean enabled;

    //additional properties as context
    //TODO
    @ToString.Exclude
    private Map<String, Serializable> additionalProperties = new HashMap<>();

    public ClientDetails(
        String clientId,
        String realm,
        String name,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(clientId, realm, name, authorities);
        this.enabled = true;
    }

    public String getClientId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
