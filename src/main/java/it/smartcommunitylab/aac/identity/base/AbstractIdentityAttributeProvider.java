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

package it.smartcommunitylab.aac.identity.base;

import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityAttributeProvider;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractIdentityAttributeProvider<
    P extends AbstractUserAuthenticatedPrincipal, U extends AbstractUserAccount
>
    extends AbstractProvider<UserAttributes>
    implements IdentityAttributeProvider<P, U> {

    protected AbstractIdentityAttributeProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    // @Override
    // public final String getType() {
    //     return SystemKeys.RESOURCE_ATTRIBUTES;
    // }

    @Override
    public Collection<UserAttributes> convertPrincipalAttributes(P principal, U account) {
        String id = principal.getPrincipalId();
        Map<String, Serializable> attributes = principal.getAttributes();

        if (account instanceof AbstractUserAccount) {
            // set principal attrs in account
            ((AbstractUserAccount) account).setAttributes(attributes);
        }

        // call extract to transform
        return extractUserAttributes(account, attributes);
    }

    @Override
    public Collection<UserAttributes> getAccountAttributes(U account) {
        // account can't be null
        if (account == null) {
            throw new IllegalArgumentException();
        }

        String id = account.getAccountId();
        Map<String, Serializable> attributes = Collections.emptyMap();

        if (account instanceof AbstractUserAccount) {
            // read principal attrs from account
            attributes = ((AbstractUserAccount) account).getAttributes();
        }

        // call extract to transform
        return extractUserAttributes(account, attributes);
    }

    /*
     * Extract operation to be implemented by subclasses
     */
    protected abstract List<UserAttributes> extractUserAttributes(
        U account,
        Map<String, Serializable> principalAttributes
    );
}
