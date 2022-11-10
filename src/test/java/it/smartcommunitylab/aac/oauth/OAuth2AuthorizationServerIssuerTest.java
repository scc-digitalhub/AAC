package it.smartcommunitylab.aac.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
 * OAuth 2.0 Authorization Server Issuer Identification
 * as per 
 * https://www.rfc-editor.org/rfc/rfc9207
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OAuth2AuthorizationServerIssuerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BootstrapConfig config;

    private ClientRegistration client;

    @BeforeEach
    public void setUp() {
        if (client == null) {
            client = OAuth2ConfigUtils.with(config).client();
        }

        if (client == null) {
            throw new IllegalArgumentException("missing config");
        }
    }

    @Test
    public void requiredMetadataIsAvailable() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // check that required keys are available
        REQUIRED_METADATA.forEach(k -> {
            assertThat(k).isIn(metadata.keySet());
        });
    }

    @Test
    public void metadataIssuerIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // issuer
        assertThat(metadata.get(OAUTH2_PARAM_ISSUER)).isNotNull().isInstanceOf(String.class);
        String issuer = (String) metadata.get(OAUTH2_PARAM_ISSUER);
        assertThat(issuer).isNotBlank();

        // issuer must be a valid URL
        assertDoesNotThrow(() -> {
            new URL(issuer);
        });

        // issuer as URL can not contain fragment or query params
        URL url = new URL(issuer);
        assertThat(url.getQuery()).isNull();
        assertThat(url.getRef()).isNull();

    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void authorizationResponseIssAvailable() throws Exception {
        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, client.getClientId());
        // set empty scopes to avoid fall back to predefined
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        MvcResult res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder.fromUriString(redirectedUrl);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

        // iss
        assertThat(queryParams.get(OAUTH2_PARAM_ISS)).isNotNull().isNotEmpty();
        String iss = queryParams.get(OAUTH2_PARAM_ISS).get(0);
        assertThat(iss).isNotBlank();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void authorizationResponseIssMatchesMetadata() throws Exception {
        // fetch issuer from meta
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // issuer
        assertThat(metadata.get(OAUTH2_PARAM_ISSUER)).isNotNull().isInstanceOf(String.class);
        String issuer = (String) metadata.get(OAUTH2_PARAM_ISSUER);
        assertThat(issuer).isNotBlank();

        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, client.getClientId());
        // set empty scopes to avoid fall back to predefined
        params.add(OAuth2ParameterNames.SCOPE, "");

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder.fromUriString(redirectedUrl);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectedUrl);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

        // iss
        assertThat(queryParams.get(OAUTH2_PARAM_ISS)).isNotNull().isNotEmpty();
        String iss = queryParams.get(OAUTH2_PARAM_ISS).get(0);
        assertThat(iss).isNotBlank();

        // iss matches issuer
        assertThat(iss).isEqualTo(issuer);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void idTokenIssMatchesMetadata() throws Exception {
        // fetch issuer from meta
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // issuer
        assertThat(metadata.get(OAUTH2_PARAM_ISSUER)).isNotNull().isInstanceOf(String.class);
        String issuer = (String) metadata.get(OAUTH2_PARAM_ISSUER);
        assertThat(issuer).isNotBlank();

        // authorize request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.RESPONSE_TYPE, ResponseType.CODE_IDTOKEN.toString());
        params.add(OAuth2ParameterNames.CLIENT_ID, client.getClientId());
        // ask for id token also via scope
        params.add(OAuth2ParameterNames.SCOPE, Config.SCOPE_OPENID);
        // add nonce
        String nonce = "random-secure-nonce-value";
        params.add(OidcParameterNames.NONCE, nonce);

        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(AUTHORIZE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().isOk())
                .andReturn();

        // expect a forward in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String forwardedUrl = res.getResponse().getForwardedUrl();
        assertThat(res.getResponse().getForwardedUrl()).isNotNull().startsWith(AUTHORIZED_URL);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow forward to fetch response
        req = MockMvcRequestBuilders.get(forwardedUrl).session(session);

        res = this.mockMvc
                .perform(req)
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // expect a redirect in response with query
        assertThat(res.getResponse().getContentAsString()).isBlank();

        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();

        // parse as fragment since we use hybrid flow
        assertDoesNotThrow(() -> {
            new URL(redirectedUrl);
        });

        URL url = new URL(redirectedUrl);
        String fragment = url.getRef();
        assertThat(fragment).isNotBlank();

        // parse as queryString
        assertDoesNotThrow(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            builder.query(fragment);
        });

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.query(fragment);
        MultiValueMap<String, String> queryParams = builder.build(true).getQueryParams();
        assertThat(queryParams).isNotNull();

        // id token
        assertThat(queryParams.get(OidcParameterNames.ID_TOKEN)).isNotNull().isNotEmpty();
        String idToken = queryParams.get(OidcParameterNames.ID_TOKEN).get(0);
        assertThat(idToken).isNotBlank();

        // parse JWT
        assertDoesNotThrow(() -> {
            SignedJWT.parse(idToken);
        });
        SignedJWT jwt = SignedJWT.parse(idToken);

        // check iss
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo(issuer);
    }

    /*
     * Metadata endpoint
     * 
     * use well-known URI
     */
    public final static String METADATA_URL = "/.well-known/oauth-authorization-server";
    private final static String AUTHORIZE_URL = AuthorizationEndpoint.AUTHORIZATION_URL;
    private final static String AUTHORIZED_URL = AuthorizationEndpoint.AUTHORIZED_URL;

    /*
     * Claims definition
     */
    public final static String OAUTH2_PARAM_ISSUER = "issuer";
    public final static String OAUTH2_PARAM_ISS = "iss";
    public final static String OAUTH2_METADATA_ISS = "authorization_response_iss_parameter_supported";

    public static final Set<String> METADATA;
    public static final Set<String> REQUIRED_METADATA;

    private static final String[] REQUIRED_METADATA_VALUES = {
            OAUTH2_METADATA_ISS, OAUTH2_PARAM_ISSUER
    };

    static {
        REQUIRED_METADATA = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(REQUIRED_METADATA_VALUES)));
        TreeSet<String> set = new TreeSet<>();
        set.addAll(REQUIRED_METADATA);
        METADATA = Collections.unmodifiableSortedSet(set);
    }

    private final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
}
