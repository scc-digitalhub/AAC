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

package it.smartcommunitylab.aac.bootstrap;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.attributes.service.AttributeProviderService;
import it.smartcommunitylab.aac.base.service.AbstractConfigurableProviderService;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.service.IdentityProviderService;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientAppService;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.password.auth.UsernamePasswordAuthenticationToken;
import it.smartcommunitylab.aac.password.model.InternalUserPassword;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServiceClaim;
import it.smartcommunitylab.aac.services.ServiceScope;
import it.smartcommunitylab.aac.services.ServicesManager;
import it.smartcommunitylab.aac.services.ServicesService;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.templates.service.TemplateProviderService;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Component
@Transactional
public class AACBootstrap {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${bootstrap.apply}")
    private boolean apply;

    @Value("${bootstrap.file}")
    private String source;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.roles}")
    private String[] adminRoles;

    @Autowired
    private BootstrapConfig config;

    @Autowired
    private RealmService realmService;

    @Autowired
    private ServicesService serviceService;

    @Autowired
    private ServicesManager serviceManager;    

    @Autowired
    private UserEntityService userEntityService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private TemplateProviderService templateProviderService;

    @Autowired
    private SpaceRoleService roleService;

    @Autowired
    private it.smartcommunitylab.aac.accounts.service.UserAccountService userAccountService;

    @Autowired
    private it.smartcommunitylab.aac.credentials.service.UserCredentialsService userCredentialsService;

    @Autowired
    private UserAccountService<InternalUserAccount> internalUserAccountService;

    //    @Autowired
    //    private UserAccountService<OIDCUserAccount> oidcUserAccountService;
    //
    //    @Autowired
    //    private UserAccountService<SamlUserAccount> samlUserAccountService;
    //
    //    @Autowired
    //    private UserCredentialsService<WebAuthnUserCredential> webAuthnUserCredentialsService;

    @Autowired
    private UserCredentialsService<InternalUserPassword> internalUserPasswordService;

    @Autowired
    private OAuth2ClientAppService clientAppService;

    @Autowired
    private ResourceEntityService resourceService;

    @Autowired
    private PasswordHash hasher;
    
    
    
    @Autowired
    private SearchableApprovalStore approvalStore;

    //    @EventListener
    public void onApplicationEvent(ApplicationStartedEvent event) {
        // build a security context as admin to bootstrap configs
        // DISABLED
        // do note this works ONLY for asynch events which execute in their own thread
        // AND breaks with parallelStream (which uses a shared forkPool)!
        //        SecurityContext context = initContext(adminUsername);

        try {
            bootstrap();
        } finally {
            //            if (context != null) {
            //                SecurityContextHolder.clearContext();
            //            }
        }
    }

    public void bootstrap() {
        try {
            // base initialization
            logger.debug("application bootstrap");

            bootstrapSystemProviders();

            // bootstrap admin account
            bootstrapAdminUser(adminUsername, adminEmail, adminPassword);

            // validate and remediate realm provider configs
            bootstrapRealmProviders();
        } catch (Exception e) {
            logger.error("error bootstrapping: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private SecurityContext initContext(String username) {
        // assign authorities
        Collection<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority(Config.R_ADMIN)
        );

        // use an auth token
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
            username,
            null,
            authorities
        );

        // mock a security context with token
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);

        return context;
    }

    private void bootstrapSystemProviders() {
        // bootstrap only idps
        bootstrapConfigurableProviders(identityProviderService, SystemKeys.REALM_SYSTEM);
    }

    private void bootstrapRealmProviders() {
        // load all realm providers from storage
        Collection<Realm> realms = realmService.listUserRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default thread-pool, loading should be
        // thread-safe
        // do note that parallel uses a shared fork pool, without any security context
        realms
            .parallelStream()
            .forEach(realm -> {
                bootstrapRealmProviders(realm.getSlug());
            });
    }

    private void bootstrapRealmProviders(String slug) {
        // load providers in order, single thread
        bootstrapConfigurableProviders(templateProviderService, slug);
        bootstrapConfigurableProviders(identityProviderService, slug);
        bootstrapConfigurableProviders(attributeProviderService, slug);
    }

    private void bootstrapConfigurableProviders(AbstractConfigurableProviderService<?, ?> service, String realm) {
        // register in parallel via thread-pool
        service
            .listProviders(realm)
            .parallelStream()
            .forEach(cp -> {
                if (cp.isEnabled()) {
                    try {
                        service.registerProvider(cp.getProvider());
                    } catch (Exception e) {
                        logger.error(
                            "error registering provider {}:{} for realm {}: {}",
                            cp.getAuthority(),
                            cp.getProvider(),
                            cp.getRealm(),
                            e.getMessage()
                        );

                        if (logger.isTraceEnabled()) {
                            e.printStackTrace();
                        }
                    }
                }
            });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void bootstrapAdminUser(String username, String email, String password)
        throws RegistrationException, NoSuchUserException {
        // create admin as superuser for system
        logger.debug("create internal admin user for realm system", username);

        String realm = SystemKeys.REALM_SYSTEM;
        String repositoryId = realm;

        String userId = null;
        UserEntity user = null;

        InternalUserAccount account = internalUserAccountService.findAccountById(repositoryId, username);
        if (account == null) {
            // register as new user
            userId = userEntityService.createUser(realm).getUuid();
            user = userEntityService.addUser(userId, realm, username, email);

            // register account
            account = new InternalUserAccount(repositoryId, realm, null);
            account.setProvider(realm);
            account.setUsername(username);
            account.setUserId(userId);
            account.setEmail(email);
            account.setStatus(SubjectStatus.ACTIVE.getValue());
            account.setConfirmed(true);
            account = internalUserAccountService.addAccount(repositoryId, username, account);
        } else {
            userId = account.getUserId();

            // update account
            account.setEmail(email);

            // override confirm
            account.setConfirmed(true);
            account.setConfirmationDeadline(null);
            account.setConfirmationKey(null);

            // set as active
            account.setStatus(SubjectStatus.ACTIVE.getValue());

            account = internalUserAccountService.updateAccount(repositoryId, username, account);

            // update user
            user = userEntityService.updateUser(userId, username, email);
        }

        try {
            // ensure user is active
            user = userEntityService.activateUser(userId);

            // always reset password
            internalUserPasswordService.deleteAllCredentialsByUser(repositoryId, userId);
            InternalUserPassword pass = new InternalUserPassword(realm, UUID.randomUUID().toString());
            pass.setProvider(repositoryId);
            // pass.setUsername(username);
            pass.setUserId(account.getUserId());
            pass.setPassword(hasher.createHash(password));
            pass.setChangeOnFirstAccess(false);
            internalUserPasswordService.addCredentials(repositoryId, pass.getId(), pass);

            // assign authorities to subject
            String subjectId = user.getUuid();

            // at minimum we set ADMIN+DEV global, and ADMIN+DEV in SYSTEM realm
            Set<Map.Entry<String, String>> roles = new HashSet<>();
            roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, Config.R_ADMIN));
            roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, Config.R_DEVELOPER));
            roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_SYSTEM, Config.R_ADMIN));
            roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_SYSTEM, Config.R_DEVELOPER));
            // admin roles are global, ie they are valid for any realm
            for (String role : adminRoles) {
                roles.add(new AbstractMap.SimpleEntry<>(SystemKeys.REALM_GLOBAL, role));
            }

            subjectService.addAuthorities(subjectId, roles);

            // set minimal space roles
            Collection<SpaceRole> spaceRoles = roleService.getRoles(subjectId);
            if (spaceRoles.isEmpty()) {
                roleService.addRole(subjectId, null, "", Config.R_PROVIDER);
            }

            logger.debug("admin user id {}", userId);
            logger.debug("admin user {}", user.toString());
            logger.debug("admin account {}", account.toString());
        } catch (NoSuchUserException | NoSuchSubjectException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("error updating admin account: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bootstrapConfig() {
        // custom bootstrap
        if (!apply) {
            logger.debug("bootstrap disabled by config");
            return;
        }

        logger.debug("application bootstrap");

        // read configuration
        if (config == null) {
            logger.error("error loading config");
            return;
        }

        /*
         * Realms creation
         */
        logger.debug("bootstrap realms from config");
        if (config.getRealms() == null) {
            return;
        }

        config
            .getRealms()
            .parallelStream()
            .forEach(rc -> {
                if (rc.getRealm() == null || !StringUtils.hasText(rc.getRealm().getSlug())) {
                    // we ask id to be provided otherwise we would create a new one every time
                    logger.error("error creating realm, missing slug");
                    return;
                }

                String slug = rc.getRealm().getSlug();

                try {
                    Realm r = rc.getRealm();
                    logger.debug("create realm {}", r.getSlug());

                    Realm realm = realmService.findRealm(r.getSlug());
                    if (realm == null) {
                        logger.debug("add realm {}", r.getSlug());

                        realm =
                            realmService.addRealm(r.getSlug(), r.getName(), r.getEmail(), r.isEditable(), r.isPublic());
                    } else {
                        logger.debug("update realm {}", r.getSlug());

                        // config maps
                        // TODO put in dedicated providers + config
                        realm =
                            realmService.updateRealm(
                                r.getSlug(),
                                r.getName(),
                                r.getEmail(),
                                r.isEditable(),
                                r.isPublic(),
                                r.getOAuthConfiguration() != null ? r.getOAuthConfiguration().getConfiguration() : null,
                                r.getTosConfiguration() != null ? r.getTosConfiguration().getConfiguration() : null,
                                r.getLocalizationConfiguration() != null ? r.getLocalizationConfiguration().getConfiguration() : null,
                                r.getTemplatesConfiguration() != null ? r.getTemplatesConfiguration().getConfiguration() : null
                            );
                    }

                    /*
                     * IdP
                     */
                    if (rc.getIdentityProviders() != null) {
                        rc
                            .getIdentityProviders()
                            .forEach(cp -> {
                                logger.debug("create identity provider for realm {}", String.valueOf(cp.getRealm()));

                                // validate realm match
                                if (StringUtils.hasText(cp.getRealm()) && !slug.equals(cp.getRealm())) {
                                    logger.error("error creating identity provider, realm mismatch");
                                    return;
                                }

                                // enforce realm
                                cp.setRealm(slug);

                                // validate id
                                if (!StringUtils.hasText(cp.getProvider())) {
                                    // we ask id to be provided otherwise we would create a new one every time
                                    logger.error("error creating identity provider, missing id");
                                    throw new IllegalArgumentException("missing id");
                                }

                                //by default imported providers are enabled
                                //TODO model enabled as Boolean with null default
                                cp.setEnabled(true);

                                if (logger.isTraceEnabled()) {
                                    logger.trace("provider: {}", String.valueOf(cp));
                                }

                                try {
                                    // add or update via service
                                    String providerId = cp.getProvider();
                                    ConfigurableIdentityProvider p = identityProviderService.findProvider(providerId);
                                    if (p == null) {
                                        logger.debug(
                                            "add identity provider {} for realm {}",
                                            providerId,
                                            String.valueOf(cp.getRealm())
                                        );

                                        p = identityProviderService.addProvider(slug, cp);
                                    } else {
                                        // check again realm match over existing
                                        if (!slug.equals(p.getRealm())) {
                                            logger.error("error creating identity provider, realm mismatch");
                                            return;
                                        }

                                        logger.debug(
                                            "update identity provider {} for realm {}",
                                            providerId,
                                            String.valueOf(cp.getRealm())
                                        );

                                        if (p.isEnabled()) {
                                            identityProviderService.unregisterProvider(providerId);
                                        }

                                        p = identityProviderService.updateProvider(providerId, cp);
                                    }

                                    if (p.isEnabled()) {
                                        // register
                                        identityProviderService.registerProvider(p.getProvider());
                                    }
                                } catch (
                                    RegistrationException
                                    | MethodArgumentNotValidException
                                    | NoSuchRealmException
                                    | NoSuchProviderException
                                    | NoSuchAuthorityException e
                                ) {
                                    logger.error(
                                        "error creating identity provider " +
                                        String.valueOf(cp.getProvider()) +
                                        ": " +
                                        e.getMessage()
                                    );
                                }
                            });
                    }

                    /*
                     * Attribute providers
                     */
                    if (rc.getAttributeProviders() != null) {
                        rc
                            .getAttributeProviders()
                            .forEach(cp -> {
                                logger.debug("create attribute provider for realm {}", String.valueOf(cp.getRealm()));

                                // validate realm match
                                if (StringUtils.hasText(cp.getRealm()) && !slug.equals(cp.getRealm())) {
                                    logger.error("error creating attribute provider, realm mismatch");
                                    return;
                                }

                                // enforce realm
                                cp.setRealm(slug);

                                // validate id
                                if (!StringUtils.hasText(cp.getProvider())) {
                                    // we ask id to be provided otherwise we would create a new one every time
                                    logger.error("error creating attribute provider, missing id");
                                    throw new IllegalArgumentException("missing id");
                                }

                                //by default imported providers are enabled
                                //TODO model enabled as Boolean with null default
                                cp.setEnabled(true);

                                if (logger.isTraceEnabled()) {
                                    logger.trace("provider: {}", String.valueOf(cp));
                                }

                                try {
                                    // add or update via service
                                    String providerId = cp.getProvider();
                                    ConfigurableAttributeProvider p = attributeProviderService.findProvider(providerId);
                                    if (p == null) {
                                        logger.debug(
                                            "add attribute provider {} for realm {}",
                                            providerId,
                                            String.valueOf(cp.getRealm())
                                        );
                                        p = attributeProviderService.addProvider(slug, cp);
                                    } else {
                                        // check again realm match over existing
                                        if (!slug.equals(p.getRealm())) {
                                            logger.error("error creating attribute provider, realm mismatch");
                                            return;
                                        }

                                        logger.debug(
                                            "update attribute provider {} for realm {}",
                                            providerId,
                                            String.valueOf(cp.getRealm())
                                        );

                                        if (p.isEnabled()) {
                                            attributeProviderService.unregisterProvider(providerId);
                                        }

                                        p = attributeProviderService.updateProvider(providerId, cp);
                                    }

                                    if (p.isEnabled()) {
                                        // register
                                        attributeProviderService.registerProvider(p.getProvider());
                                    }
                                } catch (
                                    RegistrationException
                                    | MethodArgumentNotValidException
                                    | NoSuchRealmException
                                    | NoSuchProviderException
                                    | NoSuchAuthorityException e
                                ) {
                                    logger.error(
                                        "error creating attribute provider " +
                                        String.valueOf(cp.getProvider()) +
                                        ": " +
                                        e.getMessage()
                                    );
                                }
                            });
                    }

                    /*
                     * Template provider
                     */
                    if (rc.getTemplates() != null) {
                        ConfigurableTemplateProvider cp = rc.getTemplates();
                        logger.debug("create template provider for realm {}", String.valueOf(cp.getRealm()));

                        // validate realm match
                        if (StringUtils.hasText(cp.getRealm()) && !slug.equals(cp.getRealm())) {
                            logger.error("error creating template provider, realm mismatch");
                        } else {
                            // enforce realm
                            cp.setRealm(slug);

                            if (logger.isTraceEnabled()) {
                                logger.trace("provider: {}", String.valueOf(cp));
                            }
                            try {
                                // add or update via service
                                String providerId = cp.getProvider();
                                ConfigurableTemplateProvider p = templateProviderService.findProvider(providerId);
                                if (p == null) {
                                    logger.debug(
                                        "add template provider {} for realm {}",
                                        providerId,
                                        String.valueOf(cp.getRealm())
                                    );

                                    p = templateProviderService.addProvider(slug, cp);
                                } else {
                                    // check again realm match over existing
                                    if (!slug.equals(p.getRealm())) {
                                        logger.error("error creating template provider, realm mismatch");
                                        return;
                                    }

                                    logger.debug(
                                        "update template provider {} for realm {}",
                                        providerId,
                                        String.valueOf(cp.getRealm())
                                    );

                                    templateProviderService.unregisterProvider(providerId);
                                    p = templateProviderService.updateProvider(providerId, cp);
                                }

                                // register
                                templateProviderService.registerProvider(p.getProvider());
                            } catch (
                                RegistrationException
                                | NoSuchRealmException
                                | NoSuchProviderException
                                | NoSuchAuthorityException e
                            ) {
                                logger.error(
                                    "error creating template provider " +
                                    String.valueOf(cp.getProvider()) +
                                    ": " +
                                    e.getMessage()
                                );
                            }
                        }
                    }

                    /*
                     * Services
                     */
                    if (rc.getServices() != null) {
                        // if (false) {
                        rc
                            .getServices()
                            .forEach(s -> {
                                logger.debug("create service for realm {}", String.valueOf(s.getRealm()));

                                // validate realm match
                                if (StringUtils.hasText(s.getRealm()) && !slug.equals(s.getRealm())) {
                                    logger.error("error creating service, realm mismatch");
                                    return;
                                }

                                // enforce realm
                                s.setRealm(slug);

                                // validate id
                                if (!StringUtils.hasText(s.getServiceId())) {
                                    // we ask id to be provided otherwise we create a new one every time
                                    logger.error("error creating service, missing serviceId");
                                    throw new IllegalArgumentException("missing serviceId");
                                }

                                // validate namespace
                                if (!StringUtils.hasText(s.getNamespace())) {
                                    logger.error("error creating service, missing namespace");
                                    throw new IllegalArgumentException("missing namespace");
                                }

                                if (logger.isTraceEnabled()) {
                                    logger.trace("service: {}", String.valueOf(s));
                                }

                                try {
                                    // add or update via service
                                    String id = s.getServiceId();
                                    Service service = serviceService.findService(id);
                                    if (service == null) {
                                        logger.debug("add service {} for realm {}", id, String.valueOf(s.getRealm()));

                                        service =
                                            serviceService.addService(
                                                s.getRealm(),
                                                id,
                                                s.getNamespace(),
                                                s.getName(),
                                                s.getDescription(),
                                                s.getClaimMapping()
                                            );

                                    } else {
                                        // check again realm match over existing
                                        if (!slug.equals(service.getRealm())) {
                                            logger.error("error creating service, realm mismatch");
                                            return;
                                        }

                                        logger.debug(
                                            "update service {} for realm {}",
                                            id,
                                            String.valueOf(s.getRealm())
                                        );

                                        service = serviceService.updateService(id, s.getName(), s.getDescription(), s.getClaimMapping());
                                    }                                    

                                    // related
                                    Collection<ServiceScope> scopes = s.getScopes();
                                    Set<ServiceScope> serviceScopes = new HashSet<>();

                                    if (scopes != null && !scopes.isEmpty()) {
                                        for (ServiceScope sc : scopes) {
                                            sc.setServiceId(id);

                                            ServiceScope ss =  serviceService.findScope(id, sc.getScope());
                                            if(ss == null) {
                                                //add
                                                ss = serviceService.addScope(
                                                    id,
                                                    sc.getScope(),
                                                    sc.getName(),
                                                    sc.getDescription(),
                                                    sc.getType(),
                                                    sc.getClaims(),
                                                    sc.getApprovalRoles(),
                                                    sc.getApprovalSpaceRoles(),
                                                    sc.getApprovalFunction(),
                                                    sc.isApprovalRequired(),
                                                    sc.isApprovalAny()
                                                );
                                            } else {
                                                //update
                                                ss = serviceService.updateScope(
                                                    id,
                                                    sc.getScope(),
                                                    sc.getName(),
                                                    sc.getDescription(),
                                                    sc.getType(),
                                                    sc.getClaims(),
                                                    sc.getApprovalRoles(),
                                                    sc.getApprovalSpaceRoles(),
                                                    sc.getApprovalFunction(),
                                                    sc.isApprovalRequired(),
                                                    sc.isApprovalAny()
                                                );
                                            }

                                            serviceScopes.add(ss);
                                        }
                                    }
                                    s.setScopes(serviceScopes);

                                    Collection<ServiceClaim> claims = s.getClaims();
                                    Set<ServiceClaim> serviceClaims = new HashSet<>();
                                    if (claims != null && !claims.isEmpty()) {
                                        for (ServiceClaim sc : claims) {
                                            sc.setServiceId(id);

                                            ServiceClaim ss = serviceService.findClaim(id, sc.getKey());
                                            if(ss == null) {
                                                //add
                                                ss = serviceService.addClaim(
                                                    id,
                                                    sc.getKey(),
                                                    sc.getName(),
                                                    sc.getDescription(),
                                                    sc.getType(),
                                                    sc.isMultiple()
                                                );
                                            } else {
                                                //update
                                                ss = serviceService.updateClaim(
                                                    id,
                                                    sc.getKey(),
                                                    sc.getName(),
                                                    sc.getDescription(),
                                                    sc.getType(),
                                                    sc.isMultiple()
                                                );
                                            }

                                            serviceClaims.add(ss);
                                        }
                                    }

                                    s.setClaims(serviceClaims);
                                
                                    
                                } catch (RegistrationException | NoSuchServiceException | NoSuchScopeException | NoSuchClaimException e) {
                                    logger.error(
                                        "error creating service " +
                                        String.valueOf(s.getServiceId()) +
                                        ": " +
                                        e.getMessage()
                                    );
                                }
                            });
                    }

                    /*
                     * ClientApp
                     *
                     */
                    if (rc.getClientApps() != null) {
                        rc
                            .getClientApps()
                            .forEach(app -> {
                                logger.debug("create client app for realm {}", String.valueOf(app.getRealm()));

                                // validate realm match
                                if (StringUtils.hasText(app.getRealm()) && !slug.equals(app.getRealm())) {
                                    logger.error("error creating client, realm mismatch");
                                    return;
                                }

                                // enforce realm
                                app.setRealm(slug);

                                if (!StringUtils.hasText(app.getClientId())) {
                                    // we ask id to be provided otherwise we create a new one every time
                                    logger.error("error creating client, missing clientId");
                                    throw new IllegalArgumentException("missing clientId");
                                }

                                // support only oauth2 for now
                                if (!SystemKeys.CLIENT_TYPE_OAUTH2.equals(app.getType())) {
                                    logger.error("unsupported client type {}", String.valueOf(app.getType()));
                                    return;
                                }

                                if (logger.isTraceEnabled()) {
                                    logger.trace("app: {}", String.valueOf(app));
                                }

                                try {
                                    String clientId = app.getClientId();
                                    ClientApp client = clientAppService.findClient(clientId);

                                    if (client == null) {
                                        logger.debug(
                                            "add client app {} for realm {}",
                                            clientId,
                                            String.valueOf(app.getRealm())
                                        );

                                        client = clientAppService.registerClient(app.getRealm(), app);
                                    } else {
                                        // check again realm match over existing
                                        if (!slug.equals(client.getRealm())) {
                                            logger.error("error creating client app, realm mismatch");
                                            return;
                                        }

                                        logger.debug(
                                            "update client app {} for realm {}",
                                            clientId,
                                            String.valueOf(app.getRealm())
                                        );

                                        client = clientAppService.updateClient(app.getClientId(), app);
                                    }
                                } catch (NoSuchClientException e) {
                                    logger.error(
                                        "error creating client app " +
                                        String.valueOf(app.getClientId()) +
                                        ": " +
                                        e.getMessage()
                                    );
                                }
                            });
                    }

                    /*
                     * User Accounts
                     *
                     * TODO use User model with roles+authorities+groups
                     */
                    if (rc.getUsers() != null) {
                        rc
                            .getUsers()
                            .forEach(u -> {
                                // validate authority
                                if (!StringUtils.hasText(u.getAuthority())) {
                                    logger.error("error creating user, invalid authority");
                                    return;
                                }

                                logger.debug(
                                    "create {} user for realm {}",
                                    String.valueOf(u.getAuthority()),
                                    String.valueOf(u.getRealm())
                                );

                                // validate realm match
                                if (StringUtils.hasText(u.getRealm()) && !slug.equals(u.getRealm())) {
                                    logger.error("error creating user, realm mismatch");
                                    return;
                                }

                                // enforce realm
                                u.setRealm(slug);

                                // validate id
                                if (!StringUtils.hasText(u.getAccountId())) {
                                    // we ask id to be provided otherwise we would create a new one every time
                                    logger.error("error creating user, missing id");
                                    throw new IllegalArgumentException("missing id");
                                }

                                if (logger.isTraceEnabled()) {
                                    logger.trace(
                                        "{} user account: {}",
                                        String.valueOf(u.getAuthority()),
                                        String.valueOf(u)
                                    );
                                }

                                try {
                                    // add or update via service
                                    String id = u.getAccountId();
                                    String providerId = u.getProvider();

                                    // user entity
                                    String userId = u.getUserId();
                                    UserEntity user = null;
                                    if (StringUtils.hasText(userId)) {
                                        user = userEntityService.findUser(userId);
                                    } else {
                                        userId = userEntityService.createUser(slug).getUuid();
                                    }

                                    if (user == null) {
                                        // register as new user
                                        user =
                                            userEntityService.addUser(
                                                userId,
                                                slug,
                                                u.getUsername(),
                                                u.getEmailAddress()
                                            );
                                    }

                                    u.setUserId(userId);

                                    // set status as active by default
                                    if (!StringUtils.hasText(u.getStatus())) {
                                        u.setStatus(SubjectStatus.ACTIVE.getValue());
                                    }

                                    // user account via service
                                    String uuid = u.getUuid();

                                    UserAccount account = null;
                                    if (StringUtils.hasText(uuid)) {
                                        // existing accounts are matched via UUID
                                        account = userAccountService.findUserAccount(uuid);
                                    }

                                    if (account == null) {
                                        String authority = u.getAuthority();
                                        String provider = u.getProvider();

                                        if (!StringUtils.hasText(authority) || !StringUtils.hasText(provider)) {
                                            // we ask id to be provided
                                            logger.error("error creating user, missing provider");
                                            throw new IllegalArgumentException("missing provider");
                                        }

                                        logger.debug("add {} account {} for realm {}", authority, id, slug);

                                        // create as new
                                        account =
                                            userAccountService.createUserAccount(authority, providerId, userId, id, u);
                                    } else {
                                        // check again realm match over existing
                                        if (!slug.equals(account.getRealm())) {
                                            logger.error("error creating account, realm mismatch");
                                            return;
                                        }

                                        logger.debug(
                                            "update {} account {} for realm {}",
                                            account.getAuthority(),
                                            id,
                                            slug
                                        );

                                        // update
                                        account = userAccountService.updateUserAccount(uuid, u);
                                    }

                                    if (account != null) {
                                        if (logger.isTraceEnabled()) {
                                            logger.trace(
                                                "{} user account: {}",
                                                String.valueOf(account.getAuthority()),
                                                String.valueOf(account)
                                            );
                                        }

                                        // register as resource if missing
                                        if (resourceService.findResourceEntity(account.getUuid()) == null) {
                                            resourceService.addResourceEntity(
                                                account.getUuid(),
                                                SystemKeys.RESOURCE_ACCOUNT,
                                                account.getAuthority(),
                                                account.getProvider(),
                                                account.getAccountId()
                                            );
                                        }
                                    }
                                } catch (
                                    RegistrationException
                                    | NoSuchUserException
                                    | NoSuchAuthorityException
                                    | NoSuchProviderException e
                                ) {
                                    logger.error(
                                        "error creating {} user {}: {}",
                                        String.valueOf(u.getAuthority()),
                                        String.valueOf(u.getAccountId()),
                                        e.getMessage()
                                    );

                                    e.printStackTrace();
                                }
                            });
                    }

                    /*
                     * Credentials
                     */
                    if (rc.getCredentials() != null) {
                        rc
                            .getCredentials()
                            .forEach(c -> {
                                // validate authority
                                if (!StringUtils.hasText(c.getAuthority())) {
                                    logger.error("error creating credentials, invalid authority");
                                    return;
                                }

                                // validate type
                                if (!StringUtils.hasText(c.getType())) {
                                    logger.error("error creating credentials, invalid type");
                                    return;
                                }

                                logger.debug(
                                    "create {} credential for realm {}",
                                    String.valueOf(c.getType()),
                                    String.valueOf(c.getRealm())
                                );

                                // validate realm match
                                if (StringUtils.hasText(c.getRealm()) && !slug.equals(c.getRealm())) {
                                    logger.error("error creating credentials, realm mismatch");
                                    return;
                                }

                                // enforce realm
                                c.setRealm(slug);

                                // validate account id
                                if (!StringUtils.hasText(c.getUserId())) {
                                    logger.error("error creating credentials, missing user id");
                                    throw new IllegalArgumentException("missing user id");
                                }

                                // validate id
                                if (!StringUtils.hasText(c.getCredentialsId())) {
                                    // we ask id to be provided otherwise we would create a new one every time
                                    logger.error("error creating credentials, missing id");
                                    throw new IllegalArgumentException("missing id");
                                }
                                if (logger.isTraceEnabled()) {
                                    logger.trace(
                                        "{} user credentials: {}",
                                        String.valueOf(c.getType()),
                                        String.valueOf(c)
                                    );
                                }

                                try {
                                    // add or update via service
                                    String id = c.getId();
                                    String providerId = c.getProvider();

                                    // we skip account check
                                    String userId = c.getUserId();

                                    // set status as active by default
                                    if (!StringUtils.hasText(c.getStatus())) {
                                        c.setStatus(SubjectStatus.ACTIVE.getValue());
                                    }

                                    // user credentials via service
                                    String uuid = c.getUuid();

                                    UserCredentials credentials = null;
                                    if (StringUtils.hasText(uuid)) {
                                        // existing are matched via UUID
                                        credentials = userCredentialsService.findUserCredentials(uuid);
                                    }

                                    if (credentials == null) {
                                        String authority = c.getAuthority();
                                        String provider = c.getProvider();

                                        if (!StringUtils.hasText(authority) || !StringUtils.hasText(provider)) {
                                            // we ask id to be provided
                                            logger.error("error creating credentials, missing provider");
                                            throw new IllegalArgumentException("missing provider");
                                        }

                                        logger.debug(
                                            "add {} credentials {} for realm {} user {}",
                                            authority,
                                            id,
                                            slug,
                                            userId
                                        );

                                        // create as new
                                        credentials =
                                            userCredentialsService.createUserCredentials(
                                                authority,
                                                providerId,
                                                userId,
                                                id,
                                                c
                                            );
                                    } else {
                                        // check again realm match over existing
                                        if (!slug.equals(credentials.getRealm())) {
                                            logger.error("error creating credentials, realm mismatch");
                                            return;
                                        }

                                        logger.debug(
                                            "update {} credentials {} for realm {} user {}",
                                            credentials.getAuthority(),
                                            id,
                                            slug,
                                            userId
                                        );

                                        // update
                                        credentials = userCredentialsService.updateUserCredentials(uuid, c);
                                    }

                                    if (credentials != null) {
                                        if (logger.isTraceEnabled()) {
                                            logger.trace(
                                                "{} user credentials: {}",
                                                String.valueOf(credentials.getAuthority()),
                                                String.valueOf(credentials)
                                            );
                                        }
                                        // // register as resource if missing
                                        // if (resourceService.findResourceEntity(credentials.getId()) == null) {
                                        //     resourceService.addResourceEntity(
                                        //         credentials.getUuid(),
                                        //         SystemKeys.RESOURCE_CREDENTIALS,
                                        //         credentials.getAuthority(),
                                        //         credentials.getProvider(),
                                        //         credentials.getAccountId()
                                        //     );
                                        // }
                                    }
                                    //                            // TODO refactor with a single method
                                    //                            // TODO refactor password services over repo
                                    //                            // TODO support webauthn
                                    //                            if ("credentials_password".equals(c.getType())) {
                                    //                                // cast
                                    //                                InternalUserPassword uc = (InternalUserPassword) c;
                                    //
                                    //                                if (!StringUtils.hasText(uc.getProvider())) {
                                    //                                    providerId = slug;
                                    //                                }
                                    //
                                    //                                // extract password and encode if required
                                    //                                String password = uc.getPassword();
                                    //                                // TODO encode and set via new service
                                    //                                if (id != null) {
                                    //                                    credentials = internalUserPasswordService.findPasswordById(id);
                                    //                                }
                                    //
                                    //                                if (credentials == null) {
                                    //                                    logger.debug("add password {} for realm {} account {}", id, slug,
                                    //                                            uc.getAccountId());
                                    //
                                    //                                    credentials = internalUserPasswordService.setPassword(providerId, uc.getUsername(),
                                    //                                            password, false, null, 0);
                                    //                                } else {
                                    //                                    // check again realm match over existing
                                    //                                    if (!slug.equals(credentials.getRealm())) {
                                    //                                        logger.error("error creating {} password, realm mismatch");
                                    //                                        return;
                                    //                                    }
                                    //
                                    //                                    logger.debug("update password {} for realm {} account {}", id, slug,
                                    //                                            uc.getAccountId());
                                    //                                    credentials = internalUserPasswordService.setPassword(providerId, uc.getUsername(),
                                    //                                            password, false, null, 0);
                                    //                                }
                                    //                            }

                                } catch (
                                    RegistrationException
                                    | NoSuchCredentialException
                                    | NoSuchUserException
                                    | NoSuchAuthorityException
                                    | NoSuchProviderException e
                                ) {
                                    logger.error(
                                        "error creating {} user credentials {}: {}",
                                        String.valueOf(c.getAuthority()),
                                        String.valueOf(c.getId()),
                                        e.getMessage()
                                    );
                                }
                            });
                    }

                    logger.debug("bootstrap realm {} created", String.valueOf(slug));
                } catch (Exception e) {
                    logger.error("error creating realm " + String.valueOf(slug) + ": " + e.getMessage());
                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            });


        // always reload services
        try {        
            reloadServices();
        } catch (Exception e) {
            logger.error("error reloading services: " + e.getMessage());
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }
        } 

        logger.debug("bootstrap config done");
        /*
         * Migrations?
         */

    }

    public void reloadServices() throws Exception {
        Runnable run = new Runnable() {
        public void run() {
            try {    
                // reload services
                serviceManager.reload();

                //TODO add others
            } catch (Exception e) {
                logger.error("error reloading services: " + e.getMessage());
                if (logger.isTraceEnabled()) {
                    e.printStackTrace();
                }
            } 
        }
        };
        //workaround - init an admin context and dispatch
        SecurityContext context = initContext(adminUsername);
        DelegatingSecurityContextRunnable dcr = new DelegatingSecurityContextRunnable(run, context);
        new Thread(dcr).start();
    }

    /*
     * Call init on each service we expect services to be independent and to execute
     * in their own transaction to avoid rollback issues across services
     */
    //    public void initServices() throws Exception {
    //        /*
    //         * Base user
    //         */
    //        logger.trace("init user");
    //        userManager.init();
    //
    //        logger.trace("init registration");
    //        registrationManager.init();
    //
    //        /*
    //         * Base roles
    //         */
    //        logger.trace("init roles");
    //        roleManager.init();
    //
    //        /*
    //         * Base services
    //         */
    //        logger.trace("init services");
    //        serviceManager.init();
    //
    //        /*
    //         * Base clients
    //         */
    //        logger.trace("init client");
    //        clientManager.init();

    //    }

    //
    //    public void executeMigrations() {
    //
    //    }

    //    public List<ProviderConfig<?, ?>> registerProviders(String type,
    //            Collection<? extends ConfigurableProvider> cps) throws NoSuchProviderException {
    //        AuthorityService<?> pas = authorityService.getAuthorityService(type);
    //
    //        List<ProviderConfig<?, ?>> configs = new ArrayList<>();
    //
    //        for (ConfigurableProvider cp : cps) {
    //            // try register
    //            if (cp.isEnabled()) {
    //                try {
    //                    // register directly with authority
    //                    ProviderConfig<?, ?> c = pas.getAuthority(cp.getAuthority()).registerProvider(cp);
    //                    configs.add(c);
    //                } catch (Exception e) {
    //                    logger.error("error registering provider {} {} for realm {}: {}",
    //                            type, cp.getProvider(), cp.getRealm(),
    //                            e.getMessage());
    //
    //                    if (logger.isTraceEnabled()) {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            }
    //        }
    //
    //        return configs;
    //    }

}
