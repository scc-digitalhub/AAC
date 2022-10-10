package it.smartcommunitylab.aac.openid;

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
 * OpenID Connect Session Management 1.0 
 * as per 
 * https://openid.net/specs/openid-connect-session-1_0.html
 */

@SpringBootTest
@AutoConfigureMockMvc
public class OIDCSessionManagementTest {

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
    public final static String METADATA_URL = "/.well-known/openid-configuration";

    /*
     * Claims definition
     */

    public static final Set<String> METADATA;
    public static final Set<String> REQUIRED_METADATA;

    private static final String[] REQUIRED_METADATA_VALUES = {
            "check_session_iframe"
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
