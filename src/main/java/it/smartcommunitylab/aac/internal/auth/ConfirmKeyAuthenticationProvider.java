package it.smartcommunitylab.aac.internal.auth;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityConfirmService;
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

public class ConfirmKeyAuthenticationProvider implements AuthenticationProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<InternalUserAccount> userAccountService;
    private final InternalIdentityConfirmService confirmService;

    private final String repositoryId;

    public ConfirmKeyAuthenticationProvider(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        InternalIdentityConfirmService confirmService,
        String repositoryId,
        String realm
    ) {
        Assert.hasText(providerId, "provider can not be null or empty");
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmService, "account confirm service is mandatory");
        Assert.hasText(repositoryId, "repository id can not be null or empty");

        this.userAccountService = userAccountService;
        this.confirmService = confirmService;

        this.repositoryId = repositoryId;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(
            ConfirmKeyAuthenticationToken.class,
            authentication,
            "Only ConfirmKeyAuthenticationToken is supported"
        );

        ConfirmKeyAuthenticationToken authRequest = (ConfirmKeyAuthenticationToken) authentication;

        String username = authRequest.getUsername();
        String key = authRequest.getKey();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(key)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        try {
            InternalUserAccount account = userAccountService.findAccountById(repositoryId, username);
            if (account == null) {
                throw new BadCredentialsException("invalid request");
            }

            // do confirm
            account = confirmService.confirmAccount(username, key);
            if (!account.isConfirmed()) {
                throw new BadCredentialsException("invalid request");
            }

            // always grant user role
            // we really don't have any additional role on accounts, aac roles are set on
            // subject
            Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_USER));

            // build a valid token
            ConfirmKeyAuthenticationToken auth = new ConfirmKeyAuthenticationToken(username, key, account, authorities);

            return auth;
        } catch (Exception e) {
            logger.error(e.getMessage());
            // don't leak
            throw new BadCredentialsException("invalid request");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (ConfirmKeyAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
