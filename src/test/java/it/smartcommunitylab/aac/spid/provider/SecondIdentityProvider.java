package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

public class SecondIdentityProvider extends AbstractIdentityProvider {

    /* =========================================================================
     * Second SP Specific Fields
     * ========================================================================= */

    public Boolean signingUseAssertionConsumerServiceUrl;
    public Integer signingAttributeConsumingServiceIndex;

    @Override
    public void initReamlByBoostrap(ConfigurableIdentityProvider idp, String BASE_URL, String METADATA_PATH, String SSO_PATH) {
        // Initializes URLs, providers, and Entity IDs using the base class
        initCommonIdpFields(idp, BASE_URL, METADATA_PATH, SSO_PATH);

        // Specific logic for the second IDP configuration
        SpidIdentityProviderConfigMap configmapSecond = new SpidIdentityProviderConfigMap();
        configmapSecond.setConfiguration(idp.getConfiguration());

        this.signingSetSpidAttributes = configmapSecond.getSpidAttributes(); // Inherited from AbstractIdentityProvider
        this.signingUseAssertionConsumerServiceUrl = configmapSecond.getUseAssertionConsumerServiceUrl();
        this.signingAttributeConsumingServiceIndex = configmapSecond.getAttributeConsumingServiceIndex();
    }
}
