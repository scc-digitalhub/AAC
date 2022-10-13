package it.smartcommunitylab.aac.bootstrap;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.authorities.AuthorityService;
import it.smartcommunitylab.aac.core.authorities.TemplateProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.ProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.TemplateProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.TemplateProviderService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.InternalAccountServiceAuthority;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.provider.InternalAccountService;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.password.PasswordCredentialsAuthority;
import it.smartcommunitylab.aac.password.provider.PasswordCredentialsService;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServicesManager;
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
    private ResourceLoader resourceLoader;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @Autowired
    private BootstrapConfig config;

//    @Autowired
//    private AuthorityManager authorityManager;

    @Autowired
    private RealmService realmService;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private ServicesService serviceService;

    @Autowired
    private UserEntityService userService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private TemplateProviderAuthorityService templateProviderAuthorityService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private TemplateProviderService templateProviderService;

    @Autowired
    private SpaceRoleService roleService;

    @Autowired
    private InternalAccountServiceAuthority internalAccountServiceAuthority;

    @Autowired
    private PasswordCredentialsAuthority passwordCredentialsAuthority;

    @Autowired
    private ProviderAuthorityService authorityService;

    @EventListener
    public void onApplicationEvent(ApplicationStartedEvent event) {
        bootstrap();
    }

    public void bootstrap() {
        try {
            // base initialization
            logger.debug("application bootstrap");

            // build a security context as admin to bootstrap configs
            // initContext(adminUsername);

            bootstrapSystemProviders();

            // bootstrap admin account
            // use first active service for system, from authority
            // we expect a single service anyway
            Optional<InternalAccountService> sysInternalIdp = internalAccountServiceAuthority
                    .getProvidersByRealm(SystemKeys.REALM_SYSTEM).stream()
                    .findFirst();
            Optional<PasswordCredentialsService> sysPasswordIdp = passwordCredentialsAuthority
                    .getProvidersByRealm(SystemKeys.REALM_SYSTEM).stream()
                    .findFirst();

            if (sysInternalIdp.isPresent() && sysPasswordIdp.isPresent()) {
                bootstrapAdminUser(sysInternalIdp.get(), sysPasswordIdp.get());
            }

            // validate and remediate realm provider configs
            bootstrapRealmProviders();

            // custom bootstrap
            if (apply) {
                logger.debug("application bootstrap");
                bootstrapConfig();
            } else {
                logger.debug("bootstrap disabled by config");
            }

        } catch (Exception e) {
            logger.error("error bootstrapping: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void bootstrapSystemProviders() {
        // bootstrap only idps
        bootstrapIdentityProviders(SystemKeys.REALM_SYSTEM);
    }

    private void bootstrapIdentityProviders(String realm) {
        identityProviderService.listProviders(realm).parallelStream().forEach(cp -> {
            if (cp.isEnabled()) {
                try {
                    identityProviderService.registerProvider(cp.getProvider());
                } catch (Exception e) {
                    logger.error("error registering identity provider {}:{} for realm {}: {}",
                            cp.getAuthority(), cp.getProvider(), cp.getRealm(),
                            e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void bootstrapAttributeProviders(String realm) {
        attributeProviderService.listProviders(realm).parallelStream().forEach(cp -> {
            if (cp.isEnabled()) {
                try {
                    attributeProviderService.registerProvider(cp.getProvider());
                } catch (Exception e) {
                    logger.error("error registering attribute provider {}:{} for realm {}: {}",
                            cp.getAuthority(), cp.getProvider(), cp.getRealm(),
                            e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void bootstrapRealmProviders() {
        // load all realm providers from storage
        Collection<Realm> realms = realmService.listUserRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            String slug = realm.getSlug();

            // load providers in order, single thread
            bootstrapTemplateProviders(slug);
            bootstrapIdentityProviders(slug);
            bootstrapAttributeProviders(slug);
        });
    }

    private void bootstrapTemplateProviders(String realm) {
        // load all
        Collection<ConfigurableTemplateProvider> providers = templateProviderService.listProviders(realm);

        // make sure there is always a default provider available
        // use authority.realm slug as id
        String id = SystemKeys.AUTHORITY_TEMPLATE + SystemKeys.SLUG_SEPARATOR + realm;

        if (providers.isEmpty()
                || providers.stream().noneMatch(i -> (i.getAuthority().equals(SystemKeys.AUTHORITY_TEMPLATE)
                        && i.getProvider().equals(id)))) {

            // build one and register
            ConfigurableTemplateProvider p = new ConfigurableTemplateProvider(
                    SystemKeys.AUTHORITY_TEMPLATE, id, realm);
            try {
                p = templateProviderService.addProvider(realm, p);
                templateProviderService.registerProvider(p.getProvider());
            } catch (RegistrationException | SystemException | NoSuchAuthorityException | NoSuchRealmException
                    | NoSuchProviderException e) {
                // skip
                logger.error("error creating provider " + p.getProvider() + " for realm "
                        + p.getRealm() + ": " + e.getMessage());
                if (logger.isTraceEnabled()) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<TemplateProviderConfig<?>> registerTemplateProviders(Collection<ConfigurableTemplateProvider> tps) {
        Map<String, TemplateProviderAuthority<?, ?, ?, ?>> tas = templateProviderAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        List<TemplateProviderConfig<?>> providers = new ArrayList<>();

        for (ConfigurableTemplateProvider tp : tps) {
            // try register
            if (tp.isEnabled()) {
                try {
                    // register directly with authority
                    TemplateProviderAuthority<?, ?, ?, ?> ta = tas.get(tp.getAuthority());
                    if (ta == null) {
                        throw new IllegalArgumentException(
                                "no authority for " + String.valueOf(tp.getAuthority()));
                    }

                    TemplateProviderConfig<?> p = ta.registerProvider(tp);
                    providers.add(p);
                } catch (Exception e) {
                    logger.error("error registering provider " + tp.getProvider() + " for realm "
                            + tp.getRealm() + ": " + e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return providers;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void bootstrapAdminUser(InternalAccountService idp, PasswordCredentialsService service)
            throws RegistrationException, NoSuchUserException {
        // create admin as superuser for system
        // TODO rewrite via idp
        logger.debug("create internal admin user " + adminUsername);
        UserEntity user = null;
        InternalUserAccount account = idp.findAccount(adminUsername);
        if (account != null) {
//            // check if sub exists, recreate if needed
//            String uuid = account.getUuid();
//            if (!StringUtils.hasText(uuid)) {
//                // generate uuid and register as subject
//                uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
//            }
//            Subject s = subjectService.findSubject(uuid);
//            if (s == null) {
//                s = subjectService.addSubject(uuid, SystemKeys.REALM_SYSTEM, SystemKeys.RESOURCE_ACCOUNT,
//                        adminUsername);
//            }
//            account.setUuid(uuid);

            // check if user exists, recreate if needed
            user = userService.findUser(account.getUserId());
            if (user == null) {
                user = userService.addUser(userService.createUser(SystemKeys.REALM_SYSTEM).getUuid(),
                        SystemKeys.REALM_SYSTEM, adminUsername, adminEmail);
            }
        } else {
            // register as new
            user = userService.addUser(userService.createUser(SystemKeys.REALM_SYSTEM).getUuid(),
                    SystemKeys.REALM_SYSTEM, adminUsername, adminEmail);
            String userId = user.getUuid();

            // generate uuid
            String uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);

            account = new InternalUserAccount();
            account.setProvider(idp.getProvider());
            account.setUserId(userId);
            account.setUuid(uuid);
            account.setRealm(SystemKeys.REALM_SYSTEM);
            account.setUsername(adminUsername);
            account.setEmail(adminEmail);
            account.setStatus(SubjectStatus.ACTIVE.getValue());
            account = idp.createAccount(userId, account);
        }

        String userId = account.getUserId();

        try {
            // update username
            user = userService.updateUser(userId, adminUsername, adminEmail);
            // ensure user is active
            user = userService.activateUser(userId);

            // re-set password if needed
            if (!service.verifyPassword(adminUsername, adminPassword)) {
                // direct set
                service.setPassword(adminUsername, adminPassword, false);
            }

            // ensure account is confirmed and unlocked
            account = idp.confirmAccount(adminUsername);
            account = idp.unlockAccount(adminUsername);

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

            logger.debug("admin user id " + String.valueOf(account.getId()));
            logger.debug("admin user " + user.toString());
            logger.debug("admin account " + account.toString());
        } catch (NoSuchUserException | NoSuchSubjectException e) {
            logger.error("error updating admin account: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bootstrapConfig() throws Exception {

        // read configuration
        Resource res = resourceLoader.getResource(source);
        if (!res.exists()) {
            logger.debug("no bootstrap file from " + source);
            return;
        }

        // read config
        config = yamlObjectMapper.readValue(res.getInputStream(), BootstrapConfig.class);

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
                logger.debug("create or update realm " + r.getSlug());

                Realm realm = realmService.findRealm(r.getSlug());
                if (realm == null) {
                    realm = realmService.addRealm(r.getSlug(), r.getName(), r.isEditable(), r.isPublic());
                } else {
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
                } else {
                    // check if exists via default bootstrap
                    bootstrapTemplateProviders(slug);
                }

                /*
                 * Services
                 */
                if (rc.getServices() != null) {
                    rc.getServices().forEach(s -> {
                        logger.debug("create service for realm {}", String.valueOf(s.getRealm()));

                        // validate realm match
                        if (!StringUtils.hasText(s.getRealm()) || !slug.equals(s.getRealm())) {
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
                 * TODO use service in place of manager to avoid permissions
                 */
//                if (rc.getClientApps() != null) {
//                    rc.getClientApps().forEach(app -> {
//                        logger.debug("create client app for realm {}", String.valueOf(app.getRealm()));
//
//                        // validate realm match
//                        if (!StringUtils.hasText(app.getRealm()) || !slug.equals(app.getRealm())) {
//                            logger.error("error creating service, realm mismatch");
//                            return;
//                        }
//                
//                        //enforce realm
//                        app.setRealm(slug);
//                
//                        if (!StringUtils.hasText(app.getClientId())) {
//                            // we ask id to be provided otherwise we create a new one every time
//                            logger.error("error creating client, missing clientId");
//                            throw new IllegalArgumentException("missing clientId");
//                        }
//
//                        if (logger.isTraceEnabled()) {
//                            logger.trace("app: {}", String.valueOf(app));
//                        }
//
//                        try {
//                            String clientId = app.getClientId();
//                            ClientApp client = clientManager.findClientApp(app.getRealm(), clientId);
//
//                            if (client == null) {
//                                logger.debug("add client app {} for realm {}", clientId,
//                                        String.valueOf(app.getRealm()));
//
//                                client = clientManager.registerClientApp(app.getRealm(), app);
//                            } else {
//                                logger.debug("update client app {} for realm {}", clientId,
//                                        String.valueOf(app.getRealm()));
//
//                                client = clientManager.updateClientApp(app.getRealm(), app.getClientId(), app);
//                            }
//
//                        } catch (RegistrationException | NoSuchClientException | NoSuchRealmException e) {
//                            logger.error(
//                                    "error creating client app " + String.valueOf(app.getClientId()) + ": "
//                                            + e.getMessage());
//                        }
//                    });
//                }

                /*
                 * User Accounts
                 * 
                 * TODO
                 */

            } catch (Exception e) {
                logger.error("error creating realm " + String.valueOf(slug) + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

//        // Internal users
//        PasswordHash hasher = new PasswordHash();
//        for (InternalUserAccount ua : config.getUsers().getInternal()) {
//            try {
//                if (!StringUtils.hasText(ua.getRealm()) || !StringUtils.hasText(ua.getProvider())) {
//                    // invalid, skip
//                    continue;
//                }
//                if (!realms.containsKey(ua.getRealm()) || !providers.containsKey(ua.getProvider())) {
//                    // not managed here, skip
//                    continue;
//                }
//                if (!StringUtils.hasText(ua.getSubject())) {
//                    // we ask id to be provided otherwise we create a new one every time
//                    logger.error("error creating user, missing subjectId");
//                    throw new IllegalArgumentException("missing subjectId");
//                }
//                if (!StringUtils.hasText(ua.getUsername())) {
//                    // we ask id to be provided otherwise we create a new one every time
//                    logger.error("error creating user, missing username");
//                    throw new IllegalArgumentException("missing username");
//                }
//                if (!StringUtils.hasText(ua.getPassword())) {
//                    // we ask id to be provided otherwise we create a new one every time
//                    logger.error("error creating user, missing password");
//                    throw new IllegalArgumentException("missing password");
//                }
//
//                logger.debug("create or update user " + ua.getSubject() + " with authority internal");
//
//                // check if user exists, recreate if needed
//                UserEntity user = userService.findUser(ua.getSubject());
//                if (user == null) {
//                    user = userService.addUser(ua.getSubject(), ua.getRealm(), ua.getUsername(), ua.getEmail());
//                } else {
//                    // check match
//                    if (!user.getRealm().equals(ua.getRealm())) {
//                        logger.error("error creating user, realm mismatch");
//                        throw new IllegalArgumentException("realm mismatch");
//                    }
//
//                    user = userService.updateUser(ua.getSubject(), ua.getUsername(), ua.getEmail());
//                }
//
//                InternalUserAccount account = internalUserService.findAccountByUsername(ua.getRealm(),
//                        ua.getUsername());
//
//                if (account == null) {
//                    account = internalUserService.addAccount(ua);
//                } else {
//                    account = internalUserService.updateAccount(account.getId(), account);
//                }
//
//                // re-set password
//                String hash = hasher.createHash(ua.getPassword());
//                account.setPassword(hash);
//                account.setChangeOnFirstAccess(false);
//
//                // ensure account is unlocked
//                account.setConfirmed(true);
//                account.setConfirmationKey(null);
//                account.setConfirmationDeadline(null);
//                account.setResetKey(null);
//                account.setResetDeadline(null);
//                account = internalUserService.updateAccount(account.getId(), account);
//
//            } catch (Exception e) {
//                logger.error("error creating user " + String.valueOf(ua.getSubject()) + ": " + e.getMessage());
//                e.printStackTrace();
//            }
//        }

        // TODO oidc/saml users
        // requires accountService extracted from idp to detach from repo

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

    public List<ProviderConfig<?, ?>> registerProviders(String type,
            Collection<? extends ConfigurableProvider> cps) throws NoSuchProviderException {
        AuthorityService<?> pas = authorityService.getAuthorityService(type);

        List<ProviderConfig<?, ?>> configs = new ArrayList<>();

        for (ConfigurableProvider cp : cps) {
            // try register
            if (cp.isEnabled()) {
                try {
                    // register directly with authority
                    ProviderConfig<?, ?> c = pas.getAuthority(cp.getAuthority()).registerProvider(cp);
                    configs.add(c);
                } catch (Exception e) {
                    logger.error("error registering provider {} {} for realm {}: {}",
                            type, cp.getProvider(), cp.getRealm(),
                            e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return configs;
    }

}
