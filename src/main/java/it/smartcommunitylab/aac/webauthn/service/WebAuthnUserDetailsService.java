package it.smartcommunitylab.aac.webauthn.service;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnUserDetailsService implements UserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String realm;

    private final WebAuthnUserAccountService userAccountService;

    public WebAuthnUserDetailsService(WebAuthnUserAccountService userAccountService, String realm) {
        Assert.notNull(userAccountService, "user account service is mandatory");
        this.userAccountService = userAccountService;
        this.realm = realm;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        logger.debug("load by user id " + userId);

        // expected that the user name is already unwrapped ready for repository
        String username = userId;

        WebAuthnUserAccount account = userAccountService.findByUsername(realm, username);
        if (account == null) {
            throw new UsernameNotFoundException(
                    "WebAuthn user with username " + username + " does not exist for realm " + realm);
        }

        // always grant user role
        // we really don't have any additional role on accounts, aac roles are set on
        // subject
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_USER));

        // we set the id as username in result
        // also map notConfirmed to locked
        return new User(account.getUsername(), account.getCredential().toJSON(),
                true, true, true,
                account.getHasCompletedRegistration(),
                authorities);
    }
}
