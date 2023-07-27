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

public enum EncryptionMethod {
    /*
     * AES HMAC SHA
     */
    A128CBC_HS256("A128CBC-HS256"),

    A192CBC_HS384("A192CBC-HS384"),

    A256CBC_HS512("A256CBC-HS512"),

    /*
     * AES GCM
     */
    A128GCM("A128GCM"),

    A192GCM("A192GCM"),

    A256GCM("A256GCM");

    private final String value;

    EncryptionMethod(String value) {
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

    public static EncryptionMethod parse(String value) {
        for (EncryptionMethod m : EncryptionMethod.values()) {
            if (m.value.equalsIgnoreCase(value)) {
                return m;
            }
        }

        return null;
    }
}
