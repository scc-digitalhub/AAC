package it.smartcommunitylab.aac.saml.provider;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SamlUserAccountRepository accountRepository;

    private final OpenSamlAuthenticationProvider openSamlProvider;

    public SamlAuthenticationProvider(String providerId,
            SamlUserAccountRepository accountRepository,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        this.accountRepository = accountRepository;

        // build a default saml provider with a clock skew of 5 minutes
        openSamlProvider = new OpenSamlAuthenticationProvider();
//        openSamlProvider.setAssertionValidator(OpenSamlAuthenticationProvider
//                .createDefaultAssertionValidator(assertionToken -> {
//                    Map<String, Object> params = new HashMap<>();
//                    params.put(SAML2AssertionValidationParameters.CLOCK_SKEW, Duration.ofMinutes(5).toMillis());
//                    return new ValidationContext(params);
//                }));

        // use a custom authorities mapper to cleanup authorities
        // TODO replace with an authConverter for spring 5.4+
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
        Saml2Authentication auth = (Saml2Authentication) token;

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

        // TODO complete mapping, for now this suffices
        String userId = samlDetails.getName();
        String username = samlDetails.getName();

        // bind principal to ourselves
        SamlAuthenticatedPrincipal user = new SamlAuthenticatedPrincipal(getProvider(), getRealm(),
                exportInternalId(userId));
        user.setName(username);
        user.setPrincipal(samlDetails);

        return user;
    }

    private final GrantedAuthoritiesMapper nullAuthoritiesMapper = (authorities -> Collections.emptyList());

}
