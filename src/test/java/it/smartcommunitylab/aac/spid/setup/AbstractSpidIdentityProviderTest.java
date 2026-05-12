package it.smartcommunitylab.aac.spid.setup;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.SigningCredential;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

    // Identity Provider Error Configuration Credentials
    protected String expectedMessageEmpty = "CRITICAL: Missing SPID signing credentials. The Service Provider cannot establish a Circle of Trust with IdPs as required by AgID technical regulations (Binding HTTP-POST/Redirect).";
    protected String expectedMessageMissingKeyStandalone = "CRITICAL: Key missing in standalone credential.";
    protected String expectedMessageMissingKeyCredentials = "CRITICAL: Key missing in list (ID: " + signingActiveSigningCredentialIdInvalid + ").";
    protected String expectedMessageNotFoundIdMaching = "CRITICAL: Not found credential matching active ID '" + signingActiveSigningCredentialIdInvalid + "' for signing AuthRequests.";
    protected String expectedMessageKeyAndCertificateMismatchStandalone = "CRITICAL: Mismatch the Private Key or Certificate required for signing AuthRequests.";
    protected String expectedMessagetKeyAndCertificateMismatchCredentials = "CRITICAL: Key and Certificate mismatch in ID '" + signingActiveSigningCredentialIdInvalid + "' for signing AuthRequests.";
    protected String expectedMessagetDuplicateCertificates = "CRITICAL: Duplicate certificates found in the METADATA_EXPOSURE list!";

    protected void initConfigsInvalidServiceProvider(){
        configsInvalid = new SpidIdentityProviderConfigMap();
        signingListCredentialsInvalid = new ArrayList<>();
    }

    protected IllegalArgumentException createInvalidSpidIdentityProvider(SpidIdentityProviderConfigMap spidIdentityProviderConfigMapInvalid){
        return assertThrows(
            IllegalArgumentException.class,
            () -> {
                SpidIdentityProviderConfig spidIdentityProviderConfigInvalid = new SpidIdentityProviderConfig(
                    new ConfigurableIdentityProvider(signingIdpAuthority, signingIdpProviderInvalid, signingIdpRealmInvalid),
                    null,
                    spidIdentityProviderConfigMapInvalid); // Specific Configuration Credentials by Test runtime
            }
        );
    }
}
