package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

import java.util.List;

public class FirstIdentityProvider extends AbstractIdentityProvider {

    /* =========================================================================
     * First SP Specific Fields
     * ========================================================================= */

    public String signingIdpSigningCertificate;
    public String signingIdpSloUrl;
    public String signingIdpAuthority;
    public String signingIdpSigningKey;

    public String signingActiveSigningCredentialId;
    public List<SigningCredential> signingCredentials;

    @Override
    public void initReamlByBoostrap(ConfigurableIdentityProvider idp, String BASE_URL, String METADATA_PATH, String SSO_PATH) {
        // Initializes URLs, providers, and Entity IDs using the base class
        initCommonIdpFields(idp, BASE_URL, METADATA_PATH, SSO_PATH);
    }
}
