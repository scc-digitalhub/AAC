package it.smartcommunitylab.aac.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.openid.OIDCProviderMetadataTest;
import it.smartcommunitylab.aac.openid.OIDCRpInitiatedLogoutTest;
import it.smartcommunitylab.aac.openid.OIDCSessionManagementTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
 * OAuth 2.0 Authorization Server Metadata
 * as per RFC8414
 * 
 * https://www.rfc-editor.org/rfc/rfc8414
 */

@SpringBootTest
@AutoConfigureMockMvc
public class OAuth2ServerMetadataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void metadataIsAvailable() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

        // content type is json
        assertEquals(res.getResponse().getContentType(), MediaType.APPLICATION_JSON_VALUE);

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);
        assertNotNull(metadata);
    }

    @Test
    public void metadataIsWellFormed() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // every claim is either string, boolean or string array
        metadata.entrySet().forEach(e -> {
            assertThat(e.getValue()).isInstanceOfAny(String.class, Boolean.class, List.class);

            if (e instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                List<Object> l = (List<Object>) e;
                assertThat(l).allMatch(v -> (v instanceof String));
            }
        });

    }

    @Test
    public void allMetadataIsRegistered() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // check that all keys are registered
        Set<String> validKeys = new HashSet<>();
        validKeys.addAll(METADATA);
        validKeys.addAll(OIDCProviderMetadataTest.METADATA);
        validKeys.addAll(OIDCSessionManagementTest.METADATA);
        validKeys.addAll(OIDCRpInitiatedLogoutTest.METADATA);
        validKeys.addAll(OAuth2AuthorizationServerIssuerTest.METADATA);

        metadata.keySet().forEach(k -> {
            assertThat(k).isIn(validKeys);
        });

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
    public void issuerIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // issuer is present
        assertThat(metadata.keySet()).contains("issuer");

        // issuer is a single strig
        assertThat(metadata.get("issuer")).isInstanceOf(String.class);

        // validate as URI
        assertDoesNotThrow(() -> {
            String issuer = (String) metadata.get("issuer");
            URI.create(issuer);

        });

        String issuer = (String) metadata.get("issuer");
        URI uri = URI.create(issuer);

        // no fragment or query allowed
        assertThat(uri.getQuery()).isBlank();
        assertThat(uri.getFragment()).isBlank();
    }

    /*
     * Metadata endpoint
     * 
     * https://www.rfc-editor.org/rfc/rfc8414#section-3
     */
    public final static String METADATA_URL = "/.well-known/oauth-authorization-server";

    /*
     * Claims definition
     */
    public static final Set<String> METADATA;
    public static final Set<String> REQUIRED_METADATA;
    public static final Set<String> RECOMMENDED_METADATA;
    public static final Set<String> OPTIONAL_METADATA;

    private static final String[] REQUIRED_METADATA_VALUES = {
            "issuer",
            "authorization_endpoint",
            "token_endpoint",
            "response_types_supported",

    };
    private static final String[] RECOMMENDED_METADATA_VALUES = {
            "scopes_supported",
            "claims_supported"

    };

    private static final String[] OPTIONAL_METADATA_VALUES = {
            "jwks_uri",
            "registration_endpoint",
            "response_modes_supported",
            "grant_types_supported",
            "token_endpoint_auth_methods_supported",
            "token_endpoint_auth_signing_alg_values_supported",
            "service_documentation",
            "ui_locales_supported",
            "op_policy_uri",
            "op_tos_uri",
            "revocation_endpoint",
            "revocation_endpoint_auth_methods_supported",
            "revocation_endpoint_auth_signing_alg_values_supported",
            "introspection_endpoint",
            "introspection_endpoint_auth_methods_supported",
            "introspection_endpoint_auth_signing_alg_values_supported",
            "code_challenge_methods_supported"
    };

    static {
        REQUIRED_METADATA = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(REQUIRED_METADATA_VALUES)));
        RECOMMENDED_METADATA = Collections
                .unmodifiableSortedSet(new TreeSet<>(Arrays.asList(RECOMMENDED_METADATA_VALUES)));
        OPTIONAL_METADATA = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(OPTIONAL_METADATA_VALUES)));
        TreeSet<String> set = new TreeSet<>();
        set.addAll(REQUIRED_METADATA);
        set.addAll(RECOMMENDED_METADATA);
        set.addAll(OPTIONAL_METADATA);
        METADATA = Collections.unmodifiableSortedSet(set);
    }

    private final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
}
