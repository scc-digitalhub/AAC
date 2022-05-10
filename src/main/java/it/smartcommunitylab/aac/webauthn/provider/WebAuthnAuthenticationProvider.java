package it.smartcommunitylab.aac.webauthn.provider;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationToken;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnAuthenticationProvider
        extends ExtendedAuthenticationProvider<WebAuthnUserAuthenticatedPrincipal, WebAuthnUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebAuthnIdentityProviderConfig config;

    private final WebAuthnUserAccountService userAccountService;

    public WebAuthnAuthenticationProvider(String providerId, WebAuthnUserAccountService userAccountService,
            WebAuthnIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        this.config = providerConfig;
        this.userAccountService = userAccountService;
    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(WebAuthnAuthenticationToken.class, authentication,
                "Only WebAuthnAuthenticationToken is supported");
        WebAuthnAuthenticationToken authRequest = (WebAuthnAuthenticationToken) authentication;

        String userHandle = authRequest.getUserHandle();
        AssertionRequest assertionRequest = authRequest.getAssertionRequest();
        AssertionResult assertionResult = authRequest.getAssertionResult();
        String assertion = authRequest.getAssertion();
        String subject = null;

        try {
            // make sure assertion is valid
            if (!assertionResult.isSuccess()) {
                throw new BadCredentialsException("assertion failed");
            }

            // check if account is present and locked
            WebAuthnUserAccount account = userAccountService.findAccountByUserHandle(getProvider(), userHandle);
            if (account == null || account.isLocked()) {
                throw new BadCredentialsException("invalid user");
            }

            // userId is equal to subject
            subject = account.getUserId();

            // always grant user role
            // we really don't have any additional role on accounts, aac roles are set on
            // subject
            Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Config.R_USER));

            // build a valid token
            WebAuthnAuthenticationToken auth = new WebAuthnAuthenticationToken(userHandle, assertionRequest, assertion,
                    assertionResult, account, authorities);

            return auth;
        } catch (BadCredentialsException e) {
            throw new WebAuthnAuthenticationException(subject, userHandle, assertion, e,
                    e.getMessage());
        }

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (WebAuthnAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    protected WebAuthnUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        WebAuthnUserAccount account = (WebAuthnUserAccount) principal;
        String userId = account.getUserId();
        String username = account.getUsername();
//        StringBuilder fullName = new StringBuilder();
//        fullName.append(account.getName()).append(" ").append(account.getSurname());
//
//        String name = fullName.toString();
//        if (!StringUtils.hasText(name)) {
//            name = username;
//        }

        WebAuthnUserAuthenticatedPrincipal user = new WebAuthnUserAuthenticatedPrincipal(getProvider(), getRealm(),
                userId, username);
        // set principal name as username
        user.setName(username);
        // set attributes to support mapping in idp
        user.setAccountAttributes(account);

        return user;
    }

    @Override
    protected Instant expiresAt(Authentication auth) {
        // build expiration with maxAge
        return Instant.now().plusSeconds(config.getMaxSessionDuration());
    }
}
