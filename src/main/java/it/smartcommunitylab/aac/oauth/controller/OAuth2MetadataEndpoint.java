package it.smartcommunitylab.aac.oauth.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.openid.controller.OpenIDMetadataEndpoint;

/*
 * OAuth2 Authorization Server Metadata
 * https://tools.ietf.org/html/rfc8414
 * 
 * extends OIDC discovery metadata 
 */
@Controller
@Api(tags = { "AAC OAuth 2.0 Authorization Server Metadata (IETF RFC8414)" })
public class OAuth2MetadataEndpoint  {

    public static final String OAUTH2_CONFIGURATION_URL = Config.WELL_KNOWN_URL + "/oauth-authorization-server";

    private static Map<String, Object> configuration;

    @Value("${application.url}")
    private String applicationURL;
    
    @Autowired
    OpenIDMetadataEndpoint oidcMetadataEndpoint;

    @ApiOperation(value="Get authorization server metadata")
    @RequestMapping("/" + OAUTH2_CONFIGURATION_URL)
    public @ResponseBody Map<String, Object> serverMetadata() {
        return getConfiguration();
    }
    
    private Map<String, Object> getConfiguration() {
        if (configuration == null) {
            //auth server metadata
            Map<String, Object> m = getAuthServerMetadata();
            //add session metadata
            m.putAll(oidcMetadataEndpoint.getSessionMetadata());
            //cache
            configuration = m;
        }
        return configuration;
    }

    public Map<String, Object> getAuthServerMetadata() {
        String baseUrl = applicationURL;

        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl.concat("/");
        }

        // fetch oidc provider metadata
        // oauth2 metadata are an extension compatible with OIDC
        Map<String, Object> m = oidcMetadataEndpoint.getDiscoveryMetadata();

        /**
         * extend metadata for oauth2 from OIDC Provider Metadata
         * 
             revocation_endpoint 
                 OPTIONAL. URL of the authorization server's OAuth 2.0 revocation endpoint
             revocation_endpoint_auth_methods_supported
                 OPTIONAL.  JSON array containing a list of client authentication
                 methods supported by this revocation endpoint.
             revocation_endpoint_auth_signing_alg_values_supported
                    OPTIONAL.  JSON array containing a list of the JWS signing
                    algorithms ("alg" values) supported by the revocation endpoint for
                    the signature on the JWT
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
             code_challenge_methods_supported
                     OPTIONAL.  JSON array containing a list of Proof Key for Code
                     Exchange (PKCE) [RFC7636] code challenge methods supported by this
                     authorization server.
         */
        //load all signing alg
        //TODO check support
        Collection<JWSAlgorithm> clientSymmetricAndAsymmetricSigningAlgs = Lists.newArrayList(JWSAlgorithm.HS256, JWSAlgorithm.HS384, JWSAlgorithm.HS512,
                JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512,
                JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512,
                JWSAlgorithm.PS256, JWSAlgorithm.PS384, JWSAlgorithm.PS512);
        
        m.put("revocation_endpoint", baseUrl + "token_revoke"); // token revocation endpoint
        m.put("revocation_endpoint_auth_methods_supported",  Lists.newArrayList("client_secret_post", "client_secret_basic", "client_secret_jwt", "private_key_jwt", "none"));
        m.put("revocation_endpoint_auth_signing_alg_values_supported", Collections2.transform(clientSymmetricAndAsymmetricSigningAlgs, toAlgorithmName));
        
        m.put("introspection_endpoint", baseUrl + "token_introspection");
        m.put("introspection_endpoint_auth_methods_supported",  Lists.newArrayList("client_secret_post", "client_secret_basic", "client_secret_jwt", "private_key_jwt", "none"));
        m.put("introspection_endpoint_auth_signing_alg_values_supported", Collections2.transform(clientSymmetricAndAsymmetricSigningAlgs, toAlgorithmName));
        
        m.put("code_challenge_methods_supported", Lists.newArrayList("S256")); //as per spec do not expose plain
        return m;
    }
    
    // used to map JWA algorithms objects to strings
    private Function<Algorithm, String> toAlgorithmName = new Function<Algorithm, String>() {
        @Override
        public String apply(Algorithm alg) {
            if (alg == null) {
                return null;
            } else {
                return alg.getName();
            }
        }
    };
}
