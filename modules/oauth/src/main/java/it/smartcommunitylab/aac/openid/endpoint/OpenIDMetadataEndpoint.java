/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.openid.endpoint;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * OpenID Connect Discovery Provider Metadata
 * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
 *
 */
@Controller
@Tag(name = "OpenID Connect Discovery")
public class OpenIDMetadataEndpoint {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String OPENID_CONFIGURATION_URL = Config.WELL_KNOWN_URL + "/openid-configuration";

    //keep a local cache
    //TODO refactor when per-realm is supported
    private Map<String, Object> configuration;

    @Autowired
    private Collection<OIDCMetadataGenerator> oidcMetadataGenerators;

    @Autowired
    private Collection<OAuth2MetadataGenerator> oauthMetadataGenerators;

    @Operation(summary = "Get OpenID provider configuration information")
    @GetMapping(value = OPENID_CONFIGURATION_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Map<String, Object> providerConfiguration() {
        return getConfiguration();
    }

    public Map<String, Object> getConfiguration() {
        if (configuration == null) {
            logger.debug("Generate OIDC provider metadata via generators");
            // provider metadata from generators
            //NOTE: we need an actual map to perform removal on empty elements
            Map<String, Object> m = new HashMap<>();

            //include oauth2 metadata
            //NOTE: additional params are permitted by OIDC Metadata specs
            oauthMetadataGenerators.forEach(p -> m.putAll(p.generate()));

            //include oidc metadata
            //NOTE: will override overlapping params from oauth2
            oidcMetadataGenerators.forEach(p -> m.putAll(p.generate()));

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
    // private String getBaseUrl() {
    //     String baseUrl = applicationURL;

    //     //        if (!baseUrl.endsWith("/")) {
    //     //            logger.debug("Configured baseUrl doesn't end in /, adding for discovery: {}", baseUrl);
    //     //            baseUrl = baseUrl.concat("/");
    //     //        }
    //     return baseUrl;
    // }
}
