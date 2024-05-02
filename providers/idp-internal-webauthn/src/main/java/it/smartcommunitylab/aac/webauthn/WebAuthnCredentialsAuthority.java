/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.webauthn;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.credentials.base.AbstractCredentialsAuthority;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnEditableUserCredential;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsService;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsServiceConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnJpaUserCredentialsService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRegistrationRpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * WebAuthn service depends on webauthn identity provider
 *
 * every idp will expose a matching service with the same configuration for credentials handling
 */
@Service
public class WebAuthnCredentialsAuthority
    extends AbstractCredentialsAuthority<
        WebAuthnCredentialsService,
        WebAuthnUserCredential,
        WebAuthnEditableUserCredential,
        WebAuthnCredentialsServiceConfig,
        WebAuthnIdentityProviderConfigMap
    > {

    public static final String AUTHORITY_URL = "/auth/webauthn/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // key repository
    private final WebAuthnJpaUserCredentialsService credentialsService;

    // shared service
    private final WebAuthnRegistrationRpService rpService;
    private UserEntityService userService;
    private ResourceEntityService resourceService;

    public WebAuthnCredentialsAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        WebAuthnJpaUserCredentialsService credentialsService,
        WebAuthnRegistrationRpService rpService,
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, new WebAuthnConfigTranslatorRepository(registrationRepository));
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(rpService, "webauthn rp service is mandatory");

        this.accountService = userAccountService;
        this.credentialsService = credentialsService;
        this.rpService = rpService;
    }

    // @Autowired
    // public void setConfigProvider(WebAuthnCredentialsConfigurationProvider configProvider) {
    //     Assert.notNull(configProvider, "config provider is mandatory");
    //     this.configProvider = configProvider;
    // }

    @Autowired
    public void setUserService(UserEntityService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public WebAuthnCredentialsService buildProvider(WebAuthnCredentialsServiceConfig config) {
        WebAuthnCredentialsService service = new WebAuthnCredentialsService(
            config.getProvider(),
            credentialsService,
            accountService,
            rpService,
            config,
            config.getRealm()
        );

        service.setUserService(userService);
        service.setResourceService(resourceService);

        return service;
    }

    // @Override
    // public WebAuthnCredentialsServiceConfig registerProvider(ConfigurableProviderImpl cp) {
    //     throw new IllegalArgumentException("direct registration not supported");
    // }

    static class WebAuthnConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<WebAuthnIdentityProviderConfig, WebAuthnCredentialsServiceConfig> {

        public WebAuthnConfigTranslatorRepository(
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> externalRepository
        ) {
            super(externalRepository);
            setConverter(source -> {
                WebAuthnCredentialsServiceConfig config = new WebAuthnCredentialsServiceConfig(
                    source.getProvider(),
                    source.getRealm()
                );
                config.setName(source.getName());
                config.setTitleMap(source.getTitleMap());
                config.setDescriptionMap(source.getDescriptionMap());

                // we share the same configMap
                config.setConfigMap(source.getConfigMap());
                config.setVersion(source.getVersion());

                // build new settingsMap
                CredentialsServiceSettingsMap settingsMap = new CredentialsServiceSettingsMap();
                settingsMap.setRepositoryId(source.getRepositoryId());
                config.setSettingsMap(settingsMap);

                return config;
            });
        }
    }
}
