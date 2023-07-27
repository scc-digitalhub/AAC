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

package it.smartcommunitylab.aac.oauth.request;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import java.util.Collection;
import java.util.Map;
import org.springframework.util.Assert;

public class ResourceOwnerPasswordTokenRequest extends TokenRequest {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private static final String GRANT_TYPE = AuthorizationGrantType.PASSWORD.getValue();

    private String username;
    private String password;

    public ResourceOwnerPasswordTokenRequest(
        Map<String, String> requestParameters,
        String clientId,
        String username,
        String password,
        Collection<String> scope,
        Collection<String> resourceIds,
        Collection<String> audience
    ) {
        super(requestParameters, clientId, GRANT_TYPE, scope, resourceIds, audience);
        Assert.hasText(username, "username is required");

        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
