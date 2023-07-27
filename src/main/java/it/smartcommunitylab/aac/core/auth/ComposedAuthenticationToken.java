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

package it.smartcommunitylab.aac.core.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * An auth token for authenticated user and client
 */
public class ComposedAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final UserAuthentication userAuthentication;
    private final ClientAuthentication clientAuthentication;

    public ComposedAuthenticationToken(
        UserAuthentication userAuthentication,
        ClientAuthentication clientAuthentication
    ) {
        super(buildAuthorities(userAuthentication, clientAuthentication));
        Assert.notNull(userAuthentication, "user authentication can not be null");
        Assert.notNull(clientAuthentication, "client authentication can not be null");

        this.userAuthentication = userAuthentication;
        this.clientAuthentication = clientAuthentication;

        super.setAuthenticated(userAuthentication.isAuthenticated() && clientAuthentication.isAuthenticated());
    }

    public UserAuthentication getUserAuthentication() {
        return userAuthentication;
    }

    public ClientAuthentication getClientAuthentication() {
        return clientAuthentication;
    }

    public String getUserId() {
        return userAuthentication.getSubjectId();
    }

    public String getClientId() {
        return clientAuthentication.getClientId();
    }

    @Override
    public String getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.userAuthentication.getPrincipal();
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(
        UserAuthentication userAuthentication,
        ClientAuthentication clientAuthentication
    ) {
        // build an immutable set of authorities
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (userAuthentication != null && userAuthentication.isAuthenticated()) {
            authorities.addAll(userAuthentication.getAuthorities());
        }
        if (clientAuthentication != null && clientAuthentication.isAuthenticated()) {
            authorities.addAll(clientAuthentication.getAuthorities());
        }

        return Collections.unmodifiableList(new ArrayList<>(authorities));
    }
}
