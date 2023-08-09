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

package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.provider.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class InternalSubjectResolver
    extends AbstractProvider<InternalUserAccount>
    implements SubjectResolver<InternalUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String[] ATTRIBUTES = { "email" };

    private final UserAccountService<InternalUserAccount> accountService;

    private final boolean isLinkable;
    private final String repositoryId;

    public InternalSubjectResolver(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        String repositoryId,
        boolean isLinkable,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.hasText(repositoryId, "repository id is mandatory");

        this.accountService = userAccountService;

        this.isLinkable = isLinkable;
        this.repositoryId = repositoryId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    public Subject resolveByUsername(String username) {
        logger.debug("resolve by username " + username);
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            return null;
        }
        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String username) {
        // accountId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByPrincipalId(String username) {
        // principalId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByIdentityId(String username) {
        // identityId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByEmailAddress(String email) {
        if (!isLinkable) {
            return null;
        }

        logger.debug("resolve by email " + email);
        InternalUserAccount account = accountService
            .findAccountByEmail(repositoryId, email)
            .stream()
            .filter(a -> a.isEmailVerified())
            .findFirst()
            .orElse(null);

        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }
}
