package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

import java.util.ArrayList;
import java.util.List;

public class InvalidIdentityProvider {

    /* =========================================================================
     * Invalid Service Provider (Integration Test Support)
     * ========================================================================= */

    protected String signingIdpProviderInvalid = "d098e3ef-e9b1-4d61-a288-2a04dd3d22e0";
    protected String signingIdpRealmInvalid = "spid-invalid";
    public List<SigningCredential> signingListCredentialsInvalid;
    public String signingActiveSigningCredentialIdInvalid = "id-invalid";
    public SpidIdentityProviderConfigMap configsInvalid;

    // Adding a method to reset the state between tests
    public void reset() {
        this.configsInvalid = new SpidIdentityProviderConfigMap();
        this.signingListCredentialsInvalid = new ArrayList<>();
    }

    public SpidIdentityProviderConfig createInvalidSpidIdentityProvider(String signingIdpAuthority, SpidIdentityProviderConfigMap spidIdentityProviderConfigMapInvalid){
        return new SpidIdentityProviderConfig(
            new ConfigurableIdentityProvider(signingIdpAuthority, signingIdpProviderInvalid, signingIdpRealmInvalid),
            null,
            spidIdentityProviderConfigMapInvalid);
    }
}
