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
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.saml.SamlKeys;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class SpidAccountPrincipalConverter
    extends AbstractProvider<SamlUserAccount>
    implements AccountPrincipalConverter<SamlUserAccount> {

    //    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SpidIdentityProviderConfig config;
    private final String repositoryId;

    public SpidAccountPrincipalConverter(String providerId, SpidIdentityProviderConfig config, String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        this.config = config;
        this.repositoryId = providerId;
    }

    @Override
    public SamlUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        Assert.isInstanceOf(
            SpidUserAuthenticatedPrincipal.class,
            userPrincipal,
            "principal must be an instance of saml authenticated principal"
        );
        SamlUserAccount account = new SamlUserAccount(getProvider(), getRealm(), null);

        SpidUserAuthenticatedPrincipal principal = (SpidUserAuthenticatedPrincipal) userPrincipal;

        //filter saml attributes to keep only user attributes
        Map<String, Serializable> attributes = principal
            .getAttributes()
            .entrySet()
            .stream()
            .filter(e -> !SamlKeys.SAML_ATTRIBUTES.contains(e.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        account.setRepositoryId(repositoryId);
        account.setSubjectId(principal.getSubjectId());
        account.setUserId(userId);

        account.setUsername(principal.getUsername());
        account.setName(principal.getName());
        account.setEmail(principal.getEmailAddress());
        if (StringUtils.hasText(principal.getEmailAddress())) {
            account.setEmailVerified(true);
        }
        account.setAttributes(attributes);

        account.setIssuer(principal.getIdp());
        return account;
    }
}
