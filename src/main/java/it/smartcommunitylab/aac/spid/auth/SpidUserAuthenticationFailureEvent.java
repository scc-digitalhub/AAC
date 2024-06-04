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

package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.events.UserAuthenticationFailureEvent;
import java.io.Serializable;
import java.util.Map;
import org.springframework.security.core.Authentication;

public class SpidUserAuthenticationFailureEvent extends UserAuthenticationFailureEvent {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    // TODO evaluate adding fields for error and message

    public SpidUserAuthenticationFailureEvent(
        String authority,
        String provider,
        String realm,
        Authentication authentication,
        SpidAuthenticationException exception
    ) {
        super(authority, provider, realm, null, authentication, exception);
    }

    @Override
    public Map<String, Serializable> exportException() {
        Map<String, Serializable> data = super.exportException();

        SpidAuthenticationException ex = (SpidAuthenticationException) getException();
        data.put("error", ex.getError().name());
        data.put("errorCode", ex.getError().getValue());
        data.put("saml2Response", ex.getSaml2Response());

        return data;
    }
}
