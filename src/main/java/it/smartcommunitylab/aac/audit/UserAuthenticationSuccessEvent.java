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

package it.smartcommunitylab.aac.audit;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.util.Assert;

//TODO add custom serializer
//TODO add subtype inference

public class UserAuthenticationSuccessEvent extends AuthenticationSuccessEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String provider;
    private final String realm;

    public UserAuthenticationSuccessEvent(String authority, String provider, String realm, UserAuthentication auth) {
        super(auth);
        Assert.hasText(authority, "authority is required");
        Assert.notNull(provider, "provider is required");
        Assert.notNull(realm, "realm is required");

        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
    }

    public UserAuthentication getAuthenticationToken() {
        return (UserAuthentication) super.getAuthentication();
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    public String getRealm() {
        return realm;
    }

    public WebAuthenticationDetails getDetails() {
        return getAuthenticationToken().getWebAuthenticationDetails();
    }
}
