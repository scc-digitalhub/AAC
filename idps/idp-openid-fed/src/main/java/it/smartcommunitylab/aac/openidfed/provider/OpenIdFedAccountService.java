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

package it.smartcommunitylab.aac.openidfed.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractAccountService;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.oidc.model.OIDCEditableUserAccount;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class OpenIdFedAccountService
    extends AbstractAccountService<
        OIDCUserAccount,
        OIDCEditableUserAccount,
        OpenIdFedAccountServiceConfig,
        OpenIdFedIdentityProviderConfigMap
    > {

    public OpenIdFedAccountService(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        OpenIdFedAccountServiceConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_OPENIDFED, providerId, accountService, config, realm);
    }

    public OpenIdFedAccountService(
        String authority,
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        OpenIdFedAccountServiceConfig config,
        String realm
    ) {
        super(authority, providerId, accountService, config, realm);
    }
}
