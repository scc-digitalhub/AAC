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

import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

// TODO
public class SpidSubjectResolver extends AbstractProvider<SpidUserAccount> implements SubjectResolver<SpidUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserAccountService<SpidUserAccount> accountService;
    private final SpidIdentityProviderConfig config;
    private String repositoryId;

    public SpidSubjectResolver(
        String authority,
        String providerId,
        UserAccountService<SpidUserAccount> userAccountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.accountService = userAccountService;
        this.config = config;
        this.repositoryId = providerId;
    }

    @Override
    public Subject resolveByAccountId(String accountId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByPrincipalId(String principalId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByUsername(String accountId) {
        // TODO
        return null;
    }

    @Override
    public Subject resolveByEmailAddress(String accountId) {
        // TODO
        return null;
    }
}
