package it.smartcommunitylab.aac.openid.endpoint;

import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;
import net.minidev.json.JSONObject;

@Controller
@Tag(name = "OpenID Connect Discovery")
public class JWKSetPublishingEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String JWKS_URL = "/jwk";

    @Value("${security.cache.jwks}")
    private String cacheControl;

    @Autowired
    private JWTSigningAndValidationService jwtService;

    @Operation(summary = "JSON Web Key Set")
    @RequestMapping(method = RequestMethod.GET, value = JWKS_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwks() {

        // map from key id to key
        Map<String, JWK> keys = jwtService.getAllPublicKeys();

        // build as set to leverage toJSONObject, will hide private keys
        JWKSet jwkSet = new JWKSet(new ArrayList<>(keys.values()));

        // return with 200 and set custom cache header
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .body(jwkSet.toJSONObject(true));

    }

    // TODO per-realm keys

}