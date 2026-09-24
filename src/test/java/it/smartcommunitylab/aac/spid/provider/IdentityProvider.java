package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

import java.util.Base64;

/**
 * Utility component for encapsulating the Service Provider's runtime configuration state, including dynamically generated metadata and SSO endpoints.
 * It provides helper methods to compute Base64-encoded Relying Party registration IDs required for routing SAML requests to specific IdP bindings.
 */
public class IdentityProvider {

    /* =========================================================================
     * SERVICE PROVIDER DEFAULT FIELDS
     * ========================================================================= */

    public String signingIdpProvider;
    public String signingIdpMetadataUrl;
    public String signingIdpSsoUrl;
    public String signingIdpEntityId;

    public String registrationIdRedirect;
    public String registrationIdPost;

    public void initRealmByBootstrap(ConfigurableIdentityProvider idp, String BASE_URL, String METADATA_PATH, String SSO_PATH) {
        this.signingIdpProvider = idp.getProvider();
        this.signingIdpMetadataUrl = BASE_URL + METADATA_PATH + encodeBase64(this.signingIdpProvider);
        this.signingIdpSsoUrl = BASE_URL + SSO_PATH + encodeBase64(this.signingIdpProvider);
        this.signingIdpEntityId = BASE_URL + METADATA_PATH + encodeBase64(this.signingIdpProvider);
    }

    // Compute the SPID Relying Party Registration IDs (Base64 of "providerId|entityId")
    public void initRegistrationIdBinding(String ASSERTING_PARTY_ENTITY_ID_REDIRECT, String ASSERTING_PARTY_ENTITY_ID_POST) {
        this.registrationIdRedirect = encodeBase64(signingIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_REDIRECT);
        this.registrationIdPost = encodeBase64(signingIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_POST);
    }

    public static String encodeBase64(String regId) {
        return Base64.getUrlEncoder().encodeToString(regId.getBytes());
    }
}
