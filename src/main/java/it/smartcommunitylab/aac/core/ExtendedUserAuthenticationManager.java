/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.DefaultUserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.core.auth.WrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.service.AttributeProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.identity.IdentityProviderAuthority;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.identity.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.service.UserEntityService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * Authentication manager
 *
 * handles authentication process by dispatching authRequests to specific providers
 * expects all provider to be registered to an external registry
 *
 * we don't want retries, a request should be handled only by the correct provider, or dropped
 *
 */

public class ExtendedUserAuthenticationManager implements AuthenticationManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IdentityProviderAuthorityService identityProviderAuthorityService;
    private final AttributeProviderAuthorityService attributeProviderAuthorityService;
    // TODO replace manager with services for idp and ap
    //    private final AuthorityManager authorityManager;
    private final UserEntityService userService;
    private final SubjectService subjectService;

    private AuthenticationEventPublisher eventPublisher;

    public ExtendedUserAuthenticationManager(
        //            AuthorityManager authorityManager,
        IdentityProviderAuthorityService identityProviderAuthorityService,
        AttributeProviderAuthorityService attributeProviderAuthorityService,
        UserEntityService userService,
        SubjectService subjectService
    ) {
        //        Assert.notNull(authorityManager, "authority manager is required");
        Assert.notNull(identityProviderAuthorityService, "idp authority service is required");
        Assert.notNull(attributeProviderAuthorityService, "attribute provider authority service is required");
        Assert.notNull(userService, "user service is required");
        Assert.notNull(subjectService, "subject service is required");

        this.identityProviderAuthorityService = identityProviderAuthorityService;
        this.attributeProviderAuthorityService = attributeProviderAuthorityService;
        this.userService = userService;
        this.subjectService = subjectService;

        logger.debug("authentication manager created");
    }

    @Autowired
    public void setAuthenticationEventPublisher(AuthenticationEventPublisher eventPublisher) {
        Assert.notNull(eventPublisher, "AuthenticationEventPublisher cannot be null");
        this.eventPublisher = eventPublisher;
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
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
                ProviderWrappedAuthenticationToken providerRequest =
                    (ProviderWrappedAuthenticationToken) authentication;
                String authorityId = providerRequest.getAuthority();
                String providerId = providerRequest.getProvider();

                logger.debug(
                    "authentication token for provider " +
                    String.valueOf(authorityId) +
                    ":" +
                    String.valueOf(providerId)
                );
                logger.trace(String.valueOf(token));

                // validate
                if (!StringUtils.hasText(providerId)) {
                    logger.error("missing or invalid  providerId " + String.valueOf(providerId));
                    throw new ProviderNotFoundException("provider not found");
                }

                IdentityProvider<? extends UserIdentity, ?, ?, ?, ?> idp = null;
                if (StringUtils.hasText(authorityId)) {
                    // fast load
                    idp = fetchIdentityProvider(authorityId, providerId);
                } else {
                    // disabled, we don't want auth request for unloaded idps
                    // TODO handle pending requests on unload
                    //                    // from db
                    //                    idp = authorityManager.findIdentityProvider(providerId);
                }

                if (idp == null) {
                    logger.error("identity provider not found for " + providerId);
                    throw new ProviderNotFoundException("provider not found for " + providerId);
                }

                ExtendedAuthenticationProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> eap =
                    idp.getAuthenticationProvider();
                if (eap == null) {
                    logger.error("auth provider not found for " + providerId);
                    throw new ProviderNotFoundException("provider not found for " + providerId);
                }

                // process with provider, no fallback
                return doAuthenticate(request, eap);
            } else if (request instanceof RealmWrappedAuthenticationToken) {
                // TODO drop support
                RealmWrappedAuthenticationToken realmRequest = (RealmWrappedAuthenticationToken) authentication;
                String authorityId = realmRequest.getAuthority();
                String realm = realmRequest.getRealm();

                logger.debug(
                    "authentication token for realm " + String.valueOf(realm) + ":" + String.valueOf(authorityId)
                );
                logger.trace(String.valueOf(token));

                // validate
                if (!StringUtils.hasText(realm)) {
                    logger.error("missing or invalid  realm " + String.valueOf(realm));
                    throw new ProviderNotFoundException("provider not found");
                }

                // since we don't have an authority we ask all idps to process, and keep the
                // first not null result
                Collection<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> providers = Collections.emptyList();

                if (StringUtils.hasText(authorityId)) {
                    // direct load
                    providers = fetchIdentityProviders(authorityId, realm);
                } else {
                    // load all
                    providers =
                        identityProviderAuthorityService
                            .getAuthorities()
                            .stream()
                            .flatMap(a -> a.getProvidersByRealm(realm).stream())
                            .collect(Collectors.toList());
                }

                UserAuthentication result = null;

                // TODO rework loop
                for (IdentityProvider<? extends UserIdentity, ?, ?, ?, ?> idp : providers) {
                    ExtendedAuthenticationProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> eap =
                        idp.getAuthenticationProvider();
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
        } catch (AuthenticationException e) {
            auditException(e, authentication);

            throw e;
        }
    }

    /*
     * Attemp authentication, returns null if request is not supported or if invalid
     */
    protected UserAuthentication attempAuthenticate(
        WrappedAuthenticationToken request,
        ExtendedAuthenticationProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> provider
    ) throws AuthenticationException {
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
            logger.error("provider authentication error:" + ae.getMessage());
            throw ae;
        }
    }

    /*
     * Perform authentication, throws error
     */
    protected UserAuthentication doAuthenticate(
        WrappedAuthenticationToken request,
        ExtendedAuthenticationProvider<? extends UserAuthenticatedPrincipal, ? extends UserAccount> eap
    ) throws AuthenticationException {
        /*
         * Extended authentication:
         *
         * process auth request via specific provider and obtain a valid userAuth
         * containing a principal
         */
        AbstractAuthenticationToken token = request.getAuthenticationToken();

        // check if request is supported
        if (!eap.supports(token.getClass())) {
            logger.error("token is not supported by the requested provider");
            throw new BadCredentialsException("invalid authentication request");
        }

        // perform extended authentication
        logger.debug("perform authentication via provider");
        ExtendedAuthenticationToken auth = eap.authenticate(token);
        logger.debug("received authentication token from provider");

        if (auth == null) {
            logger.error("null authentication result");
            throw new BadCredentialsException("invalid authentication request");
        }

        return createSuccessAuthentication(request, auth);
    }

    protected UserAuthentication createSuccessAuthentication(
        WrappedAuthenticationToken request,
        ExtendedAuthenticationToken auth
    ) throws AuthenticationException {
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

        // authentication provider should yield a proper local id (addressable)
        String principalId = principal.getPrincipalId();
        logger.debug("authenticated principalId is " + principalId);

        // fetch identity provider for the given account
        // fast load skipping db
        IdentityProvider<?, ?, ?, ?, ?> idp = fetchIdentityProvider(authorityId, providerId);
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
        // TODO handle subject as UserSubject
        SubjectResolver<? extends UserAccount> resolver = idp.getSubjectResolver();
        Subject s = resolver.resolveByPrincipalId(principalId);
        if (s != null) {
            // principal is associated to a persisted account linked to a user
            subjectId = s.getSubjectId();
            logger.debug("resolved user for identity to " + subjectId);
        }

        if (subjectId == null) {
            // new or non-persisted account
            // let idp resolver try with identifier(username)
            s = resolver.resolveByUsername(principal.getUsername());
            if (s != null) {
                subjectId = s.getSubjectId();
                logger.debug("resolved user for identity to " + subjectId);
            }
        }

        if (subjectId == null && principal.isEmailVerified()) {
            // new or non-persisted account for idp, non resolvable
            // fallback to other idps from the same realm via verified emailAddress
            Collection<IdentityProvider<?, ?, ?, ?, ?>> idps = fetchIdentityProviders(realm);
            // first result is ok
            for (IdentityProvider<?, ?, ?, ?, ?> i : idps) {
                Subject ss = i.getSubjectResolver().resolveByEmailAddress(principal.getEmailAddress());
                if (ss != null) {
                    subjectId = ss.getSubjectId();
                    logger.debug("linked subject for identity to " + subjectId);
                    break;
                }
            }
        }

        if (subjectId == null) {
            // generate a new subject, always persisted
            UserEntity u = userService.createUser(realm);
            subjectId = u.getUuid();
            try {
                u = userService.addUser(subjectId, realm, null, null);
            } catch (AlreadyRegisteredException e) {
                // something wrong, stop
                logger.error("error creating new userfor subject {}", String.valueOf(subjectId));
                throw new AuthenticationServiceException("error processing request");
            }

            logger.debug("created subject for identity to " + subjectId);
        }

        // this must exist
        UserEntity user = userService.findUser(subjectId);
        if (user == null) {
            // providers misbehave
            logger.error("resolved subject does not exists");
            throw new AuthenticationServiceException("error processing request");
        }

        // check user status
        if (user.isBlocked()) {
            throw new LockedException("subject is blocked");
        }
        if (user.isInactive()) {
            throw new DisabledException("subject is inactive");
        }
        if (user.isExpired()) {
            throw new AccountExpiredException("subject account is expired");
        }

        // check current authenticated session for match subject
        UserAuthentication currentAuth = null;
        Authentication currentSession = SecurityContextHolder.getContext().getAuthentication();
        if (currentSession != null && currentSession instanceof UserAuthentication) {
            currentAuth = (UserAuthentication) currentSession;
            // also enforce realm match on subject
            if (!subjectId.equals(currentAuth.getSubject().getSubjectId()) || !realm.equals((currentAuth.getRealm()))) {
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
            if (!subjectId.equals(identity.getUserId())) {
                // provider misbehave or corruption
                logger.error("userId does not match");
                throw new AuthenticationServiceException("error processing request");
            }

            // make sure user is not locked
            if (identity.getAccount().isLocked()) {
                // provider misbehave
                logger.error("account is locked");
                throw new AuthenticationServiceException("error processing request");
            }

            // TODO attribute providers outside idp
            //            // fetch attributes
            //            // could be a no-op, we expect attributes mapped to the shared schema
            //            Collection<UserAttributes> attributeSets = idp.getAttributeProvider()
            //                    .convertAttributes(identity.getAttributes());
            Collection<UserAttributes> attributeSets = Collections.emptyList();

            /*
             * Build complete subject
             *
             * we got a login identity, update subject
             */

            // always override username with last login
            if (StringUtils.hasText(identity.getAccount().getUsername())) {
                user =
                    userService.updateUser(
                        subjectId,
                        identity.getAccount().getUsername(),
                        identity.getAccount().getEmailAddress()
                    );
            }

            if (identity.getAccount().isEmailVerified() && !user.isEmailVerified()) {
                user = userService.verifyEmail(subjectId, identity.getAccount().getEmailAddress());
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
            // fetch subject from service
            //            Subject subject = new Subject(subjectId, realm, user.getUsername(), SystemKeys.RESOURCE_USER);
            Subject subject = subjectService.getSubject(subjectId);
            // update
            subject = subjectService.updateSubject(subjectId, user.getUsername());

            // fetch global and realm authorities for subject
            List<GrantedAuthority> userAuthorities = subjectService.getAuthorities(subjectId);

            Set<GrantedAuthority> authorities = new HashSet<>();
            // always grant user role
            authorities.add(new SimpleGrantedAuthority(Config.R_USER));
            authorities.addAll(userAuthorities);

            logger.debug("authenticated user granted authorities are " + authorities.toString());

            // clear credentials from token, we don't use them anymore
            auth.eraseCredentials();
            // we also force erase credentials from the request to ensure non reuse
            request.eraseCredentials();
            // also erase credentials from identity, we don't want them in token
            if (identity instanceof CredentialsContainer) {
                ((CredentialsContainer) identity).eraseCredentials();
            }

            // we can build the user authentication
            DefaultUserAuthenticationToken userAuth = new DefaultUserAuthenticationToken(
                subject,
                realm,
                auth,
                identity,
                attributeSets,
                authorities
            );

            // set webAuth details matching this request
            userAuth.setWebAuthenticationDetails(webAuthDetails);

            // load additional attributes from providers
            UserDetails userDetails = userAuth.getUser();
            Collection<AttributeProvider<?, ?>> attributeProviders = attributeProviderAuthorityService
                .getAuthorities()
                .stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream())
                .collect(Collectors.toList());

            for (AttributeProvider<?, ?> ap : attributeProviders) {
                // try to fetch attributes, don't stop authentication on errors
                // attributes from aps are optional by definition
                try {
                    Collection<UserAttributes> attrs = ap.convertPrincipalAttributes(principal, subjectId);
                    if (attrs != null) {
                        attrs.forEach(a -> userDetails.addAttributeSet(a));
                    }
                } catch (RuntimeException e) {
                    logger.error("error loading attributes with provider " + ap.getProvider() + ": " + e.getMessage());
                }
            }

            logger.debug("successfully build userAuthentication token for " + userAuth.getSubjectId());
            logger.trace("userAuthentication is " + userAuth.toString());

            // audit trail for request
            if (eventPublisher != null) {
                // publish as is, listener will resolve realm
                eventPublisher.publishAuthenticationSuccess(userAuth);
            }

            // merge userAuth from session here, filters won't have the context
            DefaultUserAuthenticationToken result = userAuth;
            // merge only same realm authorities
            if (currentAuth != null && realm.equals(currentAuth.getRealm())) {
                // merge authorities
                Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
                grantedAuthorities.addAll(currentAuth.getAuthorities());
                grantedAuthorities.addAll(userAuth.getAuthorities());

                // current authentication is first, new extends
                result = new DefaultUserAuthenticationToken(subject, realm, grantedAuthorities, currentAuth, userAuth);
            }

            // load additional identities from same realm providers
            // fast load, get only idp with persistence
            Collection<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> idps = fetchIdentityProviders(realm);
            // ask all providers except the one already used
            for (IdentityProvider<? extends UserIdentity, ?, ?, ?, ?> ip : idps) {
                if (!providerId.equals(ip.getProvider())) {
                    Collection<? extends UserIdentity> identities = ip.listIdentities(subjectId, true);
                    if (identities == null) {
                        // this idp does not support linking
                        continue;
                    }
                    // add to session
                    for (UserIdentity i : identities) {
                        // only add new identities
                        result.getUser().addIdentity(i, false);
                    }
                }
            }

            // TODO evaluate: load additional attributes from non idp attribute providers
            // this should load all attribute sets ONLY for login identity, others should be
            // fetched when needed

            // set webAuth details matching this request
            result.setWebAuthenticationDetails(webAuthDetails);

            // audit trail for result
            // DISABLED, for now audit single login events
            //            if (eventPublisher != null) {
            //                // publish as is, listener will resolve realm
            //                eventPublisher.publishAuthenticationSuccess(result);
            //            }

            return result;
        } catch (NoSuchUserException | NoSuchSubjectException | RegistrationException e) {
            logger.error("idp could not resolve identity for user " + subjectId);
            throw new UsernameNotFoundException("no identity for user from provider");
        }
    }

    //    private Collection<GrantedAuthority> convertUserRoles(List<UserRoleEntity> userRoles,
    //            List<UserRoleEntity> realmRoles) {
    //        Set<GrantedAuthority> authorities = new HashSet<>();
    //        // always grant user role
    //        authorities.add(new SimpleGrantedAuthority(Config.R_USER));
    //
    //        for (UserRoleEntity role : userRoles) {
    //            authorities.add(new SimpleGrantedAuthority(role.getRole()));
    //        }
    //
    //        for (UserRoleEntity role : realmRoles) {
    //            authorities.add(new RealmGrantedAuthority(role.getRealm(), role.getRole()));
    //        }
    //
    //        return authorities;
    //    }

    public boolean supports(Class<?> authentication) {
        // we support only requests with provider ids
        // TODO extend to support lookup providerId or realm in details as parameter map
        return (
            ProviderWrappedAuthenticationToken.class.isAssignableFrom(authentication) ||
            RealmWrappedAuthenticationToken.class.isAssignableFrom(authentication)
        );
    }

    private void auditException(AuthenticationException ex, Authentication auth) {
        if (eventPublisher != null) {
            // publish failure as is, will be sent to global audit
            // on listener we infer realm from auth
            eventPublisher.publishAuthenticationFailure(ex, auth);
        }
    }

    /*
     * Idp helpers
     *
     * TODO refactor and remove
     */
    private IdentityProvider<? extends UserIdentity, ?, ?, ?, ?> fetchIdentityProvider(
        String authorityId,
        String providerId
    ) {
        // lookup in authority
        IdentityProviderAuthority<?, ?, ?, ?> ia = identityProviderAuthorityService.findAuthority(authorityId);
        if (ia == null) {
            return null;
        }
        try {
            return ia.getProvider(providerId);
        } catch (NoSuchProviderException e) {
            return null;
        }
    }

    private Collection<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> fetchIdentityProviders(
        String authorityId,
        String realm
    ) {
        List<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> providers = new ArrayList<>();
        // lookup in authority
        IdentityProviderAuthority<?, ?, ?, ?> ia = identityProviderAuthorityService.findAuthority(authorityId);
        if (ia != null) {
            providers.addAll(ia.getProvidersByRealm(realm));
        }

        return providers;
    }

    private Collection<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> fetchIdentityProviders(String realm) {
        List<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> providers = new ArrayList<>();

        for (IdentityProviderAuthority<?, ?, ?, ?> ia : identityProviderAuthorityService.getAuthorities()) {
            providers.addAll(ia.getProvidersByRealm(realm));
        }

        return providers;
    }
    //    private void auditSuccess(UserAuthentication auth) {
    //        if (eventPublisher != null) {
    //            // publish as is, listener will resolve realm
    //            eventPublisher.publishAuthenticationSuccess(auth);
    //        }
    //    }

}
