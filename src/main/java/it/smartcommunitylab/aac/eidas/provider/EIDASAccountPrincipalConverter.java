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

package it.smartcommunitylab.aac.eidas.provider;

import it.smartcommunitylab.aac.eidas.model.EIDASAttribute;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.provider.SpidAccountPrincipalConverter;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.io.Serializable;
import java.util.Map;

/*
 * Builds the eidas account from the authenticated principal.
 *
 * The SPID converter does the work (subject, username, attributes, issuer); three fields are then
 * corrected, because the SPID choices encode assumptions that hold for a SPID identity provider and
 * not for an eIDAS node:
 *
 *  - name and surname: SPID stores the username as account name and leaves the surname empty. The
 *    eIDAS Minimum Data Set (annex to Reg. (EU) 2015/1501) always carries the current given name and
 *    the current family name, released by the node as the SPID attributes name and familyName: the
 *    account takes them from there.
 *  - emailVerified: SPID marks the email as verified whenever present, on the trust that the SPID
 *    identity provider verified it. The email is outside the eIDAS Minimum Data Set and no eIDAS
 *    rule states it is verified: the flag stays false, so the eidas account is never linked to an
 *    existing user on the strength of the email alone.
 */
public class EIDASAccountPrincipalConverter extends SpidAccountPrincipalConverter {

    public EIDASAccountPrincipalConverter(String providerId, SpidIdentityProviderConfig config, String realm) {
        super(providerId, config, realm);
    }

    @Override
    public SamlUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        SamlUserAccount account = super.convertAccount(userPrincipal, userId);

        Map<String, Serializable> attributes = account.getAttributes();
        if (attributes != null) {
            account.setName(asString(attributes.get(EIDASAttribute.NAME.getValue())));
            account.setSurname(asString(attributes.get(EIDASAttribute.FAMILY_NAME.getValue())));
        }

        account.setEmailVerified(false);

        return account;
    }

    private static String asString(Serializable value) {
        return value != null ? value.toString() : null;
    }
}
