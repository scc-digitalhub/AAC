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
import it.smartcommunitylab.aac.openid.provider.OIDCCoreMetadataGenerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
 * OAuth2 Dynamic Client Registration Metadata
 * https://datatracker.ietf.org/doc/html/rfc8414
 */
@Component
public class OAuth2AuthorizationServerMetadataGenerator implements OAuth2MetadataGenerator {

    private static final String[] METADATA = {
        "issuer",
        "authorization_endpoint",
        "token_endpoint",
        "jwks_uri",
        "scopes_supported",
        "response_types_supported",
        "grant_types_supported",
        "token_endpoint_auth_methods_supported",
        "token_endpoint_auth_signing_alg_values_supported",
        "ui_locales_supported",
    };

    @Autowired
    private OIDCCoreMetadataGenerator oidcMetadataGenerator;

    @Override
    public Map<String, Object> generate() {
        //@formatter:off
        /**
         * extend metadata for oauth2 for dynamic client registration
         * 
        issuer
            REQUIRED.  The authorization server's issuer identifier, which is
            a URL that uses the "https" scheme and has no query or fragment
            components.  Authorization server metadata is published at a
            location that is ".well-known" according to RFC 5785 [RFC5785]
            derived from this issuer identifier, as described in Section 3.
            The issuer identifier is used to prevent authorization server mix-
            up attacks, as described in "OAuth 2.0 Mix-Up Mitigation"
            [MIX-UP].
        authorization_endpoint
            URL of the authorization server's authorization endpoint
            [RFC6749].  This is REQUIRED unless no grant types are supported
            that use the authorization endpoint.
        token_endpoint
            URL of the authorization server's token endpoint [RFC6749].  This
            is REQUIRED unless only the implicit grant type is supported.
        jwks_uri
            OPTIONAL.  URL of the authorization server's JWK Set [JWK]
            document.  The referenced document contains the signing key(s) the
            client uses to validate signatures from the authorization server.
            This URL MUST use the "https" scheme.  The JWK Set MAY also
            contain the server's encryption key or keys, which are used by
            clients to encrypt requests to the server.  When both signing and
            encryption keys are made available, a "use" (public key use)
            parameter value is REQUIRED for all keys in the referenced JWK Set
            to indicate each key's intended usage.
        scopes_supported
            RECOMMENDED.  JSON array containing a list of the OAuth 2.0
            [RFC6749] "scope" values that this authorization server supports.
            Servers MAY choose not to advertise some supported scope values
            even when this parameter is used.
        response_types_supported
            REQUIRED.  JSON array containing a list of the OAuth 2.0
            "response_type" values that this authorization server supports.
            The array values used are the same as those used with the
            "response_types" parameter defined by "OAuth 2.0 Dynamic Client
            Registration Protocol" [RFC7591].
        grant_types_supported
            OPTIONAL.  JSON array containing a list of the OAuth 2.0 grant
            type values that this authorization server supports.  The array
            values used are the same as those used with the "grant_types"
            parameter defined by "OAuth 2.0 Dynamic Client Registration
            Protocol" [RFC7591].  If omitted, the default value is
            "["authorization_code", "implicit"]".
        token_endpoint_auth_methods_supported
            OPTIONAL.  JSON array containing a list of client authentication
            methods supported by this token endpoint.  Client authentication
            method values are used in the "token_endpoint_auth_method"
            parameter defined in Section 2 of [RFC7591].  If omitted, the
            default is "client_secret_basic" -- the HTTP Basic Authentication
            Scheme specified in Section 2.3.1 of OAuth 2.0 [RFC6749].
        token_endpoint_auth_signing_alg_values_supported
            OPTIONAL.  JSON array containing a list of the JWS signing
            algorithms ("alg" values) supported by the token endpoint for the
            signature on the JWT [JWT] used to authenticate the client at the
            token endpoint for the "private_key_jwt" and "client_secret_jwt"
            authentication methods.  This metadata entry MUST be present if
            either of these authentication methods are specified in the
            "token_endpoint_auth_methods_supported" entry.  No default
            algorithms are implied if this entry is omitted.  Servers SHOULD
            support "RS256".  The value "none" MUST NOT be used.
        service_documentation
            OPTIONAL.  URL of a page containing human-readable information
            that developers might want or need to know when using the
            authorization server.  In particular, if the authorization server
            does not support Dynamic Client Registration, then information on
            how to register clients needs to be provided in this
            documentation.
        ui_locales_supported
            OPTIONAL.  Languages and scripts supported for the user interface,
            represented as a JSON array of language tag values from BCP 47
            [RFC5646].  If omitted, the set of supported languages and scripts
            is unspecified.
        op_policy_uri
            OPTIONAL.  URL that the authorization server provides to the
            person registering the client to read about the authorization
            server's requirements on how the client can use the data provided
            by the authorization server.  The registration process SHOULD
            display this URL to the person registering the client if it is
            given.  As described in Section 5, despite the identifier
            "op_policy_uri" appearing to be OpenID-specific, its usage in this
            specification is actually referring to a general OAuth 2.0 feature
            that is not specific to OpenID Connect.
        op_tos_uri
            OPTIONAL.  URL that the authorization server provides to the
            person registering the client to read about the authorization
            server's terms of service.  The registration process SHOULD
            display this URL to the person registering the client if it is
            given.  As described in Section 5, despite the identifier
            "op_tos_uri", appearing to be OpenID-specific, its usage in this
            specification is actually referring to a general OAuth 2.0 feature
            that is not specific to OpenID Connect.     

         */
        //@formatter:on

        Map<String, Object> map = new HashMap<>();

        //leverage OIDC metadata generator for shared attributes
        List<String> shared = Stream.of(METADATA).collect(Collectors.toList());
        oidcMetadataGenerator
            .generate()
            .entrySet()
            .forEach(e -> {
                if (shared.contains(e.getKey())) {
                    map.put(e.getKey(), e.getValue());
                }
            });

        //TODO add OPTIONAL fields

        return map;
    }
}
