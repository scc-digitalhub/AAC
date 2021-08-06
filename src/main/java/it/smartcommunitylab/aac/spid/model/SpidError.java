package it.smartcommunitylab.aac.spid.model;

import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SpidError {
//    AUTENTICATION_SUCCESS(1),
    SYSTEM_UNAVAILABLE(2, "system_unavailable"),
    SYSTEM_ERROR(3, "system error"),
    INVALID_BINDING_FORMAT(4, "invalid binding format"),
    INVALID_SIGNATURE(5, "invalid signature"),
    INVALID_BINDING(6, "invalid http binding"),
    INVALID_REQUEST_SIGNATURE(7, "invalid request signature"),
    MALFORMED_RESPONSE_DATA(8, "response data malformed"),
    UNKNOWN_RESPONSE_CLASS(9, "unknown response class"),
    INVALID_ISSUER(10, "invalid issuer"),
    INVALID_ID(11, "invalid ID"),
    INVALID_AUTHNCONTEXT(12, "invalid authN context"),
    INVALID_ISSUEINSTANT(13, "invalid issueInstant"),
    INVALID_DESTINATION(14, "invalid destination"),
    ISPASSIVE_ERROR(15, "isPassive flag can not be set"),
    INVALID_ACS(16, "invalid assertion consumer service"),
    INVALID_NAMEFORMAT(17, "invalid nameFormat"),
    INVALID_ACS_INDEX(18, "invalid acs index"),
    AUTH_FAILED_REQUEST_COUNT(19, "auth failed: too many requests"),
    AUTH_FAILED_INVALID_CREDENTIALS(20, "auth failed: invalid credentials"),
    AUTH_FAILED_TIMEOUT(21, "auth failed: timeout"),
    AUTH_FAILED_NOT_APPROVED(22, "auth failed: not approved"),
    AUTH_FAILED_USER_LOCKED(23, "auth failed: user locked"),
    AUTH_FAILED_CANCELED(25, "auth failed: process canceled"),
    AUTH_FAILED_WRONG_IDENTITY_TYPE(30, "auth failed: wrong identity type");

    private final Integer value;
    private final String message;

    SpidError(Integer value, String message) {
        Assert.notNull(value, "value cannot be empty");
        this.value = value;
        this.message = message;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public String toString() {
        return String.valueOf(value);
    }

    @JsonIgnore
    public String getErrorCode() {
        return Integer.toString(value);
    }

    public String getMessage() {
        return message;
    }

    public static SpidError parse(Integer value) {
        for (SpidError t : SpidError.values()) {
            if (t.value == value) {
                return t;
            }
        }

        return null;
    }

    public static SpidError translate(Saml2Error saml2Error) {
        System.out.println("saml error " + saml2Error.getErrorCode());

        SpidError error = SpidError.SYSTEM_ERROR;

        switch (saml2Error.getErrorCode()) {
        case Saml2ErrorCodes.UNKNOWN_RESPONSE_CLASS:
            error = UNKNOWN_RESPONSE_CLASS;
            break;
        case Saml2ErrorCodes.MALFORMED_RESPONSE_DATA:
            error = MALFORMED_RESPONSE_DATA;
            break;
        case Saml2ErrorCodes.INVALID_DESTINATION:
            error = INVALID_DESTINATION;
            break;
        case Saml2ErrorCodes.INVALID_ASSERTION:
            error = MALFORMED_RESPONSE_DATA;
            break;
        case Saml2ErrorCodes.INVALID_SIGNATURE:
            error = INVALID_SIGNATURE;
            break;
        case Saml2ErrorCodes.SUBJECT_NOT_FOUND:
            error = MALFORMED_RESPONSE_DATA;
            break;
        case Saml2ErrorCodes.USERNAME_NOT_FOUND:
            error = MALFORMED_RESPONSE_DATA;
            break;
        case Saml2ErrorCodes.DECRYPTION_ERROR:
            error = INVALID_SIGNATURE;
            break;
        case Saml2ErrorCodes.INVALID_ISSUER:
            error = INVALID_ISSUER;
            break;
        case Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR:
            error = MALFORMED_RESPONSE_DATA;
            break;
        case Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND:
            error = INVALID_DESTINATION;
            break;

        }

        return error;
    }

}
