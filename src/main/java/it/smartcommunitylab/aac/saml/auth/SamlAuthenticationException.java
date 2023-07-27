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

package it.smartcommunitylab.aac.saml.auth;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;

public class SamlAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private final Saml2Error error;
    private final String saml2Request;
    private final String saml2Response;

    public SamlAuthenticationException(Saml2Error error) {
        this(error, error.getDescription());
    }

    public SamlAuthenticationException(Saml2Error error, String message) {
        super(message);
        this.error = error;
        saml2Request = null;
        saml2Response = null;
    }

    public SamlAuthenticationException(Saml2Error error, String message, String saml2Request, String saml2Response) {
        super(message);
        this.error = error;
        this.saml2Request = saml2Request;
        this.saml2Response = saml2Response;
    }

    public Saml2Error getError() {
        return error;
    }

    public String getErrorMessage() {
        return "saml.error." + getError().getErrorCode();
    }

    public String getSaml2Request() {
        return saml2Request;
    }

    public String getSaml2Response() {
        return saml2Response;
    }
}
