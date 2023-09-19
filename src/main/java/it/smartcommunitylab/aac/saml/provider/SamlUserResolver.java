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
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.provider.UserResolver;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class SamlUserResolver extends AbstractProvider<User> implements UserResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<SamlUserAccount> accountService;
    private final SamlIdentityProviderConfig config;

    private final String repositoryId;

    //TODO put in configMap
    //TODO make field customizable
    private Set<String> resolvables = Set.of(BasicAttributesSet.EMAIL, BasicAttributesSet.USERNAME);

    public SamlUserResolver(
        String providerId,
        UserAccountService<SamlUserAccount> userAccountService,
        SamlIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_SAML, providerId, userAccountService, config, realm);
    }

    public SamlUserResolver(
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

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_SUBJECT;
    // }

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

    public Subject resolveByAccountId(String accountId) {
        // accountId is subjectId
        return resolveBySubjectId(accountId);
    }

    public Subject resolveByPrincipalId(String principalId) {
        // principalId is subjectId
        return resolveBySubjectId(principalId);
    }

    public Subject resolveByIdentityId(String identityId) {
        // identityId is sub
        return resolveBySubjectId(identityId);
    }

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

    @Transactional(readOnly = true)
    public Subject resolveByEmailAddress(String email) {
        if (!config.isLinkable()) {
            return null;
        }

        logger.debug("resolve by email {}", String.valueOf(email));
        SamlUserAccount account = accountService
            .findAccountsByEmail(repositoryId, email)
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

    @Override
    @Transactional(readOnly = true)
    public User resolveByPrincipal(UserAuthenticatedPrincipal p) {
        if (p == null) {
            return null;
        }

        //check if principal matches
        if (p instanceof SamlUserAuthenticatedPrincipal) {
            //resolve via saml account, if matches subjectId
            SamlUserAuthenticatedPrincipal sp = (SamlUserAuthenticatedPrincipal) p;
            String subjectId = sp.getSubjectId();
            SamlUserAccount account = accountService.findAccountById(repositoryId, subjectId);

            //account could be null (yet to be created)
            return resolveByAccount(account);
        }

        //resolve via additional attributes
        //TODO make configurable
        if (resolvables.contains(BasicAttributesSet.EMAIL)) {
            String email = p.getEmailAddress();
            if (StringUtils.hasText(email)) {
                SamlUserAccount account = accountService
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
        if (a instanceof SamlUserAccount) {
            return fetchUser(((SamlUserAccount) a).getUserId());
        }

        return null;
    }

    private User fetchUser(String userId) {
        return null;
    }
}
