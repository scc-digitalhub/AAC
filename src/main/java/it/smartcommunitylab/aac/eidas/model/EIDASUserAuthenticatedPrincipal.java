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
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import java.io.Serializable;
import java.util.Map;

/*
 * Principal of an eIDAS authentication: the SPID one (the authority is a constructor parameter of the
 * SPID principal), with its own resource type and the eIDAS specific rules on attributes and email.
 */
public class EIDASUserAuthenticatedPrincipal extends SpidUserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_EIDAS_SERIAL_VERSION;

    public static final String RESOURCE_TYPE = String.join(
        SystemKeys.ID_SEPARATOR,
        SystemKeys.RESOURCE_PRINCIPAL,
        SystemKeys.AUTHORITY_EIDAS
    );

    public EIDASUserAuthenticatedPrincipal(
        String authority,
        String provider,
        String realm,
        String userId,
        String subjectId
    ) {
        super(authority, provider, realm, userId, subjectId);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    /*
     * AAC speaks SPID and reads the SPID attribute names only. The italian node releases the SPID names
     * alone (observed on the pre-production node, 5.10.2026); should a node also release the eIDAS Name
     * URIs (naturalperson namespace), that form would be noise persisted with the account and surfaced in
     * the claims: kept out of the principal attributes.
     */
    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> attributes = super.getAttributes();
        attributes.keySet().removeIf(key -> key.startsWith(EIDASAttribute.NATURAL_PERSON_NAMESPACE));
        return attributes;
    }

    /*
     * SPID treats the email as verified because the SPID identity provider verifies it. For eIDAS the
     * email is a sector specific attribute outside the Minimum Data Set, released by a foreign node under
     * rules we do not know: no source says it is verified, so the trust is not inherited. Consequence: no automatic
     * linking of the eidas account to an existing user by email.
     */
    @Override
    public boolean isEmailVerified() {
        return false;
    }
}
