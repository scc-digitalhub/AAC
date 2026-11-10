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

package it.smartcommunitylab.aac.eidas.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidUserIdentity;

/*
 * Identity built from an eIDAS authentication: same shape as the SPID one (backed by the shared
 * saml account persistence), with its own resource type.
 */
public class EIDASUserIdentity extends SpidUserIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_EIDAS_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_IDENTITY + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_EIDAS;

    public EIDASUserIdentity(
        String authority,
        String provider,
        String realm,
        SamlUserAccount account,
        SpidUserAuthenticatedPrincipal principal
    ) {
        super(authority, provider, realm, account, principal);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }
}
