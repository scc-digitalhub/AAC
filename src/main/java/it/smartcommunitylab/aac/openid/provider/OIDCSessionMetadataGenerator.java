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

package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.openid.common.OIDCMetadataGenerator;
import it.smartcommunitylab.aac.openid.endpoint.EndSessionEndpoint;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OIDCSessionMetadataGenerator implements OIDCMetadataGenerator {

    @Value("${application.url}")
    private String applicationURL;

    @Override
    public Map<String, Object> generate() {
        //@formatter:off
        /*
         * OpenID Provider Discovery Metadata
         * https://openid.net/specs/openid-connect-session-1_0.html#OPMetadata
         *
            check_session_iframe
                OPTIONAL. URL of an OP endpoint that provides a page to
                support cross-origin communications for session state information with the RP
                Client, using the HTML5 postMessage API. The page is loaded from an invisible
                iframe embedded in an RP page so that it can run in the OP's security
                context. See [OpenID.Session]. 
            end_session_endpoint 
                OPTIONAL. URL of the OP's endpoint that initiates logging out the End-User. See [OpenID.Session]
         */
        //@formatter:on
        String baseUrl = applicationURL;
        Map<String, Object> m = new HashMap<>();

        //        m.put("check_session_iframe",""); //not supported
        m.put("end_session_endpoint", baseUrl + EndSessionEndpoint.END_SESSION_URL);

        return m;
    }
}
