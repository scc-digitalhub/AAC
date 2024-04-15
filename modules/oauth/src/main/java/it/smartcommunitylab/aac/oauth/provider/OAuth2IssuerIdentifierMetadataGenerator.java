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
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/*
 * OAuth2 Authorization Server Issuer Identification Metadata
 * https://datatracker.ietf.org/doc/html/rfc9207
 * https://datatracker.ietf.org/doc/html/rfc8414
 */
@Component
public class OAuth2IssuerIdentifierMetadataGenerator implements OAuth2MetadataGenerator {

    @Override
    public Map<String, Object> generate() {
        //@formatter:off
        /**
         * extend metadata for oauth2 for issuer identifier
         * 
            authorization_response_iss_parameter_supported
                Boolean parameter indicating whether the authorization server provides the "iss"
                parameter in the authorization response as defined in Section 2.
                If omitted, the default value is false.       

         */
        //@formatter:on

        Map<String, Object> map = new HashMap<>();
        map.put("authorization_response_iss_parameter_supported", true);

        return map;
    }
}
