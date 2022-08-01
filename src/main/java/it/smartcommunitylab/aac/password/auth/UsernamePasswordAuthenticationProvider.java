package it.smartcommunitylab.aac.password.auth;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountService;
import it.smartcommunitylab.aac.password.service.InternalPasswordService;

public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalAccountService accountService;
    private final InternalPasswordService passwordService;

    public UsernamePasswordAuthenticationProvider(String providerId,
            InternalAccountService accountService, InternalPasswordService passwordService,
            String realm) {
        Assert.hasText(providerId, "provider can not be null or empty");
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");

        this.accountService = accountService;
        this.passwordService = passwordService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Only UsernamePasswordAuthenticationToken is supported");

        UsernamePasswordAuthenticationToken authRequest = (UsernamePasswordAuthenticationToken) authentication;

        String username = authRequest.getUsername();
        String password = authRequest.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        try {
            InternalUserAccount account = accountService.findAccountByUsername(username);
            if (account == null) {
                throw new BadCredentialsException("invalid request");
            }

            // delegate password check
            if (!passwordService.verifyPassword(username, password)) {
                throw new BadCredentialsException("invalid request");
            }

            // always grant user role
            // we really don't have any additional role on accounts, aac roles are set on
            // subject
            Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_USER));

            // build a valid token
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, password,
                    account, authorities);

            return auth;

        } catch (Exception e) {
            logger.error(e.getMessage());
            // don't leak
            throw new BadCredentialsException("invalid request");
        }

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
