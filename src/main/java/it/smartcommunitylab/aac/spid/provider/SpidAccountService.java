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

package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.base.AbstractAccountService;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.saml.model.SamlEditableUserAccount;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SpidAccountService
    extends AbstractAccountService<SamlUserAccount, SamlEditableUserAccount, SpidAccountServiceConfig, SpidIdentityProviderConfigMap> {

    public SpidAccountService(
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        SpidAccountServiceConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_SPID, providerId, accountService, config, realm);
    }

    public SpidAccountService(
        String authority,
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        SpidAccountServiceConfig config,
        String realm
    ) {
        super(authority, providerId, accountService, config, realm);
    }

    @Override
    public SamlEditableUserAccount getEditableAccount(String userId, String subject) throws NoSuchUserException {
        SamlUserAccount account = findAccount(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }
        return toEditableAccount(account);
    }

    public SamlEditableUserAccount toEditableAccount(SamlUserAccount account) {
        SamlEditableUserAccount ea = new SamlEditableUserAccount(
            getAuthority(),
            getProvider(),
            getRealm(),
            account.getUserId(),
            account.getUuid()
        );
        ea.setSubjectId(account.getSubjectId());
        ea.setUsername(account.getUsername());

        ea.setCreateDate(account.getCreateDate());
        ea.setModifiedDate(account.getModifiedDate());

        ea.setEmail(account.getEmail());
        ea.setName(account.getName());
        ea.setSurname(account.getSurname());

        return ea;
    }
}
