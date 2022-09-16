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
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountProvider;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationException;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationToken;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;

public class WebAuthnAuthenticationProvider
        extends ExtendedAuthenticationProvider<WebAuthnUserAuthenticatedPrincipal, InternalUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final WebAuthnIdentityProviderConfig config;

    private final InternalAccountProvider accountProvider;
    private final WebAuthnIdentityCredentialsService credentialsService;

    public WebAuthnAuthenticationProvider(String providerId,
            InternalAccountProvider accountProvider,
            WebAuthnIdentityCredentialsService credentialsService,
            WebAuthnIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(accountProvider, "account provider is mandatory");
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.config = providerConfig;
        this.accountProvider = accountProvider;
        this.credentialsService = credentialsService;
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

        if (logger.isTraceEnabled()) {
            logger.trace("process token: {}", String.valueOf(authRequest));
            logger.trace("assertionRequest: {}", String.valueOf(assertionRequest));
            logger.trace("assertionResult: {}", String.valueOf(assertionResult));
        }

        try {
            // make sure assertion is valid
            // result is already built by filter
            if (!assertionResult.isSuccess()) {
                throw new BadCredentialsException("assertion failed");
            }

            // check if account is present and locked
            // userHandle is account uuid
            InternalUserAccount account = accountProvider.findAccountByUuid(userHandle);
            if (account == null || account.isLocked()) {
                throw new BadCredentialsException("invalid user");
            }

            try {
                // fetch associated credential
                String credentialId = assertionResult.getCredentialId().getBase64Url();
                WebAuthnUserCredential credential = credentialsService.findCredential(userHandle, credentialId);
                if (credential == null) {
                    throw new WebAuthnAuthenticationException(account.getUserId(), "invalid credentials");
                }

                // update usage counter

                credential = credentialsService.updateCredentialCounter(userHandle, credentialId,
                        assertionResult.getSignatureCount());
            } catch (RegistrationException | NoSuchCredentialException | NoSuchUserException e) {
                // don't leak credential is invalid
                throw new WebAuthnAuthenticationException(account.getUserId(), "invalid credentials");
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

            // copy details
            auth.setDetails(authRequest.getDetails());

            return auth;
        } catch (BadCredentialsException e) {
            logger.debug("invalid request: " + e.getMessage());
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
        InternalUserAccount account = (InternalUserAccount) principal;
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
