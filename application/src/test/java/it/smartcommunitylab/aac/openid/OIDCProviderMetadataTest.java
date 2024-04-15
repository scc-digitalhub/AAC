/*
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

package it.smartcommunitylab.aac.openid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/*
 * OpenID Connect Discovery 1.0 Provider Metadata
 * as per
 * https://openid.net/specs/openid-connect-discovery-1_0.html
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OIDCProviderMetadataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void metadataIsAvailable() throws Exception {
        MvcResult res = this.mockMvc.perform(get(METADATA_URL)).andDo(print()).andExpect(status().isOk()).andReturn();

        // content type is json
        assertEquals(res.getResponse().getContentType(), MediaType.APPLICATION_JSON_VALUE);

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);
        assertNotNull(metadata);
    }

    @Test
    public void metadataIsWellFormed() throws Exception {
        MvcResult res = this.mockMvc.perform(get(METADATA_URL)).andExpect(status().isOk()).andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // every claim is either string, boolean or string array
        metadata
            .entrySet()
            .forEach(e -> {
                assertThat(e.getValue()).isInstanceOfAny(String.class, Boolean.class, List.class);

                if (e instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) e;
                    assertThat(l).allMatch(v -> (v instanceof String));
                }
            });
    }

    @Test
    public void requiredMetadataAreAvailable() throws Exception {
        MvcResult res = this.mockMvc.perform(get(METADATA_URL)).andExpect(status().isOk()).andReturn();

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
        MvcResult res = this.mockMvc.perform(get(METADATA_URL)).andExpect(status().isOk()).andReturn();

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
            URI uri = URI.create(issuer);

            // no fragment or query allowed
            assertThat(uri.getQuery()).isBlank();
            assertThat(uri.getFragment()).isBlank();
        });
    }

    @Test
    public void endpointsAreValid() throws Exception {
        MvcResult res = this.mockMvc.perform(get(METADATA_URL)).andExpect(status().isOk()).andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // endpoint list
        Set<String> endpoints = METADATA.stream().filter(k -> k.endsWith("_endpoint")).collect(Collectors.toSet());

        endpoints.forEach(k -> {
            if (metadata.containsKey(k)) {
                // endpoint is a single strig
                assertThat(metadata.get(k)).isInstanceOf(String.class);

                // validate as URI
                assertDoesNotThrow(() -> {
                    String issuer = (String) metadata.get(k);
                    URI.create(issuer);
                });
            }
        });
    }

    /*
     * Metadata endpoint
     *
     * use well-known URI
     */
    public static final String METADATA_URL = "/.well-known/openid-configuration";

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
        "jwks_uri",
        "response_types_supported",
        "subject_types_supported",
        "id_token_signing_alg_values_supported",
    };
    private static final String[] RECOMMENDED_METADATA_VALUES = {
        "userinfo_endpoint",
        "registration_endpoint",
        "scopes_supported",
        "claims_supported",
    };

    private static final String[] OPTIONAL_METADATA_VALUES = {
        "response_modes_supported",
        "grant_types_supported",
        "acr_values_supported",
        "id_token_encryption_alg_values_supported",
        "id_token_encryption_enc_values_supported",
        "userinfo_signing_alg_values_supported",
        "userinfo_encryption_alg_values_supported",
        "userinfo_encryption_enc_values_supported",
        "request_object_signing_alg_values_supported",
        "request_object_encryption_alg_values_supported",
        "request_object_encryption_enc_values_supported",
        "token_endpoint_auth_methods_supported",
        "token_endpoint_auth_signing_alg_values_supported",
        "display_values_supported",
        "claim_types_supported",
        "service_documentation",
        "claims_locales_supported",
        "ui_locales_supported",
        "claims_parameter_supported",
        "request_parameter_supported",
        "request_uri_parameter_supported",
        "require_request_uri_registration",
        "op_policy_uri",
        "op_tos_uri",
        "revocation_endpoint",
        "revocation_endpoint_auth_methods_supported",
        "revocation_endpoint_auth_signing_alg_values_supported",
        "introspection_endpoint",
        "introspection_endpoint_auth_methods_supported",
        "introspection_endpoint_auth_signing_alg_values_supported",
        "code_challenge_methods_supported",
    };

    static {
        REQUIRED_METADATA = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(REQUIRED_METADATA_VALUES)));
        RECOMMENDED_METADATA =
            Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(RECOMMENDED_METADATA_VALUES)));
        OPTIONAL_METADATA = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(OPTIONAL_METADATA_VALUES)));
        TreeSet<String> set = new TreeSet<>();
        set.addAll(REQUIRED_METADATA);
        set.addAll(RECOMMENDED_METADATA);
        set.addAll(OPTIONAL_METADATA);
        METADATA = Collections.unmodifiableSortedSet(set);
    }

    private final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};
}
