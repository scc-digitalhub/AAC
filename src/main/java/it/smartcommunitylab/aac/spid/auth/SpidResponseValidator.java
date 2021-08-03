package it.smartcommunitylab.aac.spid.auth;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider.ResponseToken;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.spid.model.SpidError;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;

public class SpidResponseValidator {

    static {
        OpenSamlInitializationService.initialize();
    }

//    private final ProviderRepository<SpidIdentityProviderConfig> registrationRepository;

//    public SpidResponseValidator(
//            ProviderRepository<SpidIdentityProviderConfig> registrationRepository) {
//        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
//        this.registrationRepository = registrationRepository;
//    }

    public SpidResponseValidator() {

    }

    public void validateResponse(ResponseToken responseToken) throws SpidAuthenticationException {

        Response response = responseToken.getResponse();
        Saml2AuthenticationToken token = responseToken.getToken();
        String saml2Response = token.getSaml2Response();

        // evaluate status
        Status status = response.getStatus();
        if (status == null) {
            throw new SpidAuthenticationException(SpidError.SYSTEM_ERROR, saml2Response);
        }
        String statusCode = status.getStatusCode().getValue();

//        if (StatusCode.REQUESTER.equals(status.getStatusCode().getValue())) {
//            throw new SpidResponseException(SpidError.MALFORMED_RESPONSE_DATA);
//        }
        if (StatusCode.VERSION_MISMATCH.equals(statusCode)) {
            throw new SpidAuthenticationException(SpidError.UNKNOWN_RESPONSE_CLASS, saml2Response);
        }

        // check assertion
        Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
        if (assertion == null) {
            throw new SpidAuthenticationException(SpidError.MALFORMED_RESPONSE_DATA, saml2Response);
        }

        // check issuer
        Issuer issuer = assertion.getIssuer();
        if (issuer == null) {
            throw new SpidAuthenticationException(SpidError.INVALID_ISSUER, saml2Response);
        }

        // check name format
        Subject subject = assertion.getSubject();
        if (subject == null) {
            throw new SpidAuthenticationException(SpidError.MALFORMED_RESPONSE_DATA, saml2Response);
        }

        if (!NameIDType.TRANSIENT.equals(assertion.getSubject().getNameID().getFormat())) {
            throw new SpidAuthenticationException(SpidError.INVALID_NAMEFORMAT, saml2Response);

        }

        // finally fail on no success
        if (!StatusCode.SUCCESS.equals(statusCode)) {
            throw new SpidAuthenticationException(SpidError.SYSTEM_ERROR, saml2Response);
        }
    }
}
