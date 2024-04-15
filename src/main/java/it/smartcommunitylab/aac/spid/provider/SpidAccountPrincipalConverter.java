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

import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public class SpidAccountPrincipalConverter
    extends AbstractProvider<SpidUserAccount>
    implements AccountPrincipalConverter<SpidUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SpidIdentityProviderConfig config;
    private final String repositoryId;

    public SpidAccountPrincipalConverter(
        String authority,
        String providerId,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        this.config = config;
        // TODO: probabilmente questo è ERRATO: non è detto che in SPID i dati siano isolati per providerId
        this.repositoryId = providerId;
    }

    @Override
    public SpidUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        Assert.isInstanceOf(
            SpidUserAuthenticatedPrincipal.class,
            userPrincipal,
            "principal must be an instance of saml authenticated principal"
        );
        SpidUserAccount account = new SpidUserAccount(getProvider(), getRealm(), null);

        SpidUserAuthenticatedPrincipal principal = (SpidUserAuthenticatedPrincipal) userPrincipal;

        account.setRepositoryId(repositoryId);
        account.setSubjectId(principal.getSubjectId());
        account.setUserId(userId);

        account.setUsername(principal.getUsername());
        account.setName(principal.getName());
        account.setEmail(principal.getEmailAddress());
        account.setSpidCode(principal.getSpidCode());
        account.setIdp(principal.getIdp());

        // TODO: check that values are non-trivial
        if (principal.getAttributes().get(SpidAttribute.FAMILY_NAME.getValue()) != null) {
            account.setSurname((String) principal.getAttributes().get(SpidAttribute.FAMILY_NAME.getValue()));
        }
        if (principal.getAttributes().get(SpidAttribute.MOBILE_PHONE.getValue()) != null) {
            account.setPhone((String) principal.getAttributes().get(SpidAttribute.MOBILE_PHONE.getValue()));
        }
        if (principal.getAttributes().get(SpidAttribute.FISCAL_NUMBER.getValue()) != null) {
            account.setFiscalNumber((String) principal.getAttributes().get(SpidAttribute.FISCAL_NUMBER.getValue()));
        }
        if (principal.getAttributes().get(SpidAttribute.IVA_CODE.getValue()) != null) {
            account.setIvaCode((String) principal.getAttributes().get(SpidAttribute.IVA_CODE.getValue()));
        }
        account.setAttributes(principal.getAttributes());

        return account;
    }
}
