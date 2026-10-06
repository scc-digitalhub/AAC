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
 * Principal of an eIDAS authentication: the SPID one, specialized for the authority and cleaned of what
 * does not belong to a SPID-profile Service Provider.
 */
public class EIDASUserAuthenticatedPrincipal extends SpidUserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_EIDAS_SERIAL_VERSION;

    public static final String RESOURCE_TYPE = String.join(
        SystemKeys.ID_SEPARATOR,
        SystemKeys.RESOURCE_PRINCIPAL,
        SystemKeys.AUTHORITY_EIDAS
    );

    public EIDASUserAuthenticatedPrincipal(String provider, String realm, String userId, String subjectId) {
        super(provider, realm, userId, subjectId);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_EIDAS;
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    /*
     * The node releases every attribute twice: with the eIDAS Name URI (under the naturalperson namespace)
     * and with the SPID name. AAC speaks SPID and reads the SPID names only; the URI form is noise that would
     * otherwise be persisted with the account and surface in the claims. Kept out of the principal attributes.
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
