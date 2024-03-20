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

package it.smartcommunitylab.aac.openidfed.cie.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractAccountService;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.oidc.model.OIDCEditableUserAccount;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CieAccountService
    extends AbstractAccountService<
        OIDCUserAccount,
        OIDCEditableUserAccount,
        CieAccountServiceConfig,
        CieIdentityProviderConfigMap
    > {

    public CieAccountService(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        CieAccountServiceConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_CIE, providerId, accountService, config, realm);
    }

    public CieAccountService(
        String authority,
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        CieAccountServiceConfig config,
        String realm
    ) {
        super(authority, providerId, accountService, config, realm);
    }
}
