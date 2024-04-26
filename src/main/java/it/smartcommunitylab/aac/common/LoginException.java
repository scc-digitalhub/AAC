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

package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.oidc.auth.OIDCAuthenticationException;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticationException;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

public class LoginException extends AuthenticationException {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private final String error;

    public LoginException(AuthenticationException e) {
        super(e.getMessage(), e.getCause());
        this.error = e.getClass().getSimpleName();
    }

    public LoginException(String error, AuthenticationException e) {
        super(e.getMessage(), e.getCause());
        Assert.hasText(error, "error can not be null");
        this.error = error;
    }

    public String getError() {
        return error;
    }

    /*
     * Static builder TODO move
     */

    public static LoginException translate(SamlAuthenticationException e) {
        return new LoginException(e.getErrorMessage(), e);
    }

    public static LoginException translate(OIDCAuthenticationException e) {
        return new LoginException(e.getErrorMessage(), e);
    }

    public static LoginException translate(InternalAuthenticationException e) {
        return new LoginException(e.getErrorMessage(), e);
    }

    public static LoginException translate(SpidAuthenticationException e) {
        return new LoginException(e.getErrorMessage(), e);
    }

    public static LoginException translate(AuthenticationException e) {
        if (e instanceof SamlAuthenticationException) {
            return translate((SamlAuthenticationException) e);
        }

        if (e instanceof OIDCAuthenticationException) {
            return translate((OIDCAuthenticationException) e);
        }

        if (e instanceof InternalAuthenticationException) {
            return translate((InternalAuthenticationException) e);
        }

        if (e instanceof SpidAuthenticationException) {
            return translate((SpidAuthenticationException) e);
        }

        String error = "error." + e.getClass().getSimpleName().replaceAll(R_REGEX, R_REPL).toLowerCase();
        if (error.endsWith(R_SUFFIX)) {
            error = error.substring(0, error.length() - R_SUFFIX.length());
        }
        return new LoginException(error, e);
    }

    // regex to convert camelCase to snake_case
    private static final String R_REGEX = "([a-z])([A-Z]+)";
    private static final String R_REPL = "$1_$2";
    private static final String R_SUFFIX = "_exception";
}
