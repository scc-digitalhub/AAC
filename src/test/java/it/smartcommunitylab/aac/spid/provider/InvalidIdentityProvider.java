package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Test utility class designed to simulate and inject invalid cryptographic configurations into the SPID Identity Provider setup.
 * It provides mechanisms to intentionally mismatch private keys and certificates (or use invalid credential IDs) to verify the system's defensive validation logic.
 */
public class InvalidIdentityProvider {

    /* =========================================================================
     * Invalid Service Provider (Integration Test Support)
     * ========================================================================= */

    public final String signingActiveSigningCredentialIdInvalid = "id-invalid";

    public List<SigningCredential> signingListCredentialsInvalid;
    public SpidIdentityProviderConfigMap configsInvalid;

    public String signingIdpSigningKeyByProviderId;
    public String signingIdpSigningCertificateByProviderId;
    public List<SigningCredential> signingListCredentialsByProviderId;

    // Adding a method to reset the state between tests
    public void reset(SpidIdentityProviderConfig byProviderId) {
        this.configsInvalid = new SpidIdentityProviderConfigMap();
        this.signingListCredentialsInvalid = new ArrayList<>();

        this.signingListCredentialsByProviderId = byProviderId.getConfigMap().getSigningCredentials();
        this.signingIdpSigningKeyByProviderId = byProviderId.getConfigMap().getSigningKey();
        this.signingIdpSigningCertificateByProviderId = byProviderId.getConfigMap().getSigningCertificate();
    }

    public void createInvalidSpidIdentityProvider(SpidIdentityProviderConfigMap spidIdentityProviderConfigMapInvalid){
        new SpidIdentityProviderConfig(
            new ConfigurableIdentityProvider("spid", "34a83a02-45f5-4112-b783-4085535c617a", "spid-invalid"),
            null,
            spidIdentityProviderConfigMapInvalid);
    }
}
