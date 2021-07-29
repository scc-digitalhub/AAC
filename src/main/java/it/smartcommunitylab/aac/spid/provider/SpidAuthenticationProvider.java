package it.smartcommunitylab.aac.spid.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider.ResponseToken;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.auth.SamlAuthentication;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;

public class SpidAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String SUBJECT_ATTRIBUTE = "subject";

    private final SpidIdentityProviderConfig providerConfig;
    private final String usernameAttributeName;
    private final boolean useSpidCodeAsNameId;

    private final Set<String> registrationIds;

    private final OpenSamlAuthenticationProvider openSamlProvider;

    public SpidAuthenticationProvider(String providerId,
            SpidIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(config, "provider config is mandatory");

        this.providerConfig = config;

        this.useSpidCodeAsNameId = config.getConfigMap().getUseSpidCodeAsNameId() != null
                ? config.getConfigMap().getUseSpidCodeAsNameId().booleanValue()
                : false;
        this.usernameAttributeName = config.getConfigMap().getUserNameAttributeName() != null
                ? config.getConfigMap().getUserNameAttributeName().getValue()
                : SUBJECT_ATTRIBUTE;

        this.registrationIds = config.getRelyingPartyRegistrationIds();

        // build a default saml provider with a clock skew of 5 minutes
        openSamlProvider = new OpenSamlAuthenticationProvider();
//        openSamlProvider.setAssertionValidator(OpenSamlAuthenticationProvider
//                .createDefaultAssertionValidator(assertionToken -> {
//                    Map<String, Object> params = new HashMap<>();
//                    params.put(SAML2AssertionValidationParameters.CLOCK_SKEW, Duration.ofMinutes(5).toMillis());
//                    return new ValidationContext(params);
//                }));
        Converter<ResponseToken, Saml2Authentication> authenticationConverter = OpenSamlAuthenticationProvider
                .createDefaultResponseAuthenticationConverter();
        openSamlProvider.setResponseAuthenticationConverter((responseToken) -> {
            Response response = responseToken.getResponse();
            Saml2AuthenticationToken token = responseToken.getToken();
            Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
            Saml2Authentication auth = authenticationConverter.convert(responseToken);

            // integrate assertions as attributes
            Map<String, List<Object>> attributes = new HashMap<>();
            attributes.putAll(((DefaultSaml2AuthenticatedPrincipal) auth.getPrincipal()).getAttributes());
            if (assertion.getIssuer() != null) {
                attributes.put("issuer", Collections.singletonList(assertion.getIssuer().getValue()));
            }
            if (assertion.getIssueInstant() != null) {
                attributes.put("issueInstant", Collections.singletonList(assertion.getIssueInstant().toString()));
            }
            AuthnStatement authnStatement = CollectionUtils.firstElement(assertion.getAuthnStatements());
            if (authnStatement != null) {
                attributes.put("authnContextClassRef", Collections.singletonList(
                        authnStatement.getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef()));
            }
            attributes.put(SUBJECT_ATTRIBUTE, Collections.singletonList(assertion.getSubject().getNameID().getValue()));

            return new Saml2Authentication(new DefaultSaml2AuthenticatedPrincipal(auth.getName(), attributes),
                    token.getSaml2Response(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        });

        // use a custom authorities mapper to cleanup authorities
        // replaced with converter
//        openSamlProvider.setAuthoritiesMapper(nullAuthoritiesMapper);
    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // extract registrationId and check if matches our providerid
        Saml2AuthenticationToken loginAuthenticationToken = (Saml2AuthenticationToken) authentication;
        String registrationId = loginAuthenticationToken.getRelyingPartyRegistration().getRegistrationId();
        if (!registrationIds.contains(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        // delegate to openSaml, and leverage default converter
        Authentication token = openSamlProvider.authenticate(authentication);
        Saml2Authentication auth = (Saml2Authentication) token;

        // TODO validate as per spid
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && Saml2AuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected UserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack token and fetch properties from repo
        // note we expect default behavior, if provider has a converter this will break
        Saml2AuthenticatedPrincipal samlDetails = (Saml2AuthenticatedPrincipal) principal;

        // username mapping, default name always set
        String username = StringUtils.hasText(samlDetails.getFirstAttribute(usernameAttributeName))
                ? samlDetails.getFirstAttribute(usernameAttributeName)
                : samlDetails.getName();
        String userId = StringUtils.hasText(samlDetails.getFirstAttribute(SUBJECT_ATTRIBUTE))
                ? samlDetails.getFirstAttribute(SUBJECT_ATTRIBUTE)
                : samlDetails.getName();

        if (useSpidCodeAsNameId) {
            String spidCode = samlDetails.getFirstAttribute(SpidAttribute.SPID_CODE.getValue());
            if (!StringUtils.hasText(spidCode)) {
                throw new Saml2AuthenticationException(
                        new Saml2Error(Saml2ErrorCodes.USERNAME_NOT_FOUND, "spidCode not found"));
            }

            userId = spidCode;
        }

        String idpName = samlDetails.getFirstAttribute("issuer");

        // bind principal to ourselves
        SpidAuthenticatedPrincipal user = new SpidAuthenticatedPrincipal(getProvider(), getRealm(),
                idpName, exportInternalId(userId));
        user.setName(username);
        user.setPrincipal(samlDetails);

        return user;
    }

}
