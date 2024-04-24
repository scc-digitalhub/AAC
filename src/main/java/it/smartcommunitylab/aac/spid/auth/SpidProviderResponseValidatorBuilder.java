package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.service.SpidRequestParser;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
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
                return result.concat(new Saml2Error("SPID_ERROR_010", "missing or wrong response version"));
            }

            // Issue Instant must be present, be non null, have correct format
            Instant issueInstant = response.getIssueInstant();
            if (issueInstant == null) {
                return result.concat(new Saml2Error("SPID_ERROR_011", "missing or undefined issue instant attribute"));
            }
            if (!isIssueInstantFormatValid(response)) {
                return result.concat(new Saml2Error("SPID_ERROR_012", "invalid issue instant format"));
            }
            if (!isIssueInstantAfterRequest(initiatingRequest, response)) {
                return result.concat(new Saml2Error("SPID_ERROR_14", "issue instant is before request issue instant"));
            }

            // InResponseTo attribute must exists an d be nontrivial
            if (!StringUtils.hasText(response.getInResponseTo())) {
                return result.concat(new Saml2Error("SPID_ERROR_017", "missing or empty InResponseTo attribute"));
            }
            // Destination attribute must exists and be nontrivial
            if (!StringUtils.hasText(response.getDestination())) {
                return result.concat(new Saml2Error("SPID_ERROR_019", "missing or empty Destination attribute"));
            }

            // Issuer Format attribute must be entity
            if (!isIssuerFormatEntity(response)) {
                return result.concat(new Saml2Error("SPID_ERROR_030", "wrong or missing Issuer Format Attribute"));
            }

            if (!isResponseAcrValid(initiatingRequest, response)) {
                return result.concat(new Saml2Error("SPID_ERROR_094", "obtained ACR does not match requested ACR"));
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
}
