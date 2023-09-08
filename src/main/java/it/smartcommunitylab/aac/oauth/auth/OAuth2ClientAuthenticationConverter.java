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

import it.smartcommunitylab.aac.core.auth.ClientAuthenticationConverter;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import jakarta.servlet.http.HttpServletRequest;

public abstract class OAuth2ClientAuthenticationConverter implements ClientAuthenticationConverter {

    @Override
    public OAuth2ClientAuthenticationToken convert(HttpServletRequest request) {
        OAuth2ClientAuthenticationToken token = attemptConvert(request);
        if (token == null) {
            return null;
        }

        // collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        token.setDetails(webAuthenticationDetails);

        return token;
    }

    protected abstract OAuth2ClientAuthenticationToken attemptConvert(HttpServletRequest request);
}
