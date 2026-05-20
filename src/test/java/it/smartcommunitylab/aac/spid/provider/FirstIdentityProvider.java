package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

import java.util.List;

public class FirstIdentityProvider extends AbstractIdentityProvider {

    /* =========================================================================
     * Campi Specifici del Primo SP
     * ========================================================================= */
    public String signingIdpSigningCertificate;
    public String signingIdpSloUrl;
    public String signingIdpAuthority;
    public String signingIdpSigningKey;

    public String signingActiveSigningCredentialId;
    public List<SigningCredential> signingCredentials;

    @Override
    public void initReamlByBoostrap(ConfigurableIdentityProvider idp, String BASE_URL, String METADATA_PATH, String SSO_PATH) {

        // Inizializza URL, provider ed Entity ID tramite la classe base
        initCommonIdpFields(idp, BASE_URL, METADATA_PATH, SSO_PATH);
    }
}
