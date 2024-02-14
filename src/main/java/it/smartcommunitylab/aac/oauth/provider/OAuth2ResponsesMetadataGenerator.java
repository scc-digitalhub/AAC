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
import it.smartcommunitylab.aac.oauth.model.ResponseMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/*
 * OAuth2 Multiple Response Type Metadata
 * https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
 * https://datatracker.ietf.org/doc/html/rfc8414
 */
@Component
public class OAuth2ResponsesMetadataGenerator implements OAuth2MetadataGenerator {

    //modes are static for now
    private static final ResponseMode[] modes = { ResponseMode.QUERY, ResponseMode.FRAGMENT, ResponseMode.FORM_POST };

    @Override
    public Map<String, Object> generate() {
        List<String> responseModes = Stream.of(modes).map(t -> t.getValue()).collect(Collectors.toList());

        return Collections.singletonMap("response_modes_supported", responseModes);
    }
}
