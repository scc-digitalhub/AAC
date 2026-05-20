package it.smartcommunitylab.aac.spid.setup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Base setup class for all SPID-related tests.
 * Centralizes OpenSAML infrastructure, shared cryptographic credentials,
 * AgID-compliant metadata templates, and mock SAML payloads.
 */
public abstract class BaseSpidTest {

    /* =========================================================================
     * Shared Base Endpoints and Paths Configuration
     * ========================================================================= */

    protected final String BASE_URL = "http://localhost:8080";
    protected final String METADATA_PATH = "/auth/spid/metadata/";
    protected final String SSO_PATH = "/auth/spid/sso/";
    protected final String SLO_PATH = "/auth/spid/slo/";

    /* =========================================================================
     * PEM Certificate standard delimiters
     * ========================================================================= */

    protected final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    protected final String END_CERT = "-----END CERTIFICATE-----";

    /* =========================================================================
     * Web SSO Specific Destinations and Actions
     * ========================================================================= */

    protected final String AUTHENTICATE_PATH = "/auth/spid/authenticate/";
    protected final String USER_DESTINATION_URL = BASE_URL + "/console/user";
    protected final String LOGIN_DESTINATION_URL = BASE_URL + "/-/spid-test/login";

    /* =========================================================================
     * SETUP TEST
     * ========================================================================= */

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private Filter springSecurityFilterChain;
    protected MockMvc mockMvc;

    protected void initMockMvc(){
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.context)
            .addFilter(this.springSecurityFilterChain, "/*")
            .defaultRequest(get(BASE_URL))
            .build();
    }
}
