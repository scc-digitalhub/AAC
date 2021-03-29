package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

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

    A256GCMKW("A256GCMKW");

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
