package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

public class SecondIdentityProvider extends AbstractIdentityProvider {

    /* =========================================================================
     * Campi Specifici del Secondo SP
     * ========================================================================= */
    public Boolean signingUseAssertionConsumerServiceUrl;
    public Integer signingAttributeConsumingServiceIndex;

    @Override
    public void initReamlByBoostrap(ConfigurableIdentityProvider idp, String BASE_URL, String METADATA_PATH, String SSO_PATH) {
        // Inizializza URL, provider ed Entity ID tramite la classe base
        initCommonIdpFields(idp, BASE_URL, METADATA_PATH, SSO_PATH);

        // Logica specifica per la configurazione del secondo IDP
        SpidIdentityProviderConfigMap configmapSecond = new SpidIdentityProviderConfigMap();
        configmapSecond.setConfiguration(idp.getConfiguration());

        this.signingSetSpidAttributes = configmapSecond.getSpidAttributes(); // Ereditato da AbstractIdentityProvider
        this.signingUseAssertionConsumerServiceUrl = configmapSecond.getUseAssertionConsumerServiceUrl();
        this.signingAttributeConsumingServiceIndex = configmapSecond.getAttributeConsumingServiceIndex();
    }
}
