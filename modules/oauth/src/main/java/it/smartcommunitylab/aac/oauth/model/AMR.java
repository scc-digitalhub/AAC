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

package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum AMR {
    /*
     * Password authentication, either userPassword or clientSecret
     */
    PWD("pwd"),

    /*
     * Proof of RSA key. Also valid for self-signed JWT and X509 certificate.
     */
    RSA("rsa"),

    /*
     * OTP via mail or SMS
     */
    OTP("otp"),

    /*
     * External authentication via JWT or SAML
     */
    EXT("ext"),

    /*
     * MultiFactor in addition to other methods, which MUST be included
     */
    MFA("mfa"),

    /*
     * No auth
     */
    NONE("none");

    private final String value;

    AMR(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String toString() {
        return value;
    }

    public static AMR parse(String value) {
        for (AMR t : AMR.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
