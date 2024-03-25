package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import it.smartcommunitylab.aac.spid.auth.SpidResponseValidator;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidError;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SpidAuthenticationProvider extends ExtendedAuthenticationProvider<SpidUserAuthenticatedPrincipal, SpidUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final SpidUserAttribute SUBJECT_ATTRIBUTE = SpidUserAttribute.SUBJECT;
    private static final String ACR_ATTRIBUTE = "authnContextClassRef";
    private final Set<String> registrationIds;
    private final SpidUserAttribute subjectAttribute;
    private final SpidUserAttribute usernameAttribute;
    private final SpidResponseValidator spidValidator;


    private final OpenSaml4AuthenticationProvider samlAuthProvider;
    public SpidAuthenticationProvider(
        String providerId,
        UserAccountService<SpidUserAccount> accountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_SPID, providerId, accountService, config, realm);
    }

    public SpidAuthenticationProvider(
        String authority,
        String providerId,
        UserAccountService<SpidUserAccount> accountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        this.registrationIds = config.getRelyingPartyRegistrationIds();
        this.subjectAttribute = config.getSubAttributeName();
        this.usernameAttribute = config.getUsernameAttributeName();

        this.spidValidator = new SpidResponseValidator();
        this.samlAuthProvider = new OpenSaml4AuthenticationProvider();
        this.samlAuthProvider.setAssertionValidator(buildProviderAssertionValidator());
        this.samlAuthProvider.setResponseAuthenticationConverter(buildProviderResponseConverter());
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication) {
        Saml2AuthenticationToken loginAuthenticationToken = (Saml2AuthenticationToken) authentication;
        String registrationId = loginAuthenticationToken.getRelyingPartyRegistration().getRegistrationId();
        if (!registrationIds.contains(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        String serializedResponse = loginAuthenticationToken.getSaml2Response();
        try {
            Authentication token = samlAuthProvider.authenticate(authentication);
            Saml2Authentication auth = (Saml2Authentication) token;
            // TODO: here Saml (0) checks that the token has a subject, (1) checks that account isn't locked and (2) inserts a default role "ROLE_USER" - is this required? Btw this uses the (currently unused) accountService
            return auth;
        } catch (Saml2AuthenticationException e) {
            // check if wrapped spid ex
            if (e.getCause() instanceof SpidAuthenticationException) {
                SpidError err = ((SpidAuthenticationException) e.getCause()).getError();
                throw new SpidAuthenticationException(err, null, serializedResponse);
            }

            throw new SpidAuthenticationException(e, null, serializedResponse);
        }
    }

    @Override
    protected SpidUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack token and fetch properties from repo
        // note we expect default behavior, if provider has a converter this will break
        Saml2AuthenticatedPrincipal samlDetails = (Saml2AuthenticatedPrincipal) principal;

        String subjectId = evaluateSubjectIdFromPrincipal(samlDetails);
        if (subjectId == null) {
            logger.error("Failed to evaluate or obtain the subjectId in the the Saml2AuthenticatedPrincipal");
            return null; // fail authentication
        }
        // A. create empty user with no attributes
        SpidUserAuthenticatedPrincipal user = new SpidUserAuthenticatedPrincipal(
            getProvider(),
            getRealm(),
            null, // userId is evaluated later from attributes
            subjectId
        );
        // username must exists: let choose from config, fallback to principal name
        String username = StringUtils.hasText(evaluateSubjectIdFromPrincipal(samlDetails))
            ? evaluateUsernameFromPrincipal(samlDetails)
            : samlDetails.getName();
        user.setUsername(username);
        user.setPrincipal(samlDetails);

        // TODO: if executionService != null ... a che serve questa roba? Devo aggiungerla?

        return user;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && Saml2AuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Converter<OpenSaml4AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> buildProviderAssertionValidator() {
        // leverage opensaml default validator, then expand with custom logic and behaviour
        Converter<OpenSaml4AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> defaultValidator = OpenSaml4AuthenticationProvider
                .createDefaultAssertionValidator();
        return (assertionToken) -> {
            // call default
            Saml2ResponseValidatorResult result = defaultValidator.convert(assertionToken);

            // explicitly validate assertion signature as required, even when response is
            // signed
            // TODO

            Assertion assertion = assertionToken.getAssertion();

            return result;
        };
    }

    private Converter<OpenSaml4AuthenticationProvider.ResponseToken, ? extends AbstractAuthenticationToken> buildProviderResponseConverter() {
        // leverage opensaml default response authentication converter, then expand with custom logic and behaviour
        Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> defaultResponseConverter =
                OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();
        return (responseToken) -> {
            Response response = responseToken.getResponse();
            Saml2Authentication auth = defaultResponseConverter.convert(responseToken);

            // validate as required by SPID
            spidValidator.validateResponse(responseToken);

            // extract extra information and add as attribute, then rebuild auth with new enriched attributes and default authority
            Map<String, List<Object>> attributes = new HashMap<>();
            SpidAuthnContext authCtx = extractAcrValue(response);
            if (authCtx != null) {
                attributes.put(ACR_ATTRIBUTE, Collections.singletonList((Object)(authCtx.getValue())));
            }
            // TODO: also add issuer and issueInstant? are they required by someone

            // rebuild auth
            DefaultSaml2AuthenticatedPrincipal principal = new DefaultSaml2AuthenticatedPrincipal(
                    auth.getName(),
                    attributes
            );
            if (auth.getPrincipal() instanceof DefaultSaml2AuthenticatedPrincipal) {
                principal = new DefaultSaml2AuthenticatedPrincipal(
                            auth.getName(),
                            attributes,
                            ((DefaultSaml2AuthenticatedPrincipal) auth.getPrincipal()).getSessionIndexes()
                        );
            }

            return new Saml2Authentication(
                principal,
                auth.getSaml2Response(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        };
    }

    private static @Nullable SpidAuthnContext extractAcrValue(Response response) {
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

    private @Nullable String evaluateSubjectIdFromPrincipal(Saml2AuthenticatedPrincipal principal) {
        return StringUtils.hasText(subjectAttribute.getValue())
            ? principal.getFirstAttribute(subjectAttribute.getValue())
            : principal.getName();
    }

    private @Nullable String evaluateUsernameFromPrincipal(Saml2AuthenticatedPrincipal principal) {
        return StringUtils.hasText(usernameAttribute.getValue())
            ? principal.getFirstAttribute(usernameAttribute.getValue())
            : principal.getName();
    }
}
