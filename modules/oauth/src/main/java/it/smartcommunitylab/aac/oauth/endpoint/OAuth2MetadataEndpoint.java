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

package it.smartcommunitylab.aac.oauth.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.oauth.common.OAuth2MetadataGenerator;
import it.smartcommunitylab.aac.openid.common.OIDCMetadataGenerator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/*
 * OAuth2 Authorization Server Metadata
 * https://tools.ietf.org/html/rfc8414
 *
 */
@Controller
@Tag(name = "OAuth 2.0 Authorization Server Metadata")
public class OAuth2MetadataEndpoint {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String OAUTH2_CONFIGURATION_URL = Config.WELL_KNOWN_URL + "/oauth-authorization-server";

    //keep a local cache
    //TODO refactor when per-realm is supported
    private Map<String, Object> configuration;

    @Value("${application.url}")
    private String applicationURL;

    @Autowired
    private Collection<OAuth2MetadataGenerator> oauthMetadataGenerators;

    @Operation(summary = "Get authorization server metadata")
    @GetMapping(value = OAUTH2_CONFIGURATION_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Map<String, Object> serverMetadata() {
        return getConfiguration();
    }

    public Map<String, Object> getConfiguration() {
        if (configuration == null) {
            logger.debug("Generate OAuth2 provider metadata via generators");
            // provider metadata from generators
            //NOTE: we need an actual map to perform removal on empty elements
            Map<String, Object> m = new HashMap<>();

            //include only oauth2 metadata
            oauthMetadataGenerators.forEach(p -> m.putAll(p.generate()));

            //as per spec, filter claims with zero or null elements
            //TODO cleanup filter
            m
                .entrySet()
                .removeIf(
                    e ->
                        e.getValue() == null ||
                        ((e.getValue() instanceof String) && !StringUtils.hasText((String) e.getValue())) ||
                        ((e.getValue() instanceof Collection) && ((Collection<?>) e.getValue()).isEmpty())
                );

            //cache
            configuration = m;
        }

        return configuration;
    }
}
