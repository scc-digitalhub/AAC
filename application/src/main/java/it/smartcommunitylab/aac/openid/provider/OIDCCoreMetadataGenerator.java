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

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ResponseType;
import it.smartcommunitylab.aac.oauth.model.SubjectType;
import it.smartcommunitylab.aac.openid.common.OIDCMetadataGenerator;
import it.smartcommunitylab.aac.openid.endpoint.JWKSetPublishingEndpoint;
import it.smartcommunitylab.aac.openid.endpoint.UserInfoEndpoint;
import it.smartcommunitylab.aac.openid.scope.OpenIdScopeProvider;
import it.smartcommunitylab.aac.profiles.scope.OpenIdProfileScopeProvider;
import it.smartcommunitylab.aac.profiles.scope.ProfileScopeProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OIDCCoreMetadataGenerator implements OIDCMetadataGenerator {

    @Value("${application.url}")
    private String applicationURL;

    @Value("${jwt.issuer}")
    private String issuer;

    @Autowired
    private JWTSigningAndValidationService signService;

    @Autowired
    private JWTEncryptionAndDecryptionService encService;

    @Override
    public Map<String, Object> generate() {
        //@formatter:off
        /*
         * OpenID Provider Metadata
         * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
         * 
            issuer
                REQUIRED. URL using the https scheme with no query or fragment component that the OP asserts as its Issuer Identifier.
            authorization_endpoint
                OPTIONAL. URL of the OP's Authentication and Authorization Endpoint [OpenID.Messages].
            token_endpoint
                OPTIONAL. URL of the OP's OAuth 2.0 Token Endpoint [OpenID.Messages].
            userinfo_endpoint
                RECOMMENDED. URL of the OP's UserInfo Endpoint [OpenID.Messages]. This URL MUST use the https scheme
                and MAY contain port, path, and query parameter components.
            jwks_uri
                REQUIRED. URL of the OP's JSON Web Key Set [JWK] document. This contains the signing key(s) the Client uses to
                validate signatures from the OP. The JWK Set MAY also contain the Server's encryption key(s),
                which are used by Clients to encrypt requests to the Server. When both signing and encryption keys are made available,
                a use (Key Use) parameter value is REQUIRED for all keys in the document to indicate each key's intended usage.
            scopes_supported
                RECOMMENDED. JSON array containing a list of the OAuth 2.0 [RFC6749] scope values that this server supports.
                The server MUST support the openid scope value.
            response_types_supported
                REQUIRED. JSON array containing a list of the OAuth 2.0 response_type values that this server supports.
                The server MUST support the code, id_token, and the token id_token response type values.
            grant_types_supported
                OPTIONAL. JSON array containing a list of the OAuth 2.0 grant type values that this server supports.
                The server MUST support the authorization_code and implicit grant type values
                and MAY support the urn:ietf:params:oauth:grant-type:jwt-bearer grant type defined in OAuth JWT Bearer Token Profiles [OAuth.JWT].
                If omitted, the default value is ["authorization_code", "implicit"].
            acr_values_supported
                OPTIONAL. JSON array containing a list of the Authentication Context Class References that this server supports.
            subject_types_supported
                REQUIRED. JSON array containing a list of the subject identifier types that this server supports. Valid types include pairwise and public.
            id_token_signing_alg_values_supported
                REQUIRED. JSON array containing a list of the JWS signing algorithms (alg values) supported by the Authorization Server for the
                ID Token to encode the Claims in a JWT [JWT].
            id_token_encryption_alg_values_supported
                OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values) supported by the Authorization Server for the
                ID Token to encode the Claims in a JWT [JWT].
            id_token_encryption_enc_values_supported
                OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) supported by the Authorization Server for the
                ID Token to encode the Claims in a JWT [JWT].                   
            userinfo_signing_alg_values_supported
                OPTIONAL. JSON array containing a list of the JWS [JWS] signing algorithms (alg values) [JWA] supported by the UserInfo Endpoint to
                encode the Claims in a JWT [JWT].
            userinfo_encryption_alg_values_supported
                OPTIONAL. JSON array containing a list of the JWE [JWE] encryption algorithms (alg values) [JWA] supported by the UserInfo Endpoint to
                encode the Claims in a JWT [JWT].
            userinfo_encryption_enc_values_supported
                OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) [JWA] supported by the UserInfo Endpoint to
                encode the Claims in a JWT [JWT].
            request_object_signing_alg_values_supported
                OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported by the Authorization Server for
                the Request Object described in Section 2.9 of OpenID Connect Messages 1.0 [OpenID.Messages]. These algorithms are used both when
                the Request Object is passed by value (using the request parameter) and when it is passed by reference (using the request_uri parameter).
                Servers SHOULD support none and RS256.
            request_object_encryption_alg_values_supported
                OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values) supported by the Authorization Server for
                the Request Object described in Section 2.9 of OpenID Connect Messages 1.0 [OpenID.Messages]. These algorithms are used both when
                the Request Object is passed by value and when it is passed by reference.
            request_object_encryption_enc_values_supported
                OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) supported by the Authorization Server for
                the Request Object described in Section 2.9 of OpenID Connect Messages 1.0 [OpenID.Messages]. These algorithms are used both when
                the Request Object is passed by value and when it is passed by reference.
            token_endpoint_auth_methods_supported
                OPTIONAL. JSON array containing a list of authentication methods supported by this Token Endpoint.
                The options are client_secret_post, client_secret_basic, client_secret_jwt, and private_key_jwt,
                as described in Section 2.2.1 of OpenID Connect Messages 1.0 [OpenID.Messages].
                Other authentication methods MAY be defined by extensions.
                If omitted, the default is client_secret_basic -- the HTTP Basic Authentication Scheme as specified in
                Section 2.3.1 of OAuth 2.0 [RFC6749].
            token_endpoint_auth_signing_alg_values_supported
                OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported by the Token Endpoint for
                the private_key_jwt and client_secret_jwt methods to encode the JWT [JWT]. Servers SHOULD support RS256.
            display_values_supported
                OPTIONAL. JSON array containing a list of the display parameter values that the OpenID Provider supports.
                These values are described in Section 2.1.1 of OpenID Connect Messages 1.0 [OpenID.Messages].
            claim_types_supported
                OPTIONAL. JSON array containing a list of the Claim Types that the OpenID Provider supports.
                These Claim Types are described in Section 2.6 of OpenID Connect Messages 1.0 [OpenID.Messages].
                Values defined by this specification are normal, aggregated, and distributed.
                If not specified, the implementation supports only normal Claims.
            claims_supported
                RECOMMENDED. JSON array containing a list of the Claim Names of the Claims that the OpenID Provider MAY be able to supply values for.
                Note that for privacy or other reasons, this might not be an exhaustive list.
            service_documentation
                OPTIONAL. URL of a page containing human-readable information that developers might want or need to know when using the OpenID Provider.
                In particular, if the OpenID Provider does not support Dynamic Client Registration, then information on how to register Clients needs
                to be provided in this documentation.
            claims_locales_supported
                OPTIONAL. Languages and scripts supported for values in Claims being returned, represented as a JSON array of
                BCP47 [RFC5646] language tag values. Not all languages and scripts are necessarily supported for all Claim values.
            ui_locales_supported
                OPTIONAL. Languages and scripts supported for the user interface, represented as a JSON array of BCP47 [RFC5646] language tag values.
            claims_parameter_supported
                OPTIONAL. Boolean value specifying whether the OP supports use of the claims parameter, with true indicating support.
                If omitted, the default value is false.
            request_parameter_supported
                OPTIONAL. Boolean value specifying whether the OP supports use of the request parameter, with true indicating support.
                If omitted, the default value is false.
            request_uri_parameter_supported
                OPTIONAL. Boolean value specifying whether the OP supports use of the request_uri parameter, with true indicating support.
                If omitted, the default value is true.
            require_request_uri_registration
                OPTIONAL. Boolean value specifying whether the OP requires any request_uri values used to be pre-registered using
                the request_uris registration parameter. Pre-registration is REQUIRED when the value is true. If omitted, the default value is false.
            op_policy_uri OPTIONAL. URL that the OpenID Provider provides
         * to the person registering the Client to read about the OP's requirements on
         * how the Relying Party can use the data provided by the OP. The registration
         * process SHOULD display this URL to the person registering the Client if it is
         * given. op_tos_uri OPTIONAL. URL that the OpenID Provider provides to the
         * person registering the Client to read about OpenID Provider's terms of
         * service. The registration process SHOULD display this URL to the person
         * registering the Client if it is given.
         */
        //@formatter:on

        String baseUrl = applicationURL;
        Map<String, Object> m = new HashMap<>();

        m.put("issuer", issuer);
        m.put("authorization_endpoint", baseUrl + AuthorizationEndpoint.AUTHORIZATION_URL);
        m.put("token_endpoint", baseUrl + TokenEndpoint.TOKEN_URL);
        m.put("userinfo_endpoint", baseUrl + UserInfoEndpoint.USERINFO_URL);
        m.put("jwks_uri", baseUrl + JWKSetPublishingEndpoint.JWKS_URL);

        //TODO move to dedicated generator when per-realm scopes are available
        List<String> scopes = new ArrayList<>();
        scopes.addAll(OpenIdScopeProvider.scopes.stream().map(s -> s.getScope()).collect(Collectors.toList()));
        scopes.addAll(OpenIdProfileScopeProvider.scopes.stream().map(s -> s.getScope()).collect(Collectors.toList()));
        scopes.addAll(ProfileScopeProvider.scopes.stream().map(s -> s.getScope()).collect(Collectors.toList()));
        m.put("scopes_supported", scopes);

        List<String> responseTypes = Stream
            .of(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN)
            .map(t -> t.getValue())
            .collect(Collectors.toList());
        m.put("response_types_supported", responseTypes);

        List<String> grantTypes = Stream
            .of(
                AuthorizationGrantType.AUTHORIZATION_CODE,
                AuthorizationGrantType.IMPLICIT,
                AuthorizationGrantType.PASSWORD,
                AuthorizationGrantType.CLIENT_CREDENTIALS,
                AuthorizationGrantType.REFRESH_TOKEN
            )
            .map(t -> t.getValue())
            .collect(Collectors.toList());
        m.put("grant_types_supported", grantTypes);

        // unsupported
        //      m.put("acr_values_supported", "");

        //pairwise currently unsupported
        List<String> subjectTypes = Stream.of(SubjectType.PUBLIC).map(t -> t.getValue()).collect(Collectors.toList());
        m.put("subject_types_supported", subjectTypes);

        List<String> signAlgorithms = signService
            .getAllSigningAlgsSupported()
            .stream()
            .map(a -> a.getName())
            .collect(Collectors.toList());
        List<String> encAlgorithms = encService
            .getAllEncryptionAlgsSupported()
            .stream()
            .map(a -> a.getName())
            .collect(Collectors.toList());
        List<String> encMethods = encService
            .getAllEncryptionEncsSupported()
            .stream()
            .map(a -> a.getName())
            .collect(Collectors.toList());

        m.put("id_token_signing_alg_values_supported", signAlgorithms);
        m.put("id_token_encryption_alg_values_supported", encAlgorithms);
        m.put("id_token_encryption_enc_values_supported", encMethods);

        // unsupported
        //        m.put("userinfo_signing_alg_values_supported",signAlgorithms);
        //        m.put("userinfo_encryption_alg_values_supported",encAlgorithms);
        //        m.put("userinfo_encryption_enc_values_supported",encMethods);

        // support only plaintext
        m.put("request_object_signing_alg_values_supported", Collections.singleton(JWSAlgorithm.NONE.getName()));
        m.put("request_object_encryption_alg_values_supported", Collections.singleton(JWEAlgorithm.NONE.getName()));
        m.put("request_object_encryption_enc_values_supported", Collections.singleton(EncryptionMethod.NONE.getName()));

        List<String> authMethods = Stream
            .of(
                AuthenticationMethod.CLIENT_SECRET_BASIC,
                AuthenticationMethod.CLIENT_SECRET_POST,
                AuthenticationMethod.NONE
            )
            .map(t -> t.getValue())
            .collect(Collectors.toList());
        m.put("token_endpoint_auth_methods_supported", authMethods);

        // unsupported
        //        m.put("token_endpoint_auth_signing_alg_values_supported",signAlgorithms);

        m.put("display_values_supported", Collections.singleton("page"));
        m.put("claim_types_supported", Collections.singleton("normal"));

        // TODO export claim names from providers
        List<String> claimsSupported = Stream
            .of(
                "sub",
                "iss",
                "auth_time",
                "name",
                "given_name",
                "family_name",
                "preferred_username",
                "email",
                "email_verified",
                "locale",
                "zoneinfo"
            )
            .collect(Collectors.toList());
        m.put("claims_supported", claimsSupported);
        //	          m.put("service_documentation",""); //not supported
        //	          m.put("claims_locales_supported",""); //not supported
        //	          m.put("ui_locales_supported",""); //not supported
        m.put("claims_parameter_supported", false);
        m.put("request_parameter_supported", true);
        m.put("request_uri_parameter_supported", false);
        m.put("require_request_uri_registration", false);
        //	          m.put("op_policy_uri",""); //not supported
        //	          m.put("op_tos_uri",""); //not supported

        return m;
    }
}
