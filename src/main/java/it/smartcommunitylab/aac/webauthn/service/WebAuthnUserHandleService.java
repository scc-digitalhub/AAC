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

package it.smartcommunitylab.aac.webauthn.service;

import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class WebAuthnUserHandleService {

    private final UserAccountService<InternalUserAccount> userAccountService;

    public WebAuthnUserHandleService(UserAccountService<InternalUserAccount> userAccountService) {
        Assert.notNull(userAccountService, "account service is mandatory");

        this.userAccountService = userAccountService;
    }

    public String getUserHandleForUsername(String repositoryId, String username) {
        InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }

        // use uuid as userHandle
        if (!StringUtils.hasText(account.getUuid())) {
            return null;
        }

        return account.getUuid();
    }

    public String getUsernameForUserHandle(String repositoryId, String userHandle) {
        // userHandle is uuid
        InternalUserAccount account = userAccountService.findAccountByUuid(userHandle);
        if (account == null) {
            return null;
        }

        if (!repositoryId.equals(account.getRepositoryId())) {
            return null;
        }

        return account.getUsername();
    }
}
