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
