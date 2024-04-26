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
import it.smartcommunitylab.aac.spid.model.SpidError;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;

public class SpidAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    private final SpidError error;
    private final String saml2Request;
    private final String saml2Response;

    public SpidAuthenticationException(String message) {
        super(message);
        error = SpidError.SYSTEM_ERROR;
        saml2Request = null;
        saml2Response = null;
    }

    public SpidAuthenticationException(String message, Throwable cause) {
        super(message, cause);
        error = SpidError.SYSTEM_ERROR;
        saml2Request = null;
        saml2Response = null;
    }

    public SpidAuthenticationException(SpidError error) {
        super(error.getMessage());
        this.error = error;
        saml2Request = null;
        saml2Response = null;
    }

    public SpidAuthenticationException(SpidError error, String saml2Request, String saml2Response) {
        super(error.getErrorCode());
        this.error = error;
        this.saml2Request = saml2Request;
        this.saml2Response = saml2Response;
    }

    public SpidAuthenticationException(Saml2Error error, String message) {
        super(message);
        this.error = SpidError.translate(error);
        saml2Request = null;
        saml2Response = null;
    }

    public SpidAuthenticationException(Saml2Error error, String message, String saml2Request, String saml2Response) {
        super(message);
        this.error = SpidError.translate(error);
        this.saml2Request = saml2Request;
        this.saml2Response = saml2Response;
    }

    public SpidAuthenticationException(Saml2AuthenticationException ex) {
        super(ex.getMessage());
        this.error = SpidError.translate(ex.getSaml2Error());
        saml2Request = null;
        saml2Response = null;
    }

    public SpidAuthenticationException(Saml2AuthenticationException ex, String saml2Request, String saml2Response) {
        super(ex.getMessage());
        this.error = SpidError.translate(ex.getSaml2Error());
        this.saml2Request = saml2Request;
        this.saml2Response = saml2Response;
    }

    public SpidError getError() {
        return error;
    }

    public String getErrorMessage() {
        return "error.spid_authentication." + getError().getErrorCode();
    }

    public String getSaml2Request() {
        return saml2Request;
    }

    public String getSaml2Response() {
        return saml2Response;
    }
}
