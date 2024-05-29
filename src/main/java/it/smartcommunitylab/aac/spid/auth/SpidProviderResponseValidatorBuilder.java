package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidError;
import it.smartcommunitylab.aac.spid.service.SpidRequestParser;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
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
            SpidError codeError = validateResponseStatus(responseToken.getResponse());
            if (codeError != null) {
                throw new SpidAuthenticationException(codeError);
            }
            if (result != null && !result.getErrors().isEmpty()) {
                return result; // already invalid, no need to go on
            }
            AuthnRequest initiatingRequest = SpidRequestParser.parse(
                responseToken.getToken().getAuthenticationRequest()
            );
            if (initiatingRequest == null) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }

            Response response = responseToken.getResponse();

            // version must exists and must be 2.0 as per https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response
            if (response.getVersion() == null || !isVersion20(response)) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }

            // ID must exists and must be not null
            if (!StringUtils.hasText(response.getID())) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }

            // Issue Instant must be present, be non null, have correct format
            Instant issueInstant = response.getIssueInstant();
            if (issueInstant == null) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }
            if (!isIssueInstantFormatValid(response)) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }
            if (!isIssueInstantAfterRequest(initiatingRequest, response)) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }

            // InResponseTo attribute must exists an d be nontrivial
            if (!StringUtils.hasText(response.getInResponseTo())) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }
            // Destination attribute must exists and be nontrivial
            if (!StringUtils.hasText(response.getDestination())) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }

            // Issuer Format attribute must be entity
            if (!isIssuerFormatEntity(response)) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }

            if (!isResponseAcrValid(initiatingRequest, response)) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
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
     * Verify if the Status Code of the authentication response was positive.
     * (1) Positive, in which case null is returned.
     * (2) Negative, which either unknown reason or due to malformed data
     *  response, in which case SPID_FAILED_RESPONSE_VALIDATION is returned.
     * (3) Negative due to a known anomaly, in which case the corresponding
     *  Spid Error is returned: https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html
     * Note that status codes 19 to 30 are regulated as should be associated to
     *  a specific error message on the user side.
     */
    private @Nullable SpidError validateResponseStatus(Response response) throws SpidAuthenticationException {
        if (response.getStatus() == null || response.getStatus().getStatusCode() == null) {
            return SpidError.SPID_FAILED_RESPONSE_VALIDATION;
        }
        StatusCode sc = response.getStatus().getStatusCode();
        if (StatusCode.SUCCESS.equals(sc.getValue())) {
            return null;
        }
        if (!StatusCode.RESPONDER.equals(sc.getValue())) {
            return SpidError.SPID_FAILED_RESPONSE_VALIDATION;
        }
        StatusCode subCode = sc.getStatusCode();
        if (subCode == null || !StatusCode.AUTHN_FAILED.equals(subCode.getValue())) {
            return SpidError.SPID_FAILED_RESPONSE_VALIDATION;
        }
        StatusMessage status = response.getStatus().getStatusMessage();
        if (status == null || !StringUtils.hasText(status.getValue())) {
            return SpidError.SPID_FAILED_RESPONSE_VALIDATION;
        }
        return switch (status.getValue()) {
            case "ErrorCode nr02" -> SpidError.SYSTEM_UNAVAILABLE;
            case "ErrorCode nr03" -> SpidError.SYSTEM_ERROR;
            case "ErrorCode nr04" -> SpidError.INVALID_BINDING_FORMAT;
            case "ErrorCode nr05" -> SpidError.INVALID_SIGNATURE;
            case "ErrorCode nr06" -> SpidError.INVALID_BINDING;
            case "ErrorCode nr07" -> SpidError.INVALID_REQUEST_SIGNATURE;
            case "ErrorCode nr08" -> SpidError.MALFORMED_RESPONSE_DATA;
            case "ErrorCode nr09" -> SpidError.UNKNOWN_RESPONSE_CLASS;
            case "ErrorCode nr10" -> SpidError.INVALID_ISSUER;
            case "ErrorCode nr11" -> SpidError.INVALID_ID;
            case "ErrorCode nr12" -> SpidError.INVALID_AUTHNCONTEXT;
            case "ErrorCode nr13" -> SpidError.INVALID_ISSUEINSTANT;
            case "ErrorCode nr14" -> SpidError.INVALID_DESTINATION;
            case "ErrorCode nr15" -> SpidError.ISPASSIVE_ERROR;
            case "ErrorCode nr16" -> SpidError.INVALID_ACS;
            case "ErrorCode nr17" -> SpidError.INVALID_NAMEFORMAT;
            case "ErrorCode nr18" -> SpidError.INVALID_ACS_INDEX;
            case "ErrorCode nr19" -> SpidError.AUTH_FAILED_REQUEST_COUNT;
            case "ErrorCode nr20" -> SpidError.AUTH_FAILED_INVALID_CREDENTIALS;
            case "ErrorCode nr21" -> SpidError.AUTH_FAILED_TIMEOUT;
            case "ErrorCode nr22" -> SpidError.AUTH_FAILED_NOT_APPROVED;
            case "ErrorCode nr23" -> SpidError.AUTH_FAILED_USER_LOCKED;
            case "ErrorCode nr25" -> SpidError.AUTH_FAILED_CANCELED;
            case "ErrorCode nr30" -> SpidError.AUTH_FAILED_WRONG_IDENTITY_TYPE;
            default -> SpidError.SPID_FAILED_RESPONSE_VALIDATION;
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

    public static @Nullable Set<SpidAttribute> extractAttributes(Assertion assertion) {
        if (assertion == null || assertion.getAuthnStatements() == null) {
            return null;
        }
        List<Attribute> attrs = assertion
            .getAttributeStatements()
            .stream()
            .findFirst()
            .map(AttributeStatement::getAttributes)
            .orElse(null);
        if (attrs == null) {
            return null;
        }
        List<SpidAttribute> spidAttrs = attrs
            .stream()
            .map(a -> SpidAttribute.parse(a.getName()))
            .filter(Objects::nonNull) // attributes not matching a spid attribute are parsed to null
            .toList();
        return new HashSet<>(spidAttrs);
    }
}
