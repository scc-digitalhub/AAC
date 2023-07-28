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
import it.smartcommunitylab.aac.base.model.AbstractEditableAccount;
import it.smartcommunitylab.aac.base.provider.AbstractAccountService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.model.OIDCEditableUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class OIDCAccountService
    extends AbstractAccountService<OIDCUserAccount, AbstractEditableAccount, OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig> {

    public OIDCAccountService(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        OIDCAccountServiceConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, config, realm);
    }

    public OIDCAccountService(
        String authority,
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        OIDCAccountServiceConfig config,
        String realm
    ) {
        super(authority, providerId, accountService, config, realm);
    }

    @Override
    public OIDCEditableUserAccount getEditableAccount(String userId, String subject) throws NoSuchUserException {
        OIDCUserAccount account = findAccount(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("user-mismatch");
        }

        return toEditableAccount(account);
    }

    private OIDCEditableUserAccount toEditableAccount(OIDCUserAccount account) {
        // build editable model
        OIDCEditableUserAccount ea = new OIDCEditableUserAccount(
            getAuthority(),
            getProvider(),
            getRealm(),
            account.getUserId(),
            account.getUuid()
        );
        ea.setSubject(account.getSubject());
        ea.setUsername(account.getUsername());

        ea.setCreateDate(account.getCreateDate());
        ea.setModifiedDate(account.getModifiedDate());

        ea.setEmail(account.getEmail());
        ea.setName(account.getName());
        ea.setGivenName(account.getGivenName());
        ea.setFamilyName(account.getFamilyName());
        ea.setLang(account.getLang());

        return ea;
    }
}
