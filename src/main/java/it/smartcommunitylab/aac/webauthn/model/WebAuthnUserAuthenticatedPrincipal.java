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

package it.smartcommunitylab.aac.webauthn.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import java.io.Serializable;
import java.util.Map;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.StringUtils;

public class WebAuthnUserAuthenticatedPrincipal
    extends InternalUserAuthenticatedPrincipal
    implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PRINCIPAL + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_WEBAUTHN;

    private String userHandle;

    public WebAuthnUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm, userId, username);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> map = super.getAttributes();
        if (StringUtils.hasText(userHandle)) {
            map.put("userHandle", userHandle);
        }

        return map;
    }

    @Override
    public void setAccountAttributes(InternalUserAccount account) {
        if (account != null) {
            super.setAccountAttributes(account);

            userHandle = account.getUuid();
        }
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }
}
