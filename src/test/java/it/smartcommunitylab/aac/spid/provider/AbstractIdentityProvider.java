package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;

import java.util.Base64;
import java.util.Set;

public abstract class AbstractIdentityProvider {

    /* =========================================================================
     * SERVICE PROVIDER DEFAULT FIELDS
     * ========================================================================= */

    public String signingIdpProvider;
    public String signingIdpMetadataUrl;
    public String signingIdpSsoUrl;
    public String signingIdpEntityId;
    public Set<SpidAttribute> signingSetSpidAttributes;

    public String registrationIdRedirect;
    public String registrationIdPost;

    protected void initCommonIdpFields(ConfigurableIdentityProvider idp, String BASE_URL, String METADATA_PATH, String SSO_PATH) {
        this.signingIdpProvider = idp.getProvider();
        this.signingIdpMetadataUrl = BASE_URL + METADATA_PATH + encodeRegistrationId(this.signingIdpProvider);
        this.signingIdpSsoUrl = BASE_URL + SSO_PATH + encodeRegistrationId(this.signingIdpProvider);
        this.signingIdpEntityId = BASE_URL + METADATA_PATH + encodeRegistrationId(this.signingIdpProvider);
    }

    // Compute the SPID Relying Party Registration IDs (Base64 of "providerId|entityId")
    public void initRegistrationIdBinding(String ASSERTING_PARTY_ENTITY_ID_REDIRECT, String ASSERTING_PARTY_ENTITY_ID_POST) {
        this.registrationIdRedirect = encodeRegistrationId(signingIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_REDIRECT);
        this.registrationIdPost = encodeRegistrationId(signingIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_POST);
    }

    public void initReamlByBoostrap(ConfigurableIdentityProvider idp, String BASE_URL, String METADATA_PATH, String SSO_PATH) { }

    public static String encodeRegistrationId(String regId) {
        return Base64.getUrlEncoder().encodeToString(regId.getBytes());
    }
}
