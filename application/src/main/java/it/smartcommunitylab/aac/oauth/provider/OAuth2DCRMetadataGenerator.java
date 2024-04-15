/**
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

package it.smartcommunitylab.aac.oauth.provider;

import it.smartcommunitylab.aac.oauth.common.OAuth2MetadataGenerator;
import it.smartcommunitylab.aac.oauth.endpoint.ClientRegistrationEndpoint;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 * OAuth2 Dynamic Client Registration Metadata
 * https://datatracker.ietf.org/doc/html/rfc7591
 * https://datatracker.ietf.org/doc/html/rfc8414
 */
@Component
public class OAuth2DCRMetadataGenerator implements OAuth2MetadataGenerator {

    @Value("${application.url}")
    private String applicationURL;

    @Override
    public Map<String, Object> generate() {
        //@formatter:off
        /**
         * extend metadata for oauth2 for dynamic client registration
         * 
            registration_endpoint 
                OPTIONAL.  URL of the authorization server's OAuth 2.0 Dynamic
                Client Registration endpoint [RFC7591].

         */
        //@formatter:on

        String baseUrl = applicationURL;
        Map<String, Object> map = new HashMap<>();

        map.put("registration_endpoint", baseUrl + ClientRegistrationEndpoint.REGISTRATION_URL);

        return map;
    }
}
