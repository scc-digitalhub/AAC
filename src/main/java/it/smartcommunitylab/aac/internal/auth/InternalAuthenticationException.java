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

package it.smartcommunitylab.aac.internal.auth;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.security.core.AuthenticationException;

public class InternalAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String subject;
    private final String username;
    private final String credentials;
    private final String flow;
    private final AuthenticationException exception;

    public InternalAuthenticationException(String subject, String message) {
        super(message);
        this.subject = subject;
        this.username = null;
        this.flow = null;
        this.credentials = null;
        this.exception = null;
    }

    public InternalAuthenticationException(
        String subject,
        String username,
        String credentials,
        String flow,
        AuthenticationException ex
    ) {
        super(ex.getMessage(), ex.getCause());
        this.subject = subject;
        this.username = username;
        this.credentials = credentials;
        this.flow = flow;
        this.exception = ex;
    }

    public InternalAuthenticationException(
        String subject,
        String username,
        String credentials,
        String flow,
        AuthenticationException ex,
        String message
    ) {
        super(message, ex.getCause());
        this.subject = subject;
        this.username = username;
        this.credentials = credentials;
        this.flow = flow;
        this.exception = ex;
    }

    public String getSubject() {
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public String getCredentials() {
        return credentials;
    }

    public String getFlow() {
        return flow;
    }

    public AuthenticationException getException() {
        return exception;
    }

    public String getError() {
        return exception != null ? exception.getClass().getSimpleName() : null;
    }

    public String getErrorMessage() {
        String err = getError();
        if (err == null) {
            return "internal_error";
        }

        String msg = err.replaceAll(R_REGEX, R_REPL).toLowerCase();
        if (msg.endsWith(R_SUFFIX)) {
            msg = msg.substring(0, msg.length() - R_SUFFIX.length());
        }

        return "error." + msg;
    }

    // regex to convert camelCase to snake_case
    private static final String R_REGEX = "([a-z])([A-Z]+)";
    private static final String R_REPL = "$1_$2";
    private static final String R_SUFFIX = "_exception";
}
