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

package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;

public class ClientOAuth2AuthenticatedPrincipal extends DefaultOAuth2AuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public ClientOAuth2AuthenticatedPrincipal(
        String realm,
        String name,
        Map<String, Object> attributes,
        Collection<GrantedAuthority> authorities
    ) {
        super(realm, name, attributes, authorities);
    }

    public String getClientId() {
        return name;
    }
}
