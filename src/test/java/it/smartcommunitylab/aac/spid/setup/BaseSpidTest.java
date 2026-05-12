package it.smartcommunitylab.aac.spid.setup;

import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
     * Service Provider (SP)
     * ========================================================================= */

    protected String signingIdpSsoUrl;
    protected String signingIdpMetadataUrl;
    protected String signingIdpEntityId;
    protected String signingIdpProvider;
    protected String signingIdpSigningCertificate;

    protected String registrationIdRedirect;
    protected String registrationIdPost;

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

    protected void initReamlByBoostrap(RealmConfig realm) {
        List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
        assertThat(idps.size()).isEqualTo(2);

        ConfigurableIdentityProvider idp1 = idps.get(0);
        assertThat(idp1.getProvider()).isNotNull();

        signingIdpProvider = idp1.getProvider();
        signingIdpMetadataUrl = BASE_URL + METADATA_PATH + encodeRegistrationId(signingIdpProvider);
        signingIdpSsoUrl = BASE_URL + SSO_PATH + encodeRegistrationId(signingIdpProvider);
        signingIdpEntityId = BASE_URL + METADATA_PATH + encodeRegistrationId(signingIdpProvider);
    }

    protected void initRegistrationIdBinding(String ASSERTING_PARTY_ENTITY_ID_REDIRECT, String ASSERTING_PARTY_ENTITY_ID_POST){
        this.registrationIdRedirect = encodeRegistrationId(signingIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_REDIRECT);
        this.registrationIdPost = encodeRegistrationId(signingIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_POST);
    }

    protected static String encodeRegistrationId(String regId) {
        return Base64.getUrlEncoder().encodeToString(regId.getBytes());
    }
}
