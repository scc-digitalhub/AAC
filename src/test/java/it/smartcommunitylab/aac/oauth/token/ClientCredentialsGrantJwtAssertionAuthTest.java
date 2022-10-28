package it.smartcommunitylab.aac.oauth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Serializable;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.oauth.endpoint.TokenEndpoint;

/*
 * OAuth 2.0 Client Credentials
 * as per RFC6749
 * 
 * https://www.rfc-editor.org/rfc/rfc6749#section-4.4
 * 
 * authentication as per OpenID connect core 
 * https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
 * 
 * jwt assertion as per JWT for OAuth2.0 RFC7523
 * 
 * https://www.rfc-editor.org/rfc/rfc7523#section-2.2
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientCredentialsGrantJwtAssertionAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BootstrapConfig config;

    @Value("${application.url}")
    private String applicationURL;

    private String clientId;
    private String clientSecret;
    private String clientJwks;

    @BeforeEach
    public void setUp() {
        if (config == null) {
            throw new IllegalArgumentException("missing config");
        }

        if (clientId == null || clientSecret == null || clientJwks == null) {
            RealmConfig rc = config.getRealms().iterator().next();
            if (rc == null || rc.getClientApps() == null) {
                throw new IllegalArgumentException("missing config");
            }

            ClientApp client = rc.getClientApps().iterator().next();
            clientId = client.getClientId();
            clientSecret = (String) client.getConfiguration().get("clientSecret");
            clientJwks = (String) client.getConfiguration().get("jwks");
        }

        if (clientId == null || clientSecret == null || clientJwks == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

    @Test
    public void privateKeyJwtTest() throws Exception {
        // parse keys
        JWK jwk = loadKeys(clientJwks);

        // build signer for RSA
        RSASSASigner signer = new RSASSASigner(jwk.toRSAKey());

        // build assertion
        SignedJWT assertion = buildClientAssertion(applicationURL, clientId, signer, JWSAlgorithm.RS256,
                jwk.getKeyID());

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // expire is set and valid
        assertThat(response.get(OAuth2ParameterNames.EXPIRES_IN)).isNotNull().isInstanceOf(Integer.class);
        Integer expiresIn = (Integer) response.get(OAuth2ParameterNames.EXPIRES_IN);
        assertTrue(expiresIn > 0);
    }

    @Test
    public void clientSecretJwtTest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build assertion
        SignedJWT assertion = buildClientAssertion(applicationURL, clientId, signer, JWSAlgorithm.HS256, clientId);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a valid json in response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();

        // type bearer
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isNotNull().isInstanceOf(String.class);
        assertThat(response.get(OAuth2ParameterNames.TOKEN_TYPE)).isEqualTo("bearer");

        // access token
        assertThat(response.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull().isInstanceOf(String.class);
        String accessToken = (String) response.get(OAuth2ParameterNames.ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();

        // expire is set and valid
        assertThat(response.get(OAuth2ParameterNames.EXPIRES_IN)).isNotNull().isInstanceOf(Integer.class);
        Integer expiresIn = (Integer) response.get(OAuth2ParameterNames.EXPIRES_IN);
        assertTrue(expiresIn > 0);
    }

    @Test
    public void clientSecretJwtMissingClientIdTest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build valid assertion
        SignedJWT assertion = buildClientAssertion(applicationURL, clientId, signer, JWSAlgorithm.HS256, clientId);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void clientSecretJwtWrongSecretTest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner("32charssecret321sdfgtreacvgfdasx");

        // build valid assertion with wrong signature
        SignedJWT assertion = buildClientAssertion(applicationURL, clientId, signer, JWSAlgorithm.HS256, clientId);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void clientSecretJwtInvalidTimeTest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build invalid assertion with correct signature
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(clientId)
                .build();

        // build claims with time in the past
        Instant issuedAt = Instant.now().minus(Duration.ofDays(1));
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(Collections.singletonList(applicationURL))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT assertion = buildClientAssertion(clientId, signer, header, claims);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void clientSecretJwtInvalidAudienceTest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build invalid assertion with correct signature
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(clientId)
                .build();

        // build claims with invalid audience
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                // use clientId as audience
                .audience(Collections.singletonList(clientId))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT assertion = buildClientAssertion(clientId, signer, header, claims);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void clientSecretJwtInvalidIssuerTest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build invalid assertion with correct signature
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(clientId)
                .build();

        // build claims with time in the past
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                // use application as issuer
                .issuer(applicationURL)
                .subject(clientId)
                .audience(Collections.singletonList(applicationURL))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT assertion = buildClientAssertion(clientId, signer, header, claims);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void clientSecretJwtInvalidSubjectTest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build invalid assertion with correct signature
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(clientId)
                .build();

        // build claims with time in the past
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                // use another subject
                .subject("subject")
                .audience(Collections.singletonList(applicationURL))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT assertion = buildClientAssertion(clientId, signer, header, claims);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    @Test
    public void clientSecretJwtInvalidJTITest() throws Exception {
        // build signer for HMAC SHA-256
        MACSigner signer = new MACSigner(clientSecret);

        // build invalid assertion with correct signature
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(clientId)
                .build();

        // build claims with time in the past
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(Collections.singletonList(applicationURL))
                // null id
                // .jwtID()
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT assertion = buildClientAssertion(clientId, signer, header, claims);

        // token request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE, JWT_ASSERTION_TYPE);
        params.add(OAuth2ParameterNames.CLIENT_ASSERTION, assertion.serialize());
        // add client id because AAC requires it for security reasons
        params.add(OAuth2ParameterNames.CLIENT_ID, clientId);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // expect a 401 with an error
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        Map<String, Serializable> response = mapper.readValue(res.getResponse().getContentAsString(), typeRef);
        assertThat(response).isNotEmpty();
        assertThat(response.get("error")).isEqualTo("unauthorized");
    }

    public static SignedJWT buildClientAssertion(String clientId, JWSSigner signer, JWSHeader header,
            JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);

            return jwt;
        } catch (JOSEException e) {
            throw new JwtEncodingException("error encoding the jwt with the provided key");
        }
    }

    public static SignedJWT buildClientAssertion(String applicationURL, String clientId, JWSSigner signer,
            JWSAlgorithm alg, String keyId) {
        JWSHeader header = new JWSHeader.Builder(alg)
                .keyID(keyId)
                .build();

        // build claims
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(DEFAULT_DURATION));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(Collections.singletonList(applicationURL))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        return buildClientAssertion(clientId, signer, header, claims);
    }

    public static JWK loadKeys(String jwks) throws ParseException {
        JWKSet set = JWKSet.parse(jwks);
        if (set.getKeys().isEmpty()) {
            throw new IllegalArgumentException("missing keys");
        }

        JWK jwk = set.getKeys().stream().filter(k -> k.getKeyType() == KeyType.RSA).findFirst().orElse(null);
        if (jwk == null) {
            throw new IllegalArgumentException("missing keys");
        }
        return jwk;
    }

    private static final int DEFAULT_DURATION = 180;
    public static final String JWT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final static String TOKEN_URL = TokenEndpoint.TOKEN_URL;
    private final TypeReference<Map<String, Serializable>> typeRef = new TypeReference<Map<String, Serializable>>() {
    };
}
