package it.smartcommunitylab.aac.spid.model;

import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SpidError {
//    AUTENTICATION_SUCCESS(1),
    SYSTEM_UNAVAILABLE(2),
    SYSTEM_ERROR(3),
    INVALID_BINDING_FORMAT(4),
    INVALID_SIGNATURE(5),
    INVALID_BINDING(6),
    INVALID_REQUEST_SIGNATURE(7),
    MALFORMED_RESPONSE_DATA(8),
    UNKNOWN_RESPONSE_CLASS(9),
    INVALID_ISSUER(10),
    INVALID_ID(11),
    INVALID_AUTHNCONTEXT(12),
    INVALID_ISSUEINSTANT(13),
    INVALID_DESTINATION(14),
    ISPASSIVE_ERROR(15),
    INVALID_ACS(16),
    INVALID_NAMEFORMAT(17),
    INVALID_ACS_INDEX(18),
    AUTH_FAILED_REQUEST_COUNT(19),
    AUTH_FAILED_INVALID_CREDENTIALS(20),
    AUTH_FAILED_TIMEOUT(21),
    AUTH_FAILED_NOT_APPROVED(22),
    AUTH_FAILED_USER_LOCKED(23),
    AUTH_FAILED_CANCELED(25),
    AUTH_FAILED_WRONG_IDENTITY_TYPE(30);

    private final Integer value;

    SpidError(Integer value) {
        Assert.notNull(value, "value cannot be empty");
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public String toString() {
        return String.valueOf(value);
    }

    public String getErrorCode() {
        return "spid.error." + Integer.toString(value);
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
