package it.smartcommunitylab.aac.openid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Serializable;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.OAuth2ConfigUtils;
import it.smartcommunitylab.aac.oauth.OAuth2TestUtils;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.openid.endpoint.JWKSetPublishingEndpoint;

/*
 * JWKSet endpoint test
 * 
 * ref 
 * https://www.rfc-editor.org/rfc/rfc7517
 * https://www.rfc-editor.org/rfc/rfc7519
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JWKSetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BootstrapConfig config;

    @Test
    public void endpointIsAvailable() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(JWKS_URL))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

        // content type is json
        assertEquals(res.getResponse().getContentType(), MediaType.APPLICATION_JSON_VALUE);

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> map = mapper.readValue(json, typeRef);
        assertNotNull(map);
    }

    @Test
    public void keysAreAvailable() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(JWKS_URL))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

        // content type is json
        assertEquals(res.getResponse().getContentType(), MediaType.APPLICATION_JSON_VALUE);

        // read as string
        String json = res.getResponse().getContentAsString();
        assertThat(json).isNotBlank();

        // try to parse keys
        assertDoesNotThrow(() -> {
            JWKSet.parse(json);
        });

        // parse
        JWKSet set = JWKSet.parse(json);
        assertThat(set).isNotNull();

        // at least one public key available
        assertThat(set.getKeys()).isNotEmpty();

        // every key is asymmetric (path is public)
        assertThat(set.getKeys()).allSatisfy(
                key -> {
                    assertThat(key).isNotNull();
                    // key is either RSA or EC
                    assertThat(key.getKeyType()).satisfiesAnyOf(
                            typ -> assertThat(typ).isEqualTo(KeyType.RSA),
                            typ -> assertThat(typ).isEqualTo(KeyType.EC));
                });

    }

    @Test
    public void privateKeyIsNotExposed() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(JWKS_URL))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

        // content type is json
        assertEquals(res.getResponse().getContentType(), MediaType.APPLICATION_JSON_VALUE);

        // read as string
        String json = res.getResponse().getContentAsString();
        assertThat(json).isNotBlank();

        // try to parse keys
        assertDoesNotThrow(() -> {
            JWKSet.parse(json);
        });

        // parse
        JWKSet set = JWKSet.parse(json);
        assertThat(set).isNotNull();

        // every key is public
        assertThat(set.getKeys()).allSatisfy(
                key -> {
                    // private key is not exposed
                    assertThat(key.isPrivate()).isFalse();
                });

    }

    @Test
    public void clientJwtTokenTestWithBasicAuth() throws Exception {
        ClientRegistration client = OAuth2ConfigUtils.with(config).client();

        // fetch a valid client access token
        String accessToken = OAuth2TestUtils.getClientAccessTokenViaClientCredentialsWithBasicAuth(mockMvc,
                client.getClientId(), client.getClientSecret());

        // expect a valid string
        assertThat(accessToken).isNotBlank();

        // parse as JWT
        assertDoesNotThrow(() -> {
            JWTParser.parse(accessToken);
        });

        JWT ujwt = JWTParser.parse(accessToken);
        assertThat(ujwt).isNotNull();

        // header is JWS
        assertThat(ujwt.getHeader()).isInstanceOf(JWSHeader.class);

        // fetch kid
        String kid = ((JWSHeader) ujwt.getHeader()).getKeyID();
        assertThat(kid).isNotBlank();

        Algorithm alg = ujwt.getHeader().getAlgorithm();

        // validate by fetching keys
        MvcResult res = this.mockMvc
                .perform(get(JWKS_URL))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

        // read as string
        String json = res.getResponse().getContentAsString();
        assertThat(json).isNotBlank();

        // parse
        JWKSet set = JWKSet.parse(json);
        assertThat(set).isNotNull();

        // fetch matching key
        JWK key = set.getKeyByKeyId(kid);
        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo(alg);

        // build decoder, we support only RSA
        assertThat(JWSAlgorithm.Family.RSA.contains(alg)).isTrue();
        assertDoesNotThrow(() -> {
            key.toRSAKey().toRSAPublicKey();
        });

        RSAPublicKey publicKey = key.toRSAKey().toRSAPublicKey();
        assertThat(publicKey).isNotNull();

        assertDoesNotThrow(() -> {
            NimbusJwtDecoder.withPublicKey(publicKey).build();
        });

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        // validate signature via decoder
        assertDoesNotThrow(() -> {
            jwtDecoder.decode(accessToken);
        });

    }

    /*
     * Keys endpoint
     * 
     * use well-known URI
     */
    public final static String JWKS_URL = JWKSetPublishingEndpoint.JWKS_URL;

    private final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

}
