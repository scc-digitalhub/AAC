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

package it.smartcommunitylab.aac.oauth.store;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.util.DigestUtils;

/*
 * Extended authKey generator, reads parameters from request + userAuth to build a unique key
 *
 * TODO drop, we should really avoid mapping the authentication along the token
 * we don't need to read back from an authentication, it is a bad and insecure design
 */
public class ExtendedAuthenticationKeyGenerator implements AuthenticationKeyGenerator {

    private static final String CLIENT_ID = "client_id";
    private static final String SCOPE = "scope";
    private static final String USERNAME = "username";
    private static final String REALM = "realm";
    private static final String RESOURCE_IDS = "resource_ids";

    private static final String SESSION = "session";

    public String extractKey(OAuth2Authentication authentication) {
        OAuth2Request request = authentication.getOAuth2Request();

        // use a sorted map as source to ensure consistency
        Map<String, String> values = new TreeMap<String, String>();

        values.put(CLIENT_ID, request.getClientId());

        if (!authentication.isClientOnly()) {
            values.put(USERNAME, authentication.getName());
        }

        if (request.getScope() != null) {
            values.put(SCOPE, OAuth2Utils.formatParameterList(new TreeSet<String>(request.getScope())));
        } else {
            values.put(SCOPE, "-");
        }

        if (request.getScope() != null) {
            values.put(RESOURCE_IDS, OAuth2Utils.formatParameterList(new TreeSet<String>(request.getResourceIds())));
        } else {
            values.put(RESOURCE_IDS, "-");
        }

        // build key and reduce to md5hash
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }

        try {
            return DigestUtils.md5DigestAsHex(sb.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("error building key");
        }
    }
}
