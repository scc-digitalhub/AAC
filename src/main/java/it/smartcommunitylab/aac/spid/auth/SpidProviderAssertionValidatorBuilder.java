package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.service.SpidRequestParser;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class SpidProviderAssertionValidatorBuilder {

    private Map<Integer, Set<SpidAttribute>> configuredSpidAttributeSets; // map [index set] -> requested attributes associated to that set

    public SpidProviderAssertionValidatorBuilder(Map<Integer, Set<SpidAttribute>> configuredSpidAttributeSets) {
        this.configuredSpidAttributeSets = configuredSpidAttributeSets;
    }

    public SpidProviderAssertionValidatorBuilder() {
        this.configuredSpidAttributeSets = new HashMap<>();
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
                return result.concat(
                    new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "missing initiating saml request")
                );
            }
            // TODO: valuta i codici di errore: attualmente sono una reference al test
            // assertions must be signed; note that signature validity is performed by default validator; here we check signature existence
            Assertion assertion = assertionToken.getAssertion();
            if (assertion.getSignature() == null) {
                return result.concat(new Saml2Error("SPID_ERROR_004", "missing assertion signature"));
            }

            // IssueInstant attribute must exists and be non trivial
            if (assertion.getIssueInstant() == null) {
                return result.concat(new Saml2Error("SPID_ERROR_037", "missing IssueInstant attribute"));
            }
            if (!isAssertionIssueInstantFormatValid(assertion)) {}
            if (!isAssertionIssueInstantAfterRequest(initiatingRequest, assertion)) {
                return result.concat(
                    new Saml2Error("SPID_ERROR_39", "assertion IssueInstant before request IssueInstant")
                );
            }
            if (!isAssertionIssueInstantBeforeRequestReception(assertion)) {
                return result.concat(
                    new Saml2Error(
                        "SPID_ERROR_40",
                        "assertion IssueInstant is after the instant of the received response"
                    )
                );
            }

            if (
                assertion.getSubject().getNameID() == null ||
                assertion.getSubject().getNameID().getNameQualifier() == null
            ) {
                return result.concat(new Saml2Error("SPID_ERROR_048", "missing NameQualifier attribute in NameId"));
            }

            if (!isAssertionSubjectConfirmationValid(assertion)) {
                return result.concat(new Saml2Error("SPID_ERROR_51", "missing SubjectConfirmation"));
            }

            if (!isAssertionIssuerValid(assertion)) {
                return result.concat(new Saml2Error("SPID_ERROR_70", "missing or invalid Assertion Issuer"));
            }
            if (!isAssertionConditionsValid(assertion)) {
                return result.concat(new Saml2Error("SPID_ERROR_73", "missing or invalid Conditions attribute"));
            }
            if (!isAssertionAuthStatementValid(assertion)) {
                return result.concat(new Saml2Error("SPID_ERROR_88", "missing or invalid AuthStatement attribute"));
            }

            //            if (!areRequestedAttributesObtained(initiatingRequest, this.configuredSpidAttributeSets, assertion)) {
            //                return result.concat(new Saml2Error("SPID_ERROR_103", "missing requested attributes attribute"));
            //            }
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
        // TODO: move subject confirmation data checks somewhere else
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
        if (assertionInstant.isBefore(requestInstant)) {
            return false;
        }
        return true;
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
        if (SpidAuthnContext.parse(acr.getURI()) == null) {
            return false;
        }
        return true;
    }

    /*
     * This test is always passed as no expected result should is provided.
     * See reference:
     *  https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/metadata.html#service-provider
     * The SPID test 103 serve the purpose of making the service aware of the
     * fact that received attributes might be different than requested
     * attributes and the SP should be written in order to either handle
     * or accommodate this scenario.
     */
    private boolean areRequestedAttributesObtained(
        AuthnRequest initiatingRequest,
        Map<Integer, Set<SpidAttribute>> availableAttributeSets,
        Assertion assertion
    ) {
        //        Set<SpidAttribute> expAttributes = SpidProviderResponseValidatorBuilder.extractRequestedAttributes(initiatingRequest, availableAttributeSets);
        //        Set<SpidAttribute> obtAttributes = SpidProviderResponseValidatorBuilder.extractAttributes(assertion);
        //        return expAttributes.containsAll(obtAttributes);
        return true;
    }
}
