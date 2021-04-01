package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.ibm.icu.util.Calendar;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.auth.RealmWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.auth.WrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.model.Subject;

/*
 * Authentication manager
 * 
 * handles authentication process by dispatching authRequests to specific providers
 * expects all provider to be registered to an external registry
 * 
 * we don't want retries, a request should be handled only by the correct provider, or dropped
 * 
 * note: we should support anonymousToken as fallback for public pages
 */

public class ExtendedAuthenticationManager implements AuthenticationManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

//    private final AuthorityManager authorityManager;
    private final UserEntityService userService;
    private final ProviderManager providerManager;

    public ExtendedAuthenticationManager(
            ProviderManager providerManager,
            UserEntityService userService) {
        Assert.notNull(providerManager, "provider manager is required");
        Assert.notNull(userService, "user service is required");

        this.providerManager = providerManager;
        this.userService = userService;

        logger.debug("authentication manager created");
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("process authentication for " + authentication.getName());

        if (!(authentication instanceof WrappedAuthenticationToken)) {
            logger.error("invalid authentication class: " + authentication.getClass().getName());
            throw new AuthenticationServiceException("invalid request");
        }

        WrappedAuthenticationToken request = (WrappedAuthenticationToken) authentication;
        AbstractAuthenticationToken token = request.getAuthenticationToken();

        if (token == null) {
            logger.error("missing authentication token");
            throw new BadCredentialsException("invalid authentication request");
        }

        // resolve type

        if (request instanceof ProviderWrappedAuthenticationToken) {
            // resolve provider and then process auth
            ProviderWrappedAuthenticationToken providerRequest = (ProviderWrappedAuthenticationToken) authentication;
            String authorityId = providerRequest.getAuthority();
            String providerId = providerRequest.getProvider();

            logger.debug("authentication token for provider " + String.valueOf(authorityId) + ":"
                    + String.valueOf(providerId));
            logger.trace(String.valueOf(token));

            // validate
            if (!StringUtils.hasText(providerId)) {
                logger.error("missing or invalid  providerId " + String.valueOf(providerId));
                throw new ProviderNotFoundException("provider not found");
            }

            IdentityProvider idp = null;
            if (StringUtils.hasText(authorityId)) {
                // fast load
                idp = providerManager.fetchIdentityProvider(authorityId, providerId);
            } else {
                // from db
                idp = providerManager.findIdentityProvider(providerId);
            }

            if (idp == null) {
                logger.error("identity provider not found for " + providerId);
                throw new ProviderNotFoundException("provider not found for " + providerId);
            }

            ExtendedAuthenticationProvider eap = idp.getAuthenticationProvider();
            if (eap == null) {
                logger.error("auth provider not found for " + providerId);
                throw new ProviderNotFoundException("provider not found for " + providerId);
            }

            // process with provider, no fallback
            return doAuthenticate(request, eap);
        } else if (request instanceof RealmWrappedAuthenticationToken) {
            RealmWrappedAuthenticationToken realmRequest = (RealmWrappedAuthenticationToken) authentication;
            String authorityId = realmRequest.getAuthority();
            String realm = realmRequest.getRealm();

            logger.debug("authentication token for realm " + String.valueOf(realm) + ":" + String.valueOf(authorityId));
            logger.trace(String.valueOf(token));

            // validate
            if (!StringUtils.hasText(realm)) {
                logger.error("missing or invalid  realm " + String.valueOf(realm));
                throw new ProviderNotFoundException("provider not found");
            }

            // since we don't have a realm we ask all idps to process, and keep the first
            // result
            List<IdentityProvider> providers = new ArrayList<>();

            if (StringUtils.hasText(authorityId)) {
                // fast load
                providers.addAll(providerManager.fetchIdentityProviders(authorityId, realm));
            } else {
                // from db
                providers.addAll(providerManager.getIdentityProviders(realm));
            }

            UserAuthenticationToken result = null;

            // TODO rework loop
            for (IdentityProvider idp : providers) {
                ExtendedAuthenticationProvider eap = idp.getAuthenticationProvider();
                if (eap == null) {
                    continue;
                }

                result = attempAuthenticate(request, eap);
                if (result != null) {
                    break;
                }
            }

            if (result == null) {
                throw new BadCredentialsException("invalid authentication request");
            }

            return result;

        } else {
            throw new ProviderNotFoundException("provider not found");
        }

    }

    /*
     * Attemp authentication, returns null if request is not supported or if invalid
     */
    protected UserAuthenticationToken attempAuthenticate(WrappedAuthenticationToken request,
            ExtendedAuthenticationProvider provider) throws AuthenticationException {
        /*
         * Extended authentication:
         * 
         * process auth request via specific provider and obtain a valid userAuth
         * containing a principal
         */
        AbstractAuthenticationToken token = request.getAuthenticationToken();

        // check if request is supported
        if (!provider.supports(token.getClass())) {
            return null;
        }

        try {
            // perform extended authentication
            logger.debug("perform authentication via provider");
            ExtendedAuthenticationToken auth = provider.authenticate(token);
            logger.debug("received authentication token from provider");

            if (auth == null) {
                logger.error("null authentication result");
                return null;
            }

            return createSuccessAuthentication(request, auth);
        } catch (AuthenticationException ae) {
            return null;
        }
    }

    /*
     * Perform authentication, throws error
     */
    protected UserAuthenticationToken doAuthenticate(WrappedAuthenticationToken request,
            ExtendedAuthenticationProvider provider) throws AuthenticationException {

        /*
         * Extended authentication:
         * 
         * process auth request via specific provider and obtain a valid userAuth
         * containing a principal
         */
        AbstractAuthenticationToken token = request.getAuthenticationToken();

        // check if request is supported
        if (!provider.supports(token.getClass())) {
            logger.error("token is not supported by the requested provider");
            throw new BadCredentialsException("invalid authentication request");
        }

        // perform extended authentication
        logger.debug("perform authentication via provider");
        ExtendedAuthenticationToken auth = provider.authenticate(token);
        logger.debug("received authentication token from provider");

        if (auth == null) {
            logger.error("null authentication result");
            throw new BadCredentialsException("invalid authentication request");
        }

        return createSuccessAuthentication(request, auth);

    }

    protected UserAuthenticationToken createSuccessAuthentication(WrappedAuthenticationToken request,
            ExtendedAuthenticationToken auth) throws AuthenticationException {
        logger.trace("auth token is " + auth.toString());
        WebAuthenticationDetails webAuthDetails = request.getAuthenticationDetails();
        String authorityId = auth.getAuthority();
        String providerId = auth.getProvider();
        String realm = auth.getRealm();

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

        // fetch identity provider for the given userId
        // fast load skipping db
        IdentityProvider idp = providerManager.fetchIdentityProvider(authorityId, providerId);
        if (idp == null) {
            // should not happen, provider has become unavailable during login
            throw new ProviderNotFoundException("provider not found");
        }
        /*
         * Subject resolution:
         * 
         * from the principal we ask providers to resolve a subject
         */

        String subjectId = null;

        // ask the IdP to resolve for persisted accounts
        SubjectResolver resolver = idp.getSubjectResolver();
        Subject s = resolver.resolveByUserId(userId);
        if (s != null) {
            subjectId = s.getSubjectId();
            logger.debug("resolved subject for identity to " + subjectId);
        }

        if (subjectId == null) {
            // account linking via attributes
            // TODO, disabled now due to security concerns
            // should use linking attribute(s)
        }

        if (subjectId == null) {
            // generate a new subject, always persisted
            UserEntity u = userService.createUser(realm);
            subjectId = u.getUuid();
            u = userService.addUser(subjectId, realm, null);

            logger.debug("created subject for identity to " + subjectId);

        }

        // this must exist
        UserEntity user = userService.findUser(subjectId);
        if (user == null) {
            // providers misbehave
            logger.error("resolved subject does not exists");
            throw new AuthenticationServiceException("error processing request");
        }

        // check current authenticated session for match subject
        UserAuthenticationToken currentAuth = null;
        Authentication currentSession = SecurityContextHolder.getContext().getAuthentication();
        if (currentSession != null && currentSession instanceof UserAuthenticationToken) {
            currentAuth = (UserAuthenticationToken) currentSession;
            // also enforce realm match on subject
            if (!subjectId.equals(currentAuth.getSubject().getSubjectId()) ||
                    !realm.equals((currentAuth.getRealm()))) {
                // not the same subject or not same realm, drop
                currentAuth = null;
            }
        }

        /*
         * Login identity:
         * 
         * we ask the IdP to convert the authentication to a valid identity bound to the
         * subject. If desired, the IdP can persist the account <=> subject association
         */

        try {
            // convert to identity for user auth
            // if needed, this will persist an account
            UserIdentity identity = idp.convertIdentity(principal, subjectId);
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

            /*
             * Build complete subject
             * 
             * we got a login identity, update subject
             */

            // always override username with last login
            if (StringUtils.hasText(identity.getAccount().getUsername())) {
                user = userService.updateUser(subjectId, identity.getAccount().getUsername());
            }

            // set login date
            // register additional audit info from request
            Date now = Calendar.getInstance().getTime();
            String ipAddr = null;

            if (webAuthDetails != null) {
                now = new Date(webAuthDetails.getTimestamp());
                ipAddr = webAuthDetails.getRemoteAddress();
            }

            user = userService.updateLogin(subjectId, providerId, now, ipAddr);

            // TODO add audit trail for subjects with login list, keep track of active
            // sessions. Should be event based

            // TODO add concurrent sessions for same subject control

            // convert to subject
            Subject subject = new Subject(subjectId, user.getUsername());

            // fetch global and realm authorities for subject
            // we don't fetch roles from different realms, user has to authenticate again to
            // gain those. Only matching realm and global roles are valid
            List<UserRoleEntity> userRoles = userService.getRoles(subjectId, SystemKeys.REALM_GLOBAL);
            List<UserRoleEntity> realmRoles = userService.getRoles(subjectId, auth.getRealm());

            Collection<GrantedAuthority> authorities = convertUserRoles(userRoles, realmRoles);
            logger.debug("authenticated user granted authorities are " + authorities.toString());

            // clear credentials from token, we don't use them anymore
            auth.eraseCredentials();
            // we also force erase credentials from the request to ensure non reuse
            request.eraseCredentials();
            // also erase credentials from identity, we don't want them in token
            identity.eraseCredentials();

            // we can build the user authentication
            UserAuthenticationToken userAuth = new UserAuthenticationToken(
                    subject, realm,
                    auth,
                    identity, attributeSets,
                    authorities);

            logger.debug("successfully build userAuthentication token for " + userAuth.getSubjectId());
            logger.trace("userAuthentication is " + userAuth.toString());

            // merge userAuth from session here, filters won't have the context
            UserAuthenticationToken result = userAuth;
            // merge only same realm authorities
            if (currentAuth != null && realm.equals(currentAuth.getRealm())) {
                // merge authorities
                Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
                grantedAuthorities.addAll(currentAuth.getAuthorities());
                grantedAuthorities.addAll(userAuth.getAuthorities());

                // current authentication is first, new extends
                result = new UserAuthenticationToken(subject, realm, grantedAuthorities, currentAuth, userAuth);

            }

            // load additional identities from same realm providers
            // fast load, get only idp with persistence
            Collection<IdentityService> idps = providerManager.fetchIdentityServices(realm);
            // ask all providers except the one already used
            for (IdentityService ip : idps) {
                if (!providerId.equals(ip.getProvider())) {
                    Collection<UserIdentity> identities = ip.listIdentities(subjectId);
                    if (identities == null) {
                        // this idp does not support linking
                        continue;
                    }
                    // add to session
                    for (UserIdentity i : identities) {
                        result.getUser().addIdentity(i);
                    }
                }
            }

            // TODO evaluate: load additional attributes from non idp attribute providers
            // this should load all attribute sets ONLY for login identity, others should be
            // fetched when needed

            // set webAuth details matching this request
            result.setWebAuthenticationDetails(webAuthDetails);

            return result;

        } catch (NoSuchUserException e) {
            logger.error("idp could not resolve identity for user " + userId);
            throw new UsernameNotFoundException("no identity for user from provider");
        }

    }

    private Collection<GrantedAuthority> convertUserRoles(List<UserRoleEntity> userRoles,
            List<UserRoleEntity> realmRoles) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        // always grant user role
        authorities.add(new SimpleGrantedAuthority(Config.R_USER));

        for (UserRoleEntity role : userRoles) {
            authorities.add(new SimpleGrantedAuthority(role.getRole()));
        }

        for (UserRoleEntity role : realmRoles) {
            authorities.add(new RealmGrantedAuthority(role.getRealm(), role.getRole()));
        }

        return authorities;
    }

    public boolean supports(Class<?> authentication) {
        // we support only requests with provider ids
        // TODO extend to support lookup providerId or realm in details as parameter map
        return (ProviderWrappedAuthenticationToken.class
                .isAssignableFrom(authentication) ||
                RealmWrappedAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
