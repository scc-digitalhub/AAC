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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.spid.provider.SpidAttributeProvider;

/*
 * Attribute extraction is identical to SPID (the node releases SPID-named attributes): only the
 * authority stamped on the produced attribute sets changes.
 */
public class EIDASAttributeProvider extends SpidAttributeProvider {

    public EIDASAttributeProvider(String providerId, EIDASIdentityProviderConfig providerConfig, String realm) {
        super(providerId, providerConfig, realm);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_EIDAS;
    }
}
