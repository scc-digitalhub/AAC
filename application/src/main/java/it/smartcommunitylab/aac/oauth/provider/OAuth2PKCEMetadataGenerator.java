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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/*
 * OAuth2 PKCE Metadata
 * https://datatracker.ietf.org/doc/html/rfc7636
 * https://datatracker.ietf.org/doc/html/rfc8414
 */
@Component
public class OAuth2PKCEMetadataGenerator implements OAuth2MetadataGenerator {

    @Override
    public Map<String, Object> generate() {
        //@formatter:off
        /**
         * extend metadata for oauth2 for PKCE
         * 
            code_challenge_methods_supported
                OPTIONAL.  JSON array containing a list of Proof Key for Code
                Exchange (PKCE) [RFC7636] code challenge methods supported by this
                authorization server.          

         */
        //@formatter:on

        Map<String, Object> map = new HashMap<>();
        map.put("code_challenge_methods_supported", Collections.singleton("S256")); // as per spec do not expose plain

        return map;
    }
}
