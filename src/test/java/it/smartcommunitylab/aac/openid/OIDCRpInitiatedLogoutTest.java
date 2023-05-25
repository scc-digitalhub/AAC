package it.smartcommunitylab.aac.openid;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * OpenID Connect RP-Initiated Logout 1.0
 * as per 
 * https://openid.net/specs/openid-connect-rpinitiated-1_0.html
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OIDCRpInitiatedLogoutTest {

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

    @Test
    public void metadataEndpointIsValid() throws Exception {
        MvcResult res = this.mockMvc
                .perform(get(METADATA_URL))
                .andExpect(status().isOk())
                .andReturn();

        // parse as Map from JSON
        String json = res.getResponse().getContentAsString();
        Map<String, Serializable> metadata = mapper.readValue(json, typeRef);

        // issuer
        assertThat(metadata.get(OIDC_ENDSESSION_ENDPOINT)).isNotNull().isInstanceOf(String.class);
        String endpoint = (String) metadata.get(OIDC_ENDSESSION_ENDPOINT);
        assertThat(endpoint).isNotBlank();

        // entpoint must be a valid URL
        assertDoesNotThrow(() -> {
            new URL(endpoint);
        });

        // url should use https - not checked

        // endpoint as URL can not contain fragment
        URL url = new URL(endpoint);
        assertThat(url.getRef()).isNull();

    }

    /*
     * Metadata endpoint
     * 
     * use well-known URI
     */
    public final static String METADATA_URL = "/.well-known/openid-configuration";

    /*
     * Claims definition
     */

    public static final Set<String> METADATA;
    public static final Set<String> REQUIRED_METADATA;
    public final static String OIDC_ENDSESSION_ENDPOINT = "end_session_endpoint";

    private static final String[] REQUIRED_METADATA_VALUES = {
            OIDC_ENDSESSION_ENDPOINT
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
