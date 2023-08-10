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
import it.smartcommunitylab.aac.accounts.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderAuthority;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityConfigurationProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityFilterProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProvider;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnLoginRpService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class WebAuthnIdentityAuthority
    extends AbstractIdentityProviderAuthority<WebAuthnIdentityProvider, InternalUserIdentity, WebAuthnIdentityProviderConfigMap, WebAuthnIdentityProviderConfig> {

    public static final String AUTHORITY_URL = "/auth/webauthn/";

    // internal account service
    private final UserAccountService<InternalUserAccount> accountService;

    // key repository
    private final WebAuthnUserCredentialsService credentialsService;

    // filter provider
    private final WebAuthnIdentityFilterProvider filterProvider;

    // services
    private ResourceEntityService resourceService;

    public WebAuthnIdentityAuthority(
        UserAccountService<InternalUserAccount> userAccountService,
        WebAuthnUserCredentialsService credentialsService,
        WebAuthnLoginRpService rpService,
        WebAuthnAssertionRequestStore requestStore,
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, registrationRepository);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(rpService, "webauthn rp service is mandatory");
        Assert.notNull(requestStore, "webauthn request store is mandatory");

        this.accountService = userAccountService;
        this.credentialsService = credentialsService;

        // build filter provider
        this.filterProvider = new WebAuthnIdentityFilterProvider(rpService, registrationRepository, requestStore);
    }

    @Autowired
    public void setConfigProvider(WebAuthnIdentityConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Autowired
    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public WebAuthnIdentityProvider buildProvider(WebAuthnIdentityProviderConfig config) {
        WebAuthnIdentityProvider idp = new WebAuthnIdentityProvider(
            config.getProvider(),
            accountService,
            credentialsService,
            config,
            config.getRealm()
        );

        idp.setResourceService(resourceService);

        return idp;
    }

    @Override
    public WebAuthnIdentityFilterProvider getFilterProvider() {
        return filterProvider;
    }
}
