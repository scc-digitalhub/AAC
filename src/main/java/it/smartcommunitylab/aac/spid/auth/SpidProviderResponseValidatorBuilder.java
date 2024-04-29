package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidError;
import it.smartcommunitylab.aac.spid.service.SpidRequestParser;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import liquibase.pro.packaged.S;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class SpidProviderResponseValidatorBuilder {

    public SpidProviderResponseValidatorBuilder() {}

    public Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2ResponseValidatorResult> build() {
        // leverage default validator
        Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2ResponseValidatorResult> defaultValidator =
            OpenSaml4AuthenticationProvider.createDefaultResponseValidator();
        return responseToken -> {
            Saml2ResponseValidatorResult result = defaultValidator.convert(responseToken);
            // overwrite status code validation in order to inject custom Status interpretation and return to user meaningful information
            switch (validateResponseStatus(responseToken.getResponse())) {
                case ERROR_UNKNOWN_OR_MALFORMED -> result =
                    result.concat(new Saml2Error("SPID_ERROR_26", "invalid status code"));
                case ERROR_19 -> throw new SpidAuthenticationException(SpidError.AUTH_FAILED_REQUEST_COUNT);
                case ERROR_20 -> throw new SpidAuthenticationException(SpidError.AUTH_FAILED_INVALID_CREDENTIALS);
                case ERROR_21 -> throw new SpidAuthenticationException(SpidError.AUTH_FAILED_TIMEOUT);
                case ERROR_22 -> throw new SpidAuthenticationException(SpidError.AUTH_FAILED_NOT_APPROVED);
                case ERROR_23 -> throw new SpidAuthenticationException(SpidError.AUTH_FAILED_USER_LOCKED);
                case ERROR_25 -> throw new SpidAuthenticationException(SpidError.AUTH_FAILED_CANCELED);
                case ERROR_30 -> throw new SpidAuthenticationException(SpidError.AUTH_FAILED_WRONG_IDENTITY_TYPE);
            }
            if (result != null && !result.getErrors().isEmpty()) {
                return result; // already invalid, no need to go on
            }
            AuthnRequest initiatingRequest = SpidRequestParser.parse(
                responseToken.getToken().getAuthenticationRequest()
            );
            if (initiatingRequest == null) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing initiating saml request")
                );
            }
            // TODO: verifica come scrivere CODICI ERRORE SPID
            // version must exists and must be 2.0 as per https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response
            Response response = responseToken.getResponse();
            if (response.getVersion() == null || !isVersion20(response)) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing or wrong response version")
                );
            }

            // Issue Instant must be present, be non null, have correct format
            Instant issueInstant = response.getIssueInstant();
            if (issueInstant == null) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "missing or undefined issue instant attribute"
                    )
                );
            }
            if (!isIssueInstantFormatValid(response)) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "invalid issue instant format")
                );
            }
            if (!isIssueInstantAfterRequest(initiatingRequest, response)) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "issue instant is before request issue instant"
                    )
                );
            }

            // InResponseTo attribute must exists an d be nontrivial
            if (!StringUtils.hasText(response.getInResponseTo())) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing or empty InResponseTo attribute")
                );
            }
            // Destination attribute must exists and be nontrivial
            if (!StringUtils.hasText(response.getDestination())) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing or empty Destination attribute")
                );
            }

            // Issuer Format attribute must be entity
            if (!isIssuerFormatEntity(response)) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "wrong or missing Issuer Format Attribute"
                    )
                );
            }

            if (!isResponseAcrValid(initiatingRequest, response)) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "obtained ACR does not match requested ACR"
                    )
                );
            }

            return result;
        };
    }

    private boolean isIssueInstantAfterRequest(AuthnRequest initiatingRequest, Response response) {
        // TODO: considera un clock skew
        Instant requestInstant = initiatingRequest.getIssueInstant();
        Instant responseInstant = response.getIssueInstant();
        if (requestInstant == null || responseInstant == null) {
            return false;
        }
        if (responseInstant.isBefore(requestInstant)) {
            return false;
        }
        return true;
    }

    /*
     * Verify if the authentication was positive on the user side. The result
     * can be one of 3 possible cases:
     * (1) Positive, in which case OK is returned.
     * (2) Negative, which either unknown reason or due to malformed data
     *  response, in which case ERROR_UNKNOWN_OR_MALFORMED is returned.
     * (3) Negative due to a known user anomaly, in which case
     *  ERROR_19 to ERROR_30 is returned.
     */
    private ResponseStatusValidationResult validateResponseStatus(Response response)
        throws SpidAuthenticationException {
        if (response.getStatus() == null || response.getStatus().getStatusCode() == null) {
            return ResponseStatusValidationResult.ERROR_UNKNOWN_OR_MALFORMED;
        }
        StatusCode sc = response.getStatus().getStatusCode();
        if (StatusCode.SUCCESS.equals(sc.getValue())) {
            return ResponseStatusValidationResult.OK;
        }
        // check if user anomaly is among those in listed in
        // https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html#anomalie-derivanti-dall-utente
        if (!StatusCode.RESPONDER.equals(sc.getValue())) {
            return ResponseStatusValidationResult.ERROR_UNKNOWN_OR_MALFORMED;
        }
        StatusCode subCode = sc.getStatusCode();
        if (subCode == null || !StatusCode.AUTHN_FAILED.equals(subCode.getValue())) {
            return ResponseStatusValidationResult.ERROR_UNKNOWN_OR_MALFORMED;
        }
        StatusMessage status = response.getStatus().getStatusMessage();
        if (status == null || !StringUtils.hasText(status.getValue())) {
            return ResponseStatusValidationResult.ERROR_UNKNOWN_OR_MALFORMED;
        }
        return switch (status.getValue()) {
            case "ErrorCode nr19" -> ResponseStatusValidationResult.ERROR_19;
            case "ErrorCode nr20" -> ResponseStatusValidationResult.ERROR_20;
            case "ErrorCode nr21" -> ResponseStatusValidationResult.ERROR_21;
            case "ErrorCode nr22" -> ResponseStatusValidationResult.ERROR_22;
            case "ErrorCode nr23" -> ResponseStatusValidationResult.ERROR_23;
            case "ErrorCode nr25" -> ResponseStatusValidationResult.ERROR_25;
            case "ErrorCode nr30" -> ResponseStatusValidationResult.ERROR_30;
            default -> ResponseStatusValidationResult.ERROR_UNKNOWN_OR_MALFORMED;
        };
    }

    private boolean isVersion20(Response response) {
        return (response.getVersion().getMajorVersion() == 2 && response.getVersion().getMinorVersion() == 0);
    }

    private boolean isIssueInstantFormatValid(Response response) {
        if (response.getDOM() == null || response.getDOM().getAttribute("IssueInstant").isEmpty()) {
            return false;
        }
        String issueInstant = response.getDOM().getAttribute("IssueInstant");

        return SpidInstantValidationUtils.isInstantFormatValid(issueInstant);
    }

    private boolean isIssuerFormatEntity(Response response) {
        if (response.getIssuer() == null || !StringUtils.hasText(response.getIssuer().getFormat())) {
            return false;
        }
        return response.getIssuer().getFormat().equals(NameIDType.ENTITY);
    }

    private boolean isResponseAcrValid(AuthnRequest initiatingRequest, Response response) {
        // assumption: we require for MINIMUM acr
        SpidAuthnContext expAcr = extractRequestedAcrValue(initiatingRequest);
        SpidAuthnContext obtAcr = extractAcrValue(response);
        if (expAcr == null || obtAcr == null) {
            return false;
        }
        // ACR are in a bijection with an ordered set assumed to be {1,2,3}
        Map<SpidAuthnContext, Integer> acrToOrderedSet = new HashMap<>() {
            {
                put(SpidAuthnContext.SPID_L1, 1);
                put(SpidAuthnContext.SPID_L2, 2);
                put(SpidAuthnContext.SPID_L3, 3);
            }
        };
        Integer expAcrValue = acrToOrderedSet.get(expAcr);
        Integer obtAcrValue = acrToOrderedSet.get(obtAcr);
        return obtAcrValue >= expAcrValue;
    }

    public static @Nullable SpidAuthnContext extractRequestedAcrValue(AuthnRequest request) {
        AuthnContextClassRef acr = request
            .getRequestedAuthnContext()
            .getAuthnContextClassRefs()
            .stream()
            .findFirst()
            .orElse(null);
        if (acr == null || !StringUtils.hasText(acr.getURI())) {
            return null;
        }
        return SpidAuthnContext.parse(acr.getURI());
    }

    /*
     * extractAcrValue parse and returns the ACR from a SPID SAML response.
     * If no ACR is successfully parser, null value is returned.
     */
    public static @Nullable SpidAuthnContext extractAcrValue(Response response) {
        Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
        if (assertion == null) {
            return null;
        }

        AuthnContext authnContext = assertion
            .getAuthnStatements()
            .stream()
            .filter(a -> a.getAuthnContext() != null)
            .findFirst()
            .map(a -> a.getAuthnContext())
            .orElse(null);

        if (authnContext == null) {
            return null;
        }

        String acrValue = authnContext.getAuthnContextClassRef().getURI();
        return SpidAuthnContext.parse(acrValue);
    }

    /* Result of validation of StatusCode in a SPID SAML Response.
     * Known errors are in bijection with those described at
     *  https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html#anomalie-derivanti-dall-utente
     */
    private enum ResponseStatusValidationResult {
        OK,
        ERROR_UNKNOWN_OR_MALFORMED,
        ERROR_19,
        ERROR_20,
        ERROR_21,
        ERROR_22,
        ERROR_23,
        ERROR_25,
        ERROR_30,
    }
}
