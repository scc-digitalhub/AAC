/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.eidas.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.provider.SpidAccountService;
import it.smartcommunitylab.aac.spid.provider.SpidAccountServiceConfig;
import org.springframework.transaction.annotation.Transactional;

/*
 * Accounts are stored exactly as SPID does (shared saml account, repositoryId = providerId), hence
 * the SPID service is inherited whole: only the authority reported to the platform changes.
 *
 * NOTE: AbstractConfigurableResourceProvider checks that the service authority matches the
 * authority of its own configuration; SpidAccountService declares itself as spid, so the internal
 * service configuration is built on the spid authority (see toServiceConfig below)
 * while the service exposes eidas.
 */
@Transactional
public class EIDASAccountService extends SpidAccountService {

    public EIDASAccountService(
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        EIDASIdentityProviderConfig config,
        String realm
    ) {
        super(providerId, accountService, toServiceConfig(config), realm);
    }

    /*
     * used by EIDASAccountServiceAuthority, which receives the configuration already converted by
     * its translator repository
     */
    public EIDASAccountService(
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        SpidAccountServiceConfig config,
        String realm
    ) {
        super(providerId, accountService, config, realm);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_EIDAS;
    }

    /*
     * Same conversion performed by SpidAccountServiceConfigConverter, with the authority pinned to
     * spid (see note above). Used as a Converter by EIDASAccountServiceAuthority.
     */
    public static SpidAccountServiceConfig toServiceConfig(EIDASIdentityProviderConfig source) {
        SpidAccountServiceConfig config = new SpidAccountServiceConfig(
            SystemKeys.AUTHORITY_SPID,
            source.getProvider(),
            source.getRealm()
        );
        config.setName(source.getName());
        config.setTitleMap(source.getTitleMap());
        config.setDescriptionMap(source.getDescriptionMap());

        config.setConfigMap(source.getConfigMap());
        config.setVersion(source.getVersion());

        // build settings map with provided information
        AccountServiceSettingsMap settingsMap = new AccountServiceSettingsMap();
        settingsMap.setPersistence(source.getPersistence());
        config.setSettingsMap(settingsMap);

        return config;
    }
}
