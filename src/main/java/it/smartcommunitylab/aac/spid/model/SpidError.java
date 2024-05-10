/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.util.Assert;

public enum SpidError {
    //    AUTHENTICATION_SUCCESS(1, "authentication_success"),
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
    AUTH_FAILED_WRONG_IDENTITY_TYPE(30, "auth failed: wrong identity type"),

    SPID_FAILED_RESPONSE_VALIDATION(1000, "invalid_spid_response"),

    // custom codes mapped 1←→1 with Saml Error Codes; used to distinguish Spid exclusive validation errors from generic Saml validation errors produced by Spring/OpenSaml
    SAML_UNKNOWN_RESPONSE_CLASS(1001, "unknown_response_class"),
    SAML_MALFORMED_REQUEST_DATA(1002, "malformed_request_data"),
    SAML_MALFORMED_RESPONSE_DATA(1003, "malformed_response_data"),
    SAML_INVALID_REQUEST(1004, "invalid_request"),
    SAML_INVALID_RESPONSE(1005, "invalid_response"),
    SAML_INVALID_DESTINATION(1006, "invalid_destination"),
    SAML_INVALID_ASSERTION(1007, "invalid_assertion"),
    SAML_INVALID_SIGNATURE(1008, "invalid_signature"),
    SAML_SUBJECT_NOT_FOUND(1009, "subject_not_found"),
    SAML_USERNAME_NOT_FOUND(1010, "username_not_found"),
    SAML_DECRYPTION_ERROR(1011, "decryption_error"),
    SAML_INVALID_ISSUER(1012, "invalid_issuer"),
    SAML_INTERNAL_VALIDATION_ERROR(1013, "internal_validation_error"),
    SAML_RELYING_PARTY_REGISTRATION_NOT_FOUND(1014, "relying_party_registration_not_found"),
    SAML_INVALID_IN_RESPONSE_TO(1015, "invalid_in_response_to");

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
            if (t.value.equals(value)) {
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
                error = SAML_UNKNOWN_RESPONSE_CLASS;
                break;
            case Saml2ErrorCodes.MALFORMED_RESPONSE_DATA:
                error = SAML_MALFORMED_REQUEST_DATA;
                break;
            case Saml2ErrorCodes.INVALID_DESTINATION:
                error = SAML_INVALID_DESTINATION;
                break;
            case Saml2ErrorCodes.INVALID_ASSERTION:
                error = SAML_INVALID_ASSERTION;
                break;
            case Saml2ErrorCodes.INVALID_SIGNATURE:
                error = SAML_INVALID_SIGNATURE;
                break;
            case Saml2ErrorCodes.SUBJECT_NOT_FOUND:
                error = SAML_SUBJECT_NOT_FOUND;
                break;
            case Saml2ErrorCodes.USERNAME_NOT_FOUND:
                error = SAML_USERNAME_NOT_FOUND;
                break;
            case Saml2ErrorCodes.DECRYPTION_ERROR:
                error = SAML_DECRYPTION_ERROR;
                break;
            case Saml2ErrorCodes.INVALID_ISSUER:
                error = SAML_INVALID_ISSUER;
                break;
            case Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR:
                error = SAML_INTERNAL_VALIDATION_ERROR;
                break;
            case Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND:
                error = SAML_RELYING_PARTY_REGISTRATION_NOT_FOUND;
                break;
        }

        return error;
    }
}
