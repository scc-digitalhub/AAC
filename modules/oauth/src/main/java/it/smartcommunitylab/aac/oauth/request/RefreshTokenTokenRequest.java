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
import java.util.Collections;
import java.util.Map;
import org.springframework.util.Assert;

public class RefreshTokenTokenRequest extends TokenRequest {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private static final String GRANT_TYPE = AuthorizationGrantType.REFRESH_TOKEN.getValue();

    private String refreshToken;

    // TODO add field for authorizationRequest
    //    private AuthorizationRequest authorizationRequest;

    public RefreshTokenTokenRequest(
        Map<String, String> requestParameters,
        String clientId,
        String refreshToken,
        Collection<String> resourceIds,
        Collection<String> audience
    ) {
        super(requestParameters, clientId, GRANT_TYPE, Collections.emptyList(), resourceIds, audience);
        Assert.hasText(refreshToken, "refresh token is required");
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
