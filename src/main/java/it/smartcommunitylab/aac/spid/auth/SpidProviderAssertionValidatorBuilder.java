package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidError;
import it.smartcommunitylab.aac.spid.service.SpidRequestParser;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.util.StringUtils;

public class SpidProviderAssertionValidatorBuilder {

    private final Set<SpidAttribute> requestedAttributes;

    public SpidProviderAssertionValidatorBuilder(Set<SpidAttribute> requestedAttributes) {
        this.requestedAttributes = requestedAttributes;
    }

    public SpidProviderAssertionValidatorBuilder() {
        this.requestedAttributes = new HashSet<>();
    }

    public Converter<OpenSaml4AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> build() {
        // leverage opensaml default validator, then expand with custom logic and behaviour
        Converter<OpenSaml4AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> defaultValidator =
            OpenSaml4AuthenticationProvider.createDefaultAssertionValidator();

        return assertionToken -> {
            Saml2ResponseValidatorResult result = defaultValidator.convert(assertionToken);
            if (result != null && !result.getErrors().isEmpty()) {
                return result; // already invalid, no need to go on
            }
            AuthnRequest initiatingRequest = SpidRequestParser.parse(
                assertionToken.getToken().getAuthenticationRequest()
            );
            if (initiatingRequest == null) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }
            // assertions must be signed; note that signature validity is performed by default validator; here we check signature existence
            Assertion assertion = assertionToken.getAssertion();
            if (assertion.getSignature() == null) {
                return result.concat(new Saml2Error(Saml2ErrorCodes.INVALID_SIGNATURE, "missing assertion signature"));
            }

            // IssueInstant attribute must exists and be non trivial
            if (assertion.getIssueInstant() == null) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing IssueInstant attribute")
                );
            }
            if (!isAssertionIssueInstantFormatValid(assertion)) {
                throw new SpidAuthenticationException(SpidError.SPID_FAILED_RESPONSE_VALIDATION);
            }
            if (!isAssertionIssueInstantAfterRequest(initiatingRequest, assertion)) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "assertion IssueInstant before request IssueInstant"
                    )
                );
            }
            if (!isAssertionIssueInstantBeforeRequestReception(assertion)) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "assertion IssueInstant is after the instant of the received response"
                    )
                );
            }

            if (
                assertion.getSubject().getNameID() == null ||
                assertion.getSubject().getNameID().getNameQualifier() == null
            ) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "missing NameQualifier attribute in NameId"
                    )
                );
            }

            if (!isAssertionSubjectConfirmationValid(assertion)) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing SubjectConfirmation")
                );
            }

            if (!isAssertionIssuerValid(assertion)) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing or invalid Assertion Issuer")
                );
            }
            if (!isAssertionConditionsValid(assertion)) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing or invalid Conditions attribute")
                );
            }
            if (!isAssertionAuthStatementValid(assertion)) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "missing or invalid AuthStatement attribute"
                    )
                );
            }

            if (!isAssertionNameIDFormatValid(assertion)) {
                return result.concat(
                    new Saml2Error(
                        Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                        "missing or invalid NameID Format attribute in assertion"
                    )
                );
            }
            if (!areRequestedAttributesObtained(assertion)) {
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing requested attributes attribute")
                );
            }
            return result;
        };
    }

    private boolean isAssertionSubjectConfirmationValid(Assertion assertion) {
        if (assertion.getSubject().getSubjectConfirmations() == null) {
            return false;
        }
        if (assertion.getSubject().getSubjectConfirmations().isEmpty()) {
            return false;
        }
        SubjectConfirmation confirmation = assertion
            .getSubject()
            .getSubjectConfirmations()
            .stream()
            .findFirst()
            .orElse(null);
        if (confirmation == null) {
            return false;
        }
        if (confirmation.getSubjectConfirmationData() == null) {
            return false;
        }
        if (confirmation.getSubjectConfirmationData().getRecipient() == null) {
            return false;
        }
        if (confirmation.getSubjectConfirmationData().getInResponseTo() == null) {
            return false;
        }
        if (confirmation.getSubjectConfirmationData().getNotOnOrAfter() == null) {
            return false;
        }
        return true;
    }

    private boolean isAssertionNameIDFormatValid(Assertion assertion) {
        if (assertion == null || assertion.getSubject() == null || assertion.getSubject().getNameID() == null) {
            return false;
        }
        String fmt = assertion.getSubject().getNameID().getFormat();
        return StringUtils.hasText(fmt) && fmt.equals(NameIDType.TRANSIENT);
    }

    private boolean isAssertionIssuerValid(Assertion assertion) {
        if (assertion == null || assertion.getIssuer() == null) {
            return false;
        }
        if (assertion.getIssuer().getFormat() == null) {
            return false;
        }
        if (!assertion.getIssuer().getFormat().equals(NameIDType.ENTITY)) {
            return false;
        }
        return true;
    }

    private boolean isAssertionIssueInstantFormatValid(Assertion assertion) {
        if (assertion.getDOM() == null || assertion.getDOM().getAttribute("IssueInstant").isEmpty()) {
            return false;
        }
        String issueInstant = assertion.getDOM().getAttribute("IssueInstant");
        return SpidInstantValidationUtils.isInstantFormatValid(issueInstant);
    }

    private boolean isAssertionIssueInstantAfterRequest(AuthnRequest initiatingRequest, Assertion assertion) {
        // TODO: considera un clock skew
        Instant requestInstant = initiatingRequest.getIssueInstant();
        Instant assertionInstant = assertion.getIssueInstant();
        if (requestInstant == null || assertionInstant == null) {
            return false;
        }
        return !assertionInstant.isBefore(requestInstant);
    }

    private boolean isAssertionIssueInstantBeforeRequestReception(Assertion assertion) {
        // TODO: considera un clock skew
        Instant assertionInstant = assertion.getIssueInstant();
        if (assertionInstant == null) {
            return false;
        }
        // NOTE: technically, Instant.now() is not the instant when the request is received. Should this be fixed?
        return assertion.getIssueInstant().isBefore(Instant.now());
    }

    private boolean isAssertionConditionsValid(Assertion assertion) {
        if (assertion == null || assertion.getConditions() == null) {
            return false;
        }
        if (assertion.getConditions().getNotBefore() == null || assertion.getConditions().getNotOnOrAfter() == null) {
            return false;
        }
        if (
            assertion.getConditions().getAudienceRestrictions() == null ||
            assertion.getConditions().getAudienceRestrictions().isEmpty()
        ) {
            return false;
        }
        return true;
    }

    private boolean isAssertionAuthStatementValid(Assertion assertion) {
        if (assertion == null || assertion.getAuthnStatements().isEmpty()) {
            return false;
        }
        AuthnStatement authStatement = assertion.getAuthnStatements().stream().findFirst().orElse(null);
        if (authStatement == null) {
            return false;
        }
        if (authStatement.getAuthnContext() == null) {
            return false;
        }
        AuthnContextClassRef acr = authStatement.getAuthnContext().getAuthnContextClassRef();
        if (acr == null) {
            return false;
        }
        return SpidAuthnContext.parse(acr.getURI()) != null;
    }

    /*
     * Check that the SPID attributes returned by the identity provider match (at least)
     * the requested SPID attributes.
     * Note that *in general* a service provider might be configured with multiple attribute
     * sets and the SAML request actually specify which attribute sets, among the many, is
     * actually request to the identity provider; but the current SPID implementation
     * supports the definition of only *one* attribute set and that set is always requested.
     */
    private boolean areRequestedAttributesObtained(Assertion assertion) {
        Set<SpidAttribute> obtainedAttributes = SpidProviderResponseValidatorBuilder.extractAttributes(assertion);
        return obtainedAttributes != null && obtainedAttributes.containsAll(this.requestedAttributes);
    }
}
