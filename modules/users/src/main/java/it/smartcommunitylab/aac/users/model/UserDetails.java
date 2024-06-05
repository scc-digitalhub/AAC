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

package it.smartcommunitylab.aac.users.model;

import it.smartcommunitylab.aac.auth.model.AuthenticatedDetails;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

/*
 * User details descriptor
 *
 * This model should be used to describe and manage the AAC user, in relation to the realm
 * which "owns" the registrations. Its usage is relevant for the auth/securityContext.
 *
 * Services and controllers should adopt the User model.
 */
@Getter
@Setter
@ToString
public class UserDetails extends AuthenticatedDetails implements CredentialsContainer {

    // we support account status
    private final boolean enabled;
    private final boolean locked;

    //additional properties as context
    //TODO
    @ToString.Exclude
    private Map<String, Serializable> additionalProperties = new HashMap<>();

    public UserDetails(
        String userId,
        String realm,
        String username,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(userId, realm, username, authorities);
        // always enabled at login
        this.enabled = true;
        this.locked = true;
    }

    public String getUserId() {
        return id;
    }

    public String getUsername() {
        return name;
    }

    @Override
    public void eraseCredentials() {}
}
