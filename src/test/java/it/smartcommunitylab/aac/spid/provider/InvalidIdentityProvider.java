package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

import java.util.ArrayList;
import java.util.List;

public class InvalidIdentityProvider {

    /* =========================================================================
     * Invalid Service Provider (Supporto ai Test di Integrazione)
     * ========================================================================= */

    protected String signingIdpProviderInvalid = "d098e3ef-e9b1-4d61-a288-2a04dd3d22e0";
    protected String signingIdpRealmInvalid = "spid-invalid";
    public List<SigningCredential> signingListCredentialsInvalid;
    public String signingActiveSigningCredentialIdInvalid = "id-invalid";
    public SpidIdentityProviderConfigMap configsInvalid;

    // Aggiungo un metodo per resettare lo stato tra un test e l'altro
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
