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

package it.smartcommunitylab.aac.clients.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.model.Client;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import java.util.Collection;
import javax.annotation.Nullable;
import org.springframework.security.core.GrantedAuthority;

/*
 * A client authentication
 */
public interface ClientAuthentication extends ExtendedAuthentication {
    /*
     * Client
     */
    @Override
    public default String getType() {
        return SystemKeys.RESOURCE_CLIENT;
    }

    // client details reports client info relevant for the auth/security context
    // do note that client details are immutable within a session:
    // in order to refresh we need to build a new session
    public ClientDetails getClientDetails();

    public default String getClientId() {
        return getClientDetails() != null ? getClientDetails().getClientId() : null;
    }

    @Override
    default Collection<? extends GrantedAuthority> getAuthorities() {
        return getClientDetails() != null ? getClientDetails().getAuthorities() : null;
    }

    // client (temporarily) stores user info and resources
    // for consumption *outside* the auth/security context
    // do note that this field *IS* refreshable, as in actual implementations
    // could update the field as needed during the session lifetime
    public @Nullable Client getClient();

    public void setClient(Client client);

    /*
     * web auth details
     * TODO remove from here, some auth could be NOT web
     */
    public @Nullable WebAuthenticationDetails getWebAuthenticationDetails();

    @Override
    default Object getDetails() {
        //web auth are the default details,
        return getWebAuthenticationDetails();
    }
}
