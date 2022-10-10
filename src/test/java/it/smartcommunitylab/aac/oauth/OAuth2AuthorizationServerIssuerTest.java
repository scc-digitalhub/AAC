package it.smartcommunitylab.aac.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.Serializable;
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
public class OAuth2AuthorizationServerIssuerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

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

    /*
     * Metadata endpoint
     * 
     * use well-known URI
     */
    public final static String METADATA_URL = "/.well-known/oauth-authorization-server";

    /*
     * Claims definition
     */

    public static final Set<String> METADATA;
    public static final Set<String> REQUIRED_METADATA;

    private static final String[] REQUIRED_METADATA_VALUES = {
            "authorization_response_iss_parameter_supported"
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
