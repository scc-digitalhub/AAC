package it.smartcommunitylab.aac.internal.provider;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.auth.DefaultUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.persistence.RoleEntityRepository;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.internal.service.InternalUserDetailsService;

public class InternalAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserAccountRepository accountRepository;

    private final InternalUserDetailsService userDetailsService;
    private final DaoAuthenticationProvider authProvider;

    public InternalAuthenticationProvider(String providerId,
            InternalUserAccountRepository accountRepository, RoleEntityRepository roleRepository,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        this.accountRepository = accountRepository;

        // build a userDetails service
        userDetailsService = new InternalUserDetailsService(accountRepository, roleRepository, realm);

        // build our internal auth provider by wrapping spring dao authprovider
        authProvider = new DaoAuthenticationProvider();
        // set user details service
        authProvider.setUserDetailsService(this.userDetailsService);
        // we use our password encoder
        authProvider.setPasswordEncoder(new InternalPasswordEncoder());

    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // just delegate to dao
        return authProvider.authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authProvider.supports(authentication);
    }

    @Override
    protected UserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack user and fetch properties from repo
        UserDetails details = (UserDetails) principal;
        // TODO complete mapping, for now this suffices
        String username = details.getUsername();
        String userId = this.exportInternalId(username);

        InternalUserAccount account = accountRepository.findByRealmAndUsername(getRealm(), username);
        // TODO build fullName, fallback to username
        String name = account.getUsername();

        DefaultUserAuthenticatedPrincipal user = new DefaultUserAuthenticatedPrincipal(userId, name,
                Collections.emptyMap());

        return user;
    }

}
