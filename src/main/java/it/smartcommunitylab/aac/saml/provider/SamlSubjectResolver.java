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

package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.provider.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public class SamlSubjectResolver extends AbstractProvider<SamlUserAccount> implements SubjectResolver<SamlUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<SamlUserAccount> accountService;
    private final SamlIdentityProviderConfig config;

    private final String repositoryId;

    public SamlSubjectResolver(
        String providerId,
        UserAccountService<SamlUserAccount> userAccountService,
        SamlIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_SAML, providerId, userAccountService, config, realm);
    }

    public SamlSubjectResolver(
        String authority,
        String providerId,
        UserAccountService<SamlUserAccount> userAccountService,
        SamlIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.accountService = userAccountService;
        this.config = config;

        // repositoryId is always providerId, saml isolates data per provider
        this.repositoryId = providerId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
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
        // principalId is subjectId
        return resolveBySubjectId(principalId);
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        // identityId is sub
        return resolveBySubjectId(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByUsername(String username) {
        logger.debug("resolve by username {}", String.valueOf(username));
        SamlUserAccount account = accountService
            .findAccountByUsername(repositoryId, username)
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
