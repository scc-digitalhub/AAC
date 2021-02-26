package it.smartcommunitylab.aac.core.auth;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.IdentityAuthority;
import it.smartcommunitylab.aac.core.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;

/*
 * Authentication manager
 * 
 * handles authentication process by dispatching authRequests to specific providers
 * expects all provider to be registered to an external registry
 * 
 * note: we should support anonymousToken as fallback for public pages
 */

public class ExtendedAuthenticationManager implements AuthenticationManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AuthorityManager authorityManager;

    public ExtendedAuthenticationManager(AuthorityManager authorityManager) {
        Assert.notNull(authorityManager, "authority manager is required");
        this.authorityManager = authorityManager;
        logger.debug("authentication manager created");
    }

    public ProviderWrappedAuthenticationToken wrapAuthentication(String authority, String provider,
            AbstractAuthenticationToken authentication) throws AuthenticationException {
        return new ProviderWrappedAuthenticationToken(authority, provider, authentication);
    }

    public AbstractAuthenticationToken unwrapAuthentication(ProviderWrappedAuthenticationToken authentication) {
        return authentication.getAuthenticationToken();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("process authentication for " + authentication.getName());

        if (!supports(authentication.getClass())) {
            logger.error("invalid authentication class: " + authentication.getClass().getName());
            throw new AuthenticationServiceException("invalid request");
        }

        // extract provider info by unwrapping request
        ProviderWrappedAuthenticationToken request = (ProviderWrappedAuthenticationToken) authentication;
        String authorityId = request.getAuthority();
        String providerId = request.getProvider();
        AbstractAuthenticationToken token = request.getAuthenticationToken();

        logger.debug("authentication token for " + String.valueOf(authorityId) + ":" + String.valueOf(providerId));
        logger.trace(String.valueOf(token));

        // validate
        if (!StringUtils.hasText(authorityId) || !StringUtils.hasText(providerId)) {
            logger.error("missing or invalid authorityId or providerId "
                    + String.valueOf(authorityId) + ":" + String.valueOf(providerId));
            throw new ProviderNotFoundException("provider not found");
        }

        if (token == null) {
            logger.error("missing authentication token");
            throw new BadCredentialsException("invalid authentication request");
        }

        IdentityAuthority authority = authorityManager.getIdentityAuthority(authorityId);
        IdentityProvider idp = authority.getIdentityProvider(providerId);

        if (idp == null) {
            logger.error("provider not found for " + authority + ":" + providerId);
            throw new ProviderNotFoundException("provider not found for " + authority + ":" + providerId);
        }

        ExtendedAuthenticationProvider provider = idp.getAuthenticationProvider();

        // perform extended authentication
        logger.debug("perform authentication via provider");
        ExtendedAuthenticationToken auth = provider.authenticateExtended(token);
        logger.debug("received authentication token from provider");
        logger.trace("auth token is " + auth.toString());

        // should have produced a valid principal
        UserAuthenticatedPrincipal principal = auth.getPrincipal();
        if (principal == null) {
            // reject
            logger.error("missing principal in auth token");
            throw new UsernameNotFoundException("no principal for user from provider");
        }

        logger.trace("auth principal is " + principal.toString());

        // authentication provider should yield a proper userId (addressable)
        String userId = auth.getPrincipal().getUserId();
        logger.debug("authenticated userId is " + userId);

        // fetch authorities
        Collection<GrantedAuthority> authorities = auth.getAuthorities();
        logger.debug("authenticated user granted authorities are " + authorities.toString());

        // convert to identity for user auth
        try {
            UserIdentity identity = idp.convertIdentity(principal);
            logger.debug("converted authToken to identity");
            logger.trace("userIdentity is " + identity.toString());

            // validate userId matches authentication
            if (!userId.equals(identity.getUserId())) {
                // provider misbehave or corruption
                logger.error("userId does not match");
                throw new AuthenticationServiceException("error processing request");
            }

            // transform attributes
            // could be a no-op, we expect attributes mapped to the shared schema
            Collection<UserAttributes> attributeSets = idp.getAttributeProvider()
                    .convertAttributes(identity.getAttributes());

            // link to subject
            SubjectResolver resolver = idp.getSubjectResolver();
            Subject subject = resolver.resolveByUserId(userId);
            if (subject == null) {
                // provider error
                logger.error("missing subject from provider");
                throw new AuthenticationServiceException("error processing request");
            }

            logger.debug("resolved subject for identity to " + subject.getSubjectId());

            // clear credentials from token, we don't use them anymore
            auth.eraseCredentials();
            // we also force erase credentials from the request to ensure non reuse
            request.eraseCredentials();
            // also erase credentials from identity, we don't want them in token
            identity.eraseCredentials();

            // we can build the user authentication
            UserAuthenticationToken result = new UserAuthenticationToken(
                    subject,
                    auth,
                    identity, attributeSets,
                    authorities);

            logger.debug("successfully build userAuthentication token for " + result.getSubjectId());
            logger.trace("userAuthentication is " + result.toString());

            return result;

        } catch (NoSuchUserException e) {
            logger.error("idp could not resolve identity for user " + userId);
            throw new UsernameNotFoundException("no identity for user from provider");
        }

    }

    public boolean supports(Class<?> authentication) {
        // we support only requests with provider ids
        return (ProviderWrappedAuthenticationToken.class
                .isAssignableFrom(authentication));
    }

}
