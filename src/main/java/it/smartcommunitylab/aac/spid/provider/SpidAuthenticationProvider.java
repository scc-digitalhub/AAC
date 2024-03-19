package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

public class SpidAuthenticationProvider extends ExtendedAuthenticationProvider<SpidUserAuthenticatedPrincipal, SpidUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
        // TODO
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication) {
        // TODO
        return null;
    }

    @Override
    protected SpidUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // TODO
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // TODO
        return false;
    }
}
