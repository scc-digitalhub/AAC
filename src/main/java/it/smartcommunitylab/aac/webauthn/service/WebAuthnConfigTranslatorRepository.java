package it.smartcommunitylab.aac.webauthn.service;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsServiceConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;

public class WebAuthnConfigTranslatorRepository extends
        TranslatorProviderConfigRepository<WebAuthnIdentityProviderConfig, WebAuthnCredentialsServiceConfig> {

    public WebAuthnConfigTranslatorRepository(
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> externalRepository) {
        super(externalRepository);
        setConverter((source) -> {
            WebAuthnCredentialsServiceConfig config = new WebAuthnCredentialsServiceConfig(source.getProvider(),
                    source.getRealm());
            config.setName(source.getName());
            config.setTitleMap(source.getTitleMap());
            config.setDescriptionMap(source.getDescriptionMap());

            // we share the same configMap
            config.setConfigMap(source.getConfigMap());
            config.setRepositoryId(source.getRepositoryId());

            return config;
        });
    }

}
