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
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.identity.base.AbstractUserResolver;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class InternalUserResolver
    extends AbstractUserResolver<InternalUserIdentity, InternalUserAccount, InternalUserAuthenticatedPrincipal> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    //TODO put in configMap (settings?)
    //TODO make field customizable
    private Set<String> resolvables = Set.of(BasicAttributesSet.EMAIL, BasicAttributesSet.USERNAME);

    public InternalUserResolver(
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<InternalUserAccount> userAccountService,
        InternalIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_INTERNAL, providerId, userEntityService, userAccountService, config, realm);
    }

    public InternalUserResolver(
        String authority,
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<InternalUserAccount> userAccountService,
        InternalIdentityProviderConfig config,
        String realm
    ) {
        this(
            authority,
            providerId,
            userEntityService,
            userAccountService,
            config.getRepositoryId(),
            config.isLinkable(),
            realm
        );
        Assert.notNull(config, "provider config is mandatory");
    }

    public InternalUserResolver(
        String authority,
        String providerId,
        UserEntityService userEntityService,
        UserAccountService<InternalUserAccount> userAccountService,
        String repositoryId,
        boolean isLinkable,
        String realm
    ) {
        super(authority, providerId, userEntityService, userAccountService, repositoryId, realm);
        this.setResolve(isLinkable);
    }

    @Transactional(readOnly = true)
    public User resolveBySubjectId(String subjectId) {
        logger.debug("resolve by subjectId {}", String.valueOf(subjectId));
        InternalUserAccount account = accountService.findAccountById(repositoryId, subjectId);
        if (account == null) {
            return null;
        }

        return fetchUser(account.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public User resolveByPrincipal(UserAuthenticatedPrincipal p) {
        if (p == null) {
            return null;
        }

        //check if principal matches
        if (p instanceof InternalUserAuthenticatedPrincipal) {
            //resolve via internal account, if matches username
            InternalUserAuthenticatedPrincipal ip = (InternalUserAuthenticatedPrincipal) p;
            String username = ip.getUsername();
            InternalUserAccount account = accountService.findAccountById(repositoryId, username);

            //account could be null (yet to be created)
            return resolveByAccount(account);
        }

        //resolve via additional attributes
        //TODO make configurable
        if (resolvables.contains(BasicAttributesSet.EMAIL)) {
            String email = p.getEmailAddress();
            if (StringUtils.hasText(email)) {
                InternalUserAccount account = accountService
                    .findAccountsByEmail(repositoryId, email)
                    .stream()
                    .filter(a -> a.isEmailVerified())
                    .findFirst()
                    .orElse(null);

                //account could be null (no match)
                return resolveByAccount(account);
            }
        }

        return null;
    }

    @Override
    public User resolveByAccount(UserAccount a) {
        if (a == null) {
            return null;
        }

        //check if account matches
        if (a instanceof InternalUserAccount) {
            //pick matching user
            return fetchUser(a.getUserId());
        }

        //do not resolve via attributes by default
        return null;
    }

    @Override
    public User resolveByIdentity(UserIdentity i) {
        if (i == null) {
            return null;
        }

        //check if account matches
        if (i instanceof InternalUserIdentity) {
            //pick matching user
            return fetchUser(i.getUserId());
        }

        //do not resolve via attributes by default
        return null;
    }
}