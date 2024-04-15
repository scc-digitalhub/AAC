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

import com.nimbusds.jose.JWSAlgorithm;
import it.smartcommunitylab.aac.oauth.common.OAuth2MetadataGenerator;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 * OAuth2 Token Introspection Metadata
 * https://datatracker.ietf.org/doc/html/rfc7662
 * https://datatracker.ietf.org/doc/html/rfc8414
 */
@Component
public class OAuth2IntrospectionMetadataGenerator implements OAuth2MetadataGenerator {

    @Value("${application.url}")
    private String applicationURL;

    @Override
    public Map<String, Object> generate() {
        //@formatter:off
        /**
         * extend metadata for oauth2 token introspection
         * 
            introspection_endpoint
                OPTIONAL.  URL of the authorization server's OAuth 2.0
                introspection endpoint
            introspection_endpoint_auth_methods_supported
                OPTIONAL.  JSON array containing a list of client authentication
                methods supported by this introspection endpoint.
            introspection_endpoint_auth_signing_alg_values_supported
                OPTIONAL.  JSON array containing a list of the JWS signing
                algorithms ("alg" values) supported by the introspection endpoint
                for the signature on the JWT
         */
        //@formatter:on

        String baseUrl = applicationURL;
        Map<String, Object> map = new HashMap<>();

        // static list of base algs supported
        // TODO check support
        // note: this does NOT depend on signService but on auth converters
        List<String> authSigninAlgorithms = Stream
            .of(
                JWSAlgorithm.HS256,
                JWSAlgorithm.HS384,
                JWSAlgorithm.HS512,
                JWSAlgorithm.RS256,
                JWSAlgorithm.RS384,
                JWSAlgorithm.RS512
            )
            .map(a -> a.getName())
            .collect(Collectors.toList());

        List<String> authMethods = Stream
            .of(
                AuthenticationMethod.CLIENT_SECRET_BASIC,
                AuthenticationMethod.CLIENT_SECRET_POST,
                AuthenticationMethod.CLIENT_SECRET_JWT,
                AuthenticationMethod.PRIVATE_KEY_JWT
            )
            .map(t -> t.getValue())
            .collect(Collectors.toList());

        map.put("introspection_endpoint", baseUrl + TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL);
        map.put("introspection_endpoint_auth_methods_supported", authMethods);
        map.put("introspection_endpoint_auth_signing_alg_values_supported", authSigninAlgorithms);

        return map;
    }
}
