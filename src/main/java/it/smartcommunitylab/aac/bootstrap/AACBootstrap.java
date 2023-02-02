package it.smartcommunitylab.aac.bootstrap;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.TemplateProviderService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientAppService;
import it.smartcommunitylab.aac.password.auth.UsernamePasswordAuthenticationToken;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServicesService;

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
    private it.smartcommunitylab.aac.core.service.UserAccountService userAccountService;

    @Autowired
    private it.smartcommunitylab.aac.core.service.UserCredentialsService userCredentialsService;

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
        Collection<GrantedAuthority> authorities = Collections
                .singletonList(new SimpleGrantedAuthority(Config.R_ADMIN));

        // use an auth token
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, null,
                authorities);

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
        realms.parallelStream().forEach(realm -> {
            bootstrapRealmProviders(realm.getSlug());
        });
    }

    private void bootstrapRealmProviders(String slug) {
        // load providers in order, single thread
        bootstrapConfigurableProviders(templateProviderService, slug);
        bootstrapConfigurableProviders(identityProviderService, slug);
        bootstrapConfigurableProviders(attributeProviderService, slug);
    }

    private void bootstrapConfigurableProviders(ConfigurableProviderService<?, ?, ?> service, String realm) {
        // register in parallel via thread-pool
        service.listProviders(realm).parallelStream().forEach(cp -> {
            if (cp.isEnabled()) {
                try {
                    service.registerProvider(cp.getProvider());
                } catch (Exception e) {
                    logger.error("error registering provider {}:{} for realm {}: {}",
                            cp.getAuthority(), cp.getProvider(), cp.getRealm(),
                            e.getMessage());

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
            account = new InternalUserAccount();
            account.setProvider(realm);
            account.setRealm(realm);
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
            internalUserPasswordService.deleteAllCredentialsByAccount(repositoryId, username);
            InternalUserPassword pass = new InternalUserPassword();
            pass.setId(UUID.randomUUID().toString());
            pass.setProvider(repositoryId);
            pass.setUsername(username);
            pass.setUserId(account.getUserId());
            pass.setRealm(account.getRealm());
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

        config.getRealms().parallelStream().forEach(rc -> {
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

                    realm = realmService.addRealm(r.getSlug(), r.getName(), r.isEditable(), r.isPublic());
                } else {
                    logger.debug("update realm {}", r.getSlug());

                    // skip config maps
                    // TODO put in dedicated providers + config
                    realm = realmService.updateRealm(r.getSlug(), r.getName(), r.isEditable(), r.isPublic(), null);
                }

                /*
                 * IdP
                 */
                if (rc.getIdentityProviders() != null) {
                    rc.getIdentityProviders().forEach(cp -> {

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

                        if (logger.isTraceEnabled()) {
                            logger.trace("provider: {}", String.valueOf(cp));
                        }

                        try {
                            // add or update via service
                            String providerId = cp.getProvider();
                            ConfigurableIdentityProvider p = identityProviderService.findProvider(providerId);
                            if (p == null) {
                                logger.debug("add identity provider {} for realm {}", providerId,
                                        String.valueOf(cp.getRealm()));

                                p = identityProviderService.addProvider(slug, cp);
                            } else {
                                // check again realm match over existing
                                if (!slug.equals(p.getRealm())) {
                                    logger.error("error creating identity provider, realm mismatch");
                                    return;
                                }

                                logger.debug("update identity provider {} for realm {}", providerId,
                                        String.valueOf(cp.getRealm()));

                                if (p.isEnabled()) {
                                    identityProviderService.unregisterProvider(providerId);
                                }

                                p = identityProviderService.updateProvider(providerId, cp);
                            }

                            if (p.isEnabled()) {
                                // register
                                identityProviderService.registerProvider(p.getProvider());
                            }
                        } catch (RegistrationException | NoSuchRealmException | NoSuchProviderException
                                | NoSuchAuthorityException e) {
                            logger.error(
                                    "error creating identity provider " + String.valueOf(cp.getProvider()) + ": "
                                            + e.getMessage());
                        }
                    });
                }

                /*
                 * Attribute providers
                 */
                if (rc.getAttributeProviders() != null) {
                    rc.getAttributeProviders().forEach(cp -> {
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

                        if (logger.isTraceEnabled()) {
                            logger.trace("provider: {}", String.valueOf(cp));
                        }

                        try {
                            // add or update via service
                            String providerId = cp.getProvider();
                            ConfigurableAttributeProvider p = attributeProviderService.findProvider(providerId);
                            if (p == null) {
                                logger.debug("add attribute provider {} for realm {}", providerId,
                                        String.valueOf(cp.getRealm()));
                                p = attributeProviderService.addProvider(slug, cp);
                            } else {
                                // check again realm match over existing
                                if (!slug.equals(p.getRealm())) {
                                    logger.error("error creating attribute provider, realm mismatch");
                                    return;
                                }

                                logger.debug("update attribute provider {} for realm {}", providerId,
                                        String.valueOf(cp.getRealm()));

                                if (p.isEnabled()) {
                                    attributeProviderService.unregisterProvider(providerId);
                                }

                                p = attributeProviderService.updateProvider(providerId, cp);
                            }

                            if (p.isEnabled()) {
                                // register
                                attributeProviderService.registerProvider(p.getProvider());
                            }
                        } catch (RegistrationException | NoSuchRealmException | NoSuchProviderException
                                | NoSuchAuthorityException e) {
                            logger.error(
                                    "error creating attribute provider " + String.valueOf(cp.getProvider()) + ": "
                                            + e.getMessage());
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
                                logger.debug("add template provider {} for realm {}", providerId,
                                        String.valueOf(cp.getRealm()));

                                p = templateProviderService.addProvider(slug, cp);
                            } else {
                                // check again realm match over existing
                                if (!slug.equals(p.getRealm())) {
                                    logger.error("error creating template provider, realm mismatch");
                                    return;
                                }

                                logger.debug("update template provider {} for realm {}", providerId,
                                        String.valueOf(cp.getRealm()));

                                templateProviderService.unregisterProvider(providerId);
                                p = templateProviderService.updateProvider(providerId, cp);
                            }

                            // register
                            templateProviderService.registerProvider(p.getProvider());
                        } catch (RegistrationException | NoSuchRealmException | NoSuchProviderException
                                | NoSuchAuthorityException e) {
                            logger.error(
                                    "error creating template provider " + String.valueOf(cp.getProvider()) + ": "
                                            + e.getMessage());
                        }
                    }
                }

                /*
                 * Services
                 */
                if (rc.getServices() != null) {
                    rc.getServices().forEach(s -> {
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

                                service = serviceService.addService(s.getRealm(), id, s.getNamespace(), s.getName(),
                                        s.getDescription());
                            } else {
                                // check again realm match over existing
                                if (!slug.equals(service.getRealm())) {
                                    logger.error("error creating service, realm mismatch");
                                    return;
                                }

                                logger.debug("update service {} for realm {}", id, String.valueOf(s.getRealm()));

                                service = serviceService.updateService(id, s.getName(), s.getDescription(), null);
                            }

                        } catch (RegistrationException | NoSuchServiceException e) {
                            logger.error(
                                    "error creating service " + String.valueOf(s.getServiceId()) + ": "
                                            + e.getMessage());
                        }
                    });
                }

                /*
                 * ClientApp
                 * 
                 */
                if (rc.getClientApps() != null) {
                    rc.getClientApps().forEach(app -> {
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
                                logger.debug("add client app {} for realm {}", clientId,
                                        String.valueOf(app.getRealm()));

                                client = clientAppService.registerClient(app.getRealm(), app);
                            } else {
                                // check again realm match over existing
                                if (!slug.equals(client.getRealm())) {
                                    logger.error("error creating client app, realm mismatch");
                                    return;
                                }

                                logger.debug("update client app {} for realm {}", clientId,
                                        String.valueOf(app.getRealm()));

                                client = clientAppService.updateClient(app.getClientId(), app);
                            }

                        } catch (NoSuchClientException e) {
                            logger.error(
                                    "error creating client app " + String.valueOf(app.getClientId()) + ": "
                                            + e.getMessage());
                        }
                    });
                }

                /*
                 * User Accounts
                 * 
                 * TODO use User model with roles+authorities+groups
                 */
                if (rc.getUsers() != null) {
                    rc.getUsers().forEach(u -> {
                        // validate authority
                        if (!StringUtils.hasText(u.getAuthority())) {
                            logger.error("error creating user, invalid authority");
                            return;
                        }

                        logger.debug("create {} user for realm {}", String.valueOf(u.getAuthority()),
                                String.valueOf(u.getRealm()));

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
                            logger.trace("{} user account: {}", String.valueOf(u.getAuthority()), String.valueOf(u));
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
                                user = userEntityService.addUser(userId, slug, u.getUsername(), u.getEmailAddress());
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
                                account = userAccountService.createUserAccount(authority, providerId, userId, id, u);
                            } else {
                                // check again realm match over existing
                                if (!slug.equals(account.getRealm())) {
                                    logger.error("error creating account, realm mismatch");
                                    return;
                                }

                                logger.debug("update {} account {} for realm {}", account.getAuthority(), id, slug);

                                // update
                                account = userAccountService.updateUserAccount(uuid, u);
                            }

//                            UserAccount account = null;
//
//                            // TODO refactor with a single method
//                            if (u instanceof InternalUserAccount) {
//                                // cast
//                                InternalUserAccount ua = (InternalUserAccount) u;
//                                // validate provider
//                                if (!StringUtils.hasText(ua.getProvider())) {
//                                    // use realm as default
//                                    providerId = slug;
//                                    ua.setProvider(slug);
//                                }
//
//                                // set confirmed by default
//                                if (!ua.isConfirmed()) {
//                                    ua.setConfirmed(true);
//                                }
//
//                                account = internalUserAccountService.findAccountById(providerId, id);
//                                if (account == null) {
//                                    logger.debug("add internal account {} for realm {}", id, slug);
//
//                                    // register account
//                                    account = internalUserAccountService.addAccount(slug, ua.getAccountId(), ua);
//                                } else {
//                                    // check again realm match over existing
//                                    if (!slug.equals(account.getRealm())) {
//                                        logger.error("error creating internal account, realm mismatch");
//                                        return;
//                                    }
//
//                                    logger.debug("update internal account {} for realm {}", id, slug);
//                                    account = internalUserAccountService.updateAccount(slug, ua.getAccountId(), ua);
//                                }
//                            }
//
//                            if (u instanceof OIDCUserAccount) {
//                                // cast
//                                OIDCUserAccount ua = (OIDCUserAccount) u;
//
//                                // validate provider
//                                if (!StringUtils.hasText(ua.getProvider())) {
//                                    // we ask id to be provided otherwise we would create a new one every time
//                                    logger.error("error creating user, missing provider");
//                                    throw new IllegalArgumentException("missing provider");
//                                }
//
//                                account = oidcUserAccountService.findAccountById(providerId, id);
//                                if (account == null) {
//                                    logger.debug("add oidc account {} for realm {}", id, slug);
//
//                                    // register account
//                                    account = oidcUserAccountService.addAccount(slug, ua.getAccountId(), ua);
//                                } else {
//                                    // check again realm match over existing
//                                    if (!slug.equals(account.getRealm())) {
//                                        logger.error("error creating oidc account, realm mismatch");
//                                        return;
//                                    }
//
//                                    logger.debug("update oidc account {} for realm {}", id, slug);
//                                    account = oidcUserAccountService.updateAccount(slug, ua.getAccountId(), ua);
//                                }
//                            }
//
//                            if (u instanceof SamlUserAccount) {
//                                // cast
//                                SamlUserAccount ua = (SamlUserAccount) u;
//
//                                // validate provider
//                                if (!StringUtils.hasText(ua.getProvider())) {
//                                    // we ask id to be provided otherwise we would create a new one every time
//                                    logger.error("error creating user, missing provider");
//                                    throw new IllegalArgumentException("missing provider");
//                                }
//
//                                account = samlUserAccountService.findAccountById(providerId, id);
//                                if (account == null) {
//                                    logger.debug("add saml account {} for realm {}", id, slug);
//
//                                    // register account
//                                    account = samlUserAccountService.addAccount(slug, ua.getAccountId(), ua);
//                                } else {
//                                    // check again realm match over existing
//                                    if (!slug.equals(account.getRealm())) {
//                                        logger.error("error creating saml account, realm mismatch");
//                                        return;
//                                    }
//
//                                    logger.debug("update saml account {} for realm {}", id, slug);
//                                    account = samlUserAccountService.updateAccount(slug, ua.getAccountId(), ua);
//                                }
//                            }

                            if (account != null) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("{} user account: {}", String.valueOf(account.getAuthority()),
                                            String.valueOf(account));
                                }

                                // register as resource if missing
                                if (resourceService.findResourceEntity(account.getUuid()) == null) {
                                    resourceService.addResourceEntity(account.getUuid(), SystemKeys.RESOURCE_ACCOUNT,
                                            account.getAuthority(), account.getProvider(), account.getAccountId());
                                }

                            }

                        } catch (RegistrationException | NoSuchUserException | NoSuchAuthorityException
                                | NoSuchProviderException e) {
                            logger.error("error creating {} user {}: {}", String.valueOf(u.getAuthority()),
                                    String.valueOf(u.getAccountId()), e.getMessage());
                        }
                    });
                }

                /*
                 * Credentials
                 */
                if (rc.getCredentials() != null) {
                    rc.getCredentials().forEach(c -> {
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

                        logger.debug("create {} credential for realm {}", String.valueOf(c.getType()),
                                String.valueOf(c.getRealm()));

                        // validate realm match
                        if (StringUtils.hasText(c.getRealm()) && !slug.equals(c.getRealm())) {
                            logger.error("error creating credentials, realm mismatch");
                            return;
                        }

                        // enforce realm
                        c.setRealm(slug);

                        // validate account id
                        if (!StringUtils.hasText(c.getAccountId())) {
                            logger.error("error creating credentials, missing account id");
                            throw new IllegalArgumentException("missing account id");
                        }

                        // validate id
                        if (!StringUtils.hasText(c.getCredentialsId())) {
                            // we ask id to be provided otherwise we would create a new one every time
                            logger.error("error creating credentials, missing id");
                            throw new IllegalArgumentException("missing id");
                        }
                        if (logger.isTraceEnabled()) {
                            logger.trace("{} user credentials: {}", String.valueOf(c.getType()), String.valueOf(c));
                        }

                        try {
                            // add or update via service
                            String id = c.getId();
                            String providerId = c.getProvider();

                            // we skip account check
                            String accountId = c.getAccountId();

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

                                logger.debug("add {} credentials {} for realm {} account {}", authority, id, slug,
                                        accountId);

                                // create as new
                                credentials = userCredentialsService.createUserCredentials(authority, providerId,
                                        accountId, id, c);
                            } else {
                                // check again realm match over existing
                                if (!slug.equals(credentials.getRealm())) {
                                    logger.error("error creating credentials, realm mismatch");
                                    return;
                                }

                                logger.debug("update {} credentials {} for realm {} account {}",
                                        credentials.getAuthority(), id, slug,
                                        accountId);

                                // update
                                credentials = userCredentialsService.updateUserCredentials(uuid, c);
                            }

                            if (credentials != null) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("{} user credentials: {}", String.valueOf(credentials.getAuthority()),
                                            String.valueOf(credentials));
                                }

                                // register as resource if missing
                                if (resourceService.findResourceEntity(credentials.getUuid()) == null) {
                                    resourceService.addResourceEntity(credentials.getUuid(),
                                            SystemKeys.RESOURCE_CREDENTIALS,
                                            credentials.getAuthority(), credentials.getProvider(),
                                            credentials.getAccountId());
                                }

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

                        } catch (RegistrationException | NoSuchCredentialException | NoSuchUserException
                                | NoSuchAuthorityException | NoSuchProviderException e) {
                            logger.error("error creating {} user credentials {}: {}", String.valueOf(c.getAuthority()),
                                    String.valueOf(c.getId()), e.getMessage());
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

        logger.debug("bootstrap config done");

        /*
         * Migrations?
         */

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
