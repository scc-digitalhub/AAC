package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum JWSAlgorithm {
    /*
     * SHA HMAC
     */
    HS256("HS256"),

    HS384("HS384"),

    HS512("HS512"),

    /*
     * RSASSA-PKCS using SHA-256
     */
    RS256("RS256"),

    RS384("RS384"),

    RS512("RS512"),

    /*
     * ECDSA
     */
    ES256("ES256"),

    ES384("ES384"),

    ES512("ES512");

    private final String value;

    JWSAlgorithm(String value) {
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

    public static JWSAlgorithm parse(String value) {
        for (JWSAlgorithm a : JWSAlgorithm.values()) {
            if (a.value.equalsIgnoreCase(value)) {
                return a;
            }
        }

        return null;
    }
}
