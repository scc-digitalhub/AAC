package it.smartcommunitylab.aac.spid.setup;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Setup class for SPID Identity Provider (IdP) configuration and backend testing.
 * Inherits all standard SPID cryptography and templates from BaseSpidTest.
 */
public abstract class AbstractSpidIdentityProviderTest extends BaseSpidTest {

    /* =========================================================================
     * Identity Provider State & Configuration Objects (Backend tests)
     * ========================================================================= */

    protected String signingIdpSloUrl;
    protected String signingIdpAuthority;
    protected String signingIdpSigningKey;

    /** The specific SPID attributes requested during authentication (e.g., name, fiscalNumber). */
    protected Set<SpidAttribute> signingSetSpidAttributes;

    protected String signingActiveSigningCredentialId;
    protected List<SigningCredential> signingCredentials;

    /* =========================================================================
     * Invalid Service Provider
     * ========================================================================= */

    protected String signingIdpProviderInvalid = "d098e3ef-e9b1-4d61-a288-2a04dd3d22e0";
    protected String signingIdpRealmInvalid = "spid-invalid";
    protected List<SigningCredential> signingListCredentialsInvalid;
    protected String signingActiveSigningCredentialIdInvalid = "id-invalid";
    protected SpidIdentityProviderConfigMap configsInvalid;

    protected void initConfigsInvalidServiceProvider(){
        configsInvalid = new SpidIdentityProviderConfigMap();
        signingListCredentialsInvalid = new ArrayList<>();
    }

    protected SpidIdentityProviderConfig createInvalidSpidIdentityProvider(SpidIdentityProviderConfigMap spidIdentityProviderConfigMapInvalid){
        return new SpidIdentityProviderConfig(
            new ConfigurableIdentityProvider(signingIdpAuthority, signingIdpProviderInvalid, signingIdpRealmInvalid),
            null,
            spidIdentityProviderConfigMapInvalid); // Specific Configuration Credentials by Test runtime
    }
}
