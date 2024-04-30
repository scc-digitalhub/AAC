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
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public class SpidSubjectResolver extends AbstractProvider<SamlUserAccount> implements SubjectResolver<SamlUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserAccountService<SamlUserAccount> accountService;
    private final SpidIdentityProviderConfig config;
    private String repositoryId;

    public SpidSubjectResolver(
        String authority,
        String providerId,
        UserAccountService<SamlUserAccount> userAccountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.accountService = userAccountService;
        this.config = config;
        this.repositoryId = providerId; // TODO: non chiaro se sia necessario isolare i dati per provider, come per saml. Probabilmente sì.
    }

    @Transactional(readOnly = true)
    public Subject resolveBySubjectId(String subjectId) {
        logger.debug("resolve by subjectId {}", String.valueOf(subjectId));
        SamlUserAccount account = accountService.findAccountById(repositoryId, subjectId);
        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String accountId) {
        // accountId is subjectId
        return resolveBySubjectId(accountId);
    }

    @Override
    public Subject resolveByPrincipalId(String principalId) {
        // accountId is subjectId
        return resolveBySubjectId(principalId);
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        // accountId is subjectId
        return resolveBySubjectId(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByUsername(String username) {
        logger.debug("resolve by username {}", String.valueOf(username));
        SamlUserAccount account = accountService
            .findAccountsByUsername(repositoryId, username)
            .stream()
            .findFirst()
            .orElse(null);
        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByEmailAddress(String email) {
        if (!config.isLinkable()) {
            return null;
        }

        logger.debug("resolve by email {}", String.valueOf(email));
        SamlUserAccount account = accountService
            .findAccountsByEmail(repositoryId, email)
            .stream()
            .findFirst()
            .orElse(null);
        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }
}
