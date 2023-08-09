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

package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractAccountProvider;
import it.smartcommunitylab.aac.accounts.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class OIDCAccountProvider extends AbstractAccountProvider<OIDCUserAccount> {

    public OIDCAccountProvider(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, repositoryId, realm);
    }

    public OIDCAccountProvider(
        String authority,
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        String repositoryId,
        String realm
    ) {
        super(authority, providerId, accountService, repositoryId, realm);
    }
}
