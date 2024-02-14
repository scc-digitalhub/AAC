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

public enum JWEAlgorithm {
    /*
     * AES Key Wrap Algorithm (RFC 3394)
     */
    A128KW("A128KW"),

    A192KW("A192KW"),

    A256KW("A256KW"),

    /*
     * Elliptic Curve Diffie-Hellman Ephemeral Static (RFC 6090)
     */
    ECDH_ES("ECDH-ES"),

    ECDH_ES_A128KW("ECDH-ES+A128KW"),

    ECDH_ES_A192KW("ECDH-ES+A192KW"),

    ECDH_ES_A256KW("ECDH-ES+A256KW"),

    /*
     * AES GCM
     */
    A128GCMKW("A128GCMKW"),

    A192GCMKW("A192GCMKW"),

    A256GCMKW("A256GCMKW"),

    /*
     * RSAES using Optimal Asymmetric Encryption Padding (OAEP) (RFC 3447),
     */

    RSA_OAEP("RSA-OAEP"),

    RSA_OAEP_256("RSA-OAEP-256"),

    RSA_OAEP_384("RSA-OAEP-384"),

    RSA_OAEP_512("RSA-OAEP-512");

    private final String value;

    JWEAlgorithm(String value) {
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

    public static JWEAlgorithm parse(String value) {
        for (JWEAlgorithm a : JWEAlgorithm.values()) {
            if (a.value.equalsIgnoreCase(value)) {
                return a;
            }
        }

        return null;
    }
}
