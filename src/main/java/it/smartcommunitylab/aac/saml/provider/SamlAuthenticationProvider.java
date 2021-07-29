package it.smartcommunitylab.aac.saml.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.auth.SamlAuthentication;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String SUBJECT_ATTRIBUTE = "subject";

    private final SamlUserAccountRepository accountRepository;
    private final SamlIdentityProviderConfig providerConfig;
    private final String usernameAttributeName;

    private final OpenSamlAuthenticationProvider openSamlProvider;

    public SamlAuthenticationProvider(String providerId,
            SamlUserAccountRepository accountRepository, SamlIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.providerConfig = config;
        this.accountRepository = accountRepository;

        this.usernameAttributeName = StringUtils.hasText(config.getConfigMap().getUserNameAttributeName())
                ? config.getConfigMap().getUserNameAttributeName()
                : SUBJECT_ATTRIBUTE;

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
//            Saml2AuthenticationToken token = responseToken.getToken();
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

            attributes.put(SUBJECT_ATTRIBUTE, Collections.singletonList(assertion.getSubject().getNameID().getValue()));

            return new SamlAuthentication(new DefaultSaml2AuthenticatedPrincipal(auth.getName(), attributes),
                    responseToken, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
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
        if (!getProvider().equals(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        // delegate to openSaml, and leverage default converter
        Authentication token = openSamlProvider.authenticate(authentication);
        SamlAuthentication auth = (SamlAuthentication) token;

        // TODO wrap response token and erase credentials etc
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

        // bind principal to ourselves
        SamlAuthenticatedPrincipal user = new SamlAuthenticatedPrincipal(getProvider(), getRealm(),
                exportInternalId(userId));
        user.setName(username);
        user.setPrincipal(samlDetails);

        return user;
    }

//    private final GrantedAuthoritiesMapper nullAuthoritiesMapper = (authorities -> Collections.emptyList());

}
