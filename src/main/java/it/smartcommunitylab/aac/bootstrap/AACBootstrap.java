package it.smartcommunitylab.aac.bootstrap;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
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
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.authorities.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.authorities.IdentityServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.AttributeProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.CredentialsServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.CredentialsServiceService;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.IdentityServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityServiceService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.SubjectStatus;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProvider;
import it.smartcommunitylab.aac.password.service.InternalPasswordService;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;
import it.smartcommunitylab.aac.services.Service;
import it.smartcommunitylab.aac.services.ServicesManager;

@Component
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

//    @Autowired
    private BootstrapConfig config;

//    @Autowired
//    private AuthorityManager authorityManager;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private ServicesManager serviceManager;

    @Autowired
    private UserEntityService userService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private UserAccountService<InternalUserAccount> internalUserService;

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private AttributeProviderAuthorityService attributeProviderAuthorityService;

    @Autowired
    private IdentityServiceAuthorityService identityServiceAuthorityService;

    @Autowired
    private CredentialsServiceAuthorityService credentialsServiceAuthorityService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private IdentityServiceService identityServiceService;

    @Autowired
    private CredentialsServiceService credentialsServiceService;

    @Autowired
    private SpaceRoleService roleService;

    @Autowired
    private InternalPasswordIdentityAuthority passwordIdentityAuthority;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            // base initialization
            logger.debug("application init");
//            initServices();

            // build a security context as admin to bootstrap configs
            // initContext(adminUsername);

//            if (authoritiesProps.getCustom() != null) {
//                bootstrapCustomAuthorities(authoritiesProps.getCustom());
//            }

            // bootstrap providers
            // TODO use a dedicated thread, or a multithread
            bootstrapSystemProviders();

            // bootstrap admin account
            // use first active password provider for system, from authority
            Optional<InternalPasswordIdentityProvider> sysPasswordIdp = passwordIdentityAuthority
                    .getProvidersByRealm(SystemKeys.REALM_SYSTEM).stream()
//                    .filter(i -> SystemKeys.RESOURCE_REALM.equals(i.getScope()))
                    .findFirst();

            if (sysPasswordIdp.isPresent()) {
                bootstrapAdminUser(sysPasswordIdp.get());
            }

//            systemProviders.values().stream()
//                    .filter(idp -> SystemKeys.AUTHORITY_PASSWORD.equals(idp.getAuthority()))
//                    .findFirst().ifPresent(idp -> {
//                        bootstrapAdminUser((InternalPasswordIdentityProvider) idp);
//                    });

            // bootstrap realm providers
            bootstrapIdentityServices();
            bootstrapCredentialsServices();
            bootstrapIdentityProviders();
            bootstrapAttributeProviders();

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

//    private List<IdentityProviderAuthority<? extends UserIdentity, ? extends IdentityProvider<?>>> bootstrapCustomAuthorities(
//            List<CustomAuthoritiesProperties> customProps) {
//        List<IdentityProviderAuthority<? extends UserIdentity, ? extends IdentityProvider<?>>> customAuthorities = new ArrayList<>();
//
//        for (CustomAuthoritiesProperties authProp : customProps) {
//
//            // read props
//            String id = authProp.getId();
//            String name = authProp.getName();
//            String description = authProp.getDescription();
//
//            if (StringUtils.hasText(id)) {
//                // derive type manually
//                // TODO refactor
//
//                if (authProp.getOidc() != null) {
//                    // buid oidc config provider
//                    OIDCIdentityProviderConfigMap configMap = authProp.getOidc();
//                    OIDCIdentityConfigurationProvider configProvider = new OIDCIdentityConfigurationProvider(id,
//                            configMap);
//
//                    // build config repositories
//                    ProviderConfigRepository<OIDCIdentityProviderConfig> registrationRepository = new InMemoryProviderConfigRepository<>();
//                    OIDCClientRegistrationRepository clientRegistrationRepository = new OIDCClientRegistrationRepository();
//                    // instantiate authority
//                    OIDCIdentityAuthority auth = new OIDCIdentityAuthority(
//                            id,
//                            userService, subjectService,
//                            oidcUserService, jdbcAttributeStore,
//                            registrationRepository,
//                            clientRegistrationRepository);
//
//                    auth.setConfigProvider(configProvider);
//                    auth.setExecutionService(executionService);
//
//                    // register for manager
//                    identityProviderAuthorityService.registerAuthority(auth);
//                    customAuthorities.add(auth);
//                }
//            }
//        }
//
//        return customAuthorities;
//    }

    private Map<String, IdentityProvider<? extends UserIdentity, ?, ?>> bootstrapSystemProviders()
            throws NoSuchRealmException {
        Map<String, IdentityProviderAuthority<?, ?, ?, ?>> ias = identityProviderAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(SystemKeys.REALM_SYSTEM);
        Map<String, IdentityProvider<? extends UserIdentity, ?, ?>> providers = new HashMap<>();
        for (ConfigurableIdentityProvider idp : idps) {
            // try register
            if (idp.isEnabled()) {
                try {
                    // register directly with authority
                    IdentityProviderAuthority<?, ?, ?, ?> ia = ias.get(idp.getAuthority());
                    if (ia == null) {
                        throw new IllegalArgumentException(
                                "no authority for " + String.valueOf(idp.getAuthority()));
                    }

                    IdentityProvider<? extends UserIdentity, ?, ?> p = ia.registerProvider(idp);
                    providers.put(p.getProvider(), p);
                } catch (Exception e) {
                    logger.error("error registering provider " + idp.getProvider() + " for realm "
                            + idp.getRealm() + ": " + e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return providers;
    }

    private void bootstrapIdentityServices() {
        Map<String, IdentityServiceAuthority<?, ?, ?, ?, ?>> ias = identityServiceAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        // load all realm providers from storage
        Collection<Realm> realms = realmManager.listRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            String slug = realm.getSlug();
            Collection<ConfigurableIdentityService> idss = identityServiceService.listProviders(slug);

            // make sure there is always an internal service available as default
            if (idss.isEmpty()
                    || idss.stream().noneMatch(i -> i.getAuthority().equals(SystemKeys.AUTHORITY_INTERNAL))) {
                ConfigurableIdentityService ids = new ConfigurableIdentityService(
                        SystemKeys.AUTHORITY_INTERNAL, null, slug);
                try {
                    ids = identityServiceService.addProvider(slug, ids);
                } catch (RegistrationException | SystemException | NoSuchAuthorityException e) {
                    // skip
                    logger.error("error creating provider " + ids.getProvider() + " for realm "
                            + ids.getRealm() + ": " + e.getMessage());
                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }

                // re-read list
                idss = identityServiceService.listProviders(slug);
            }

            for (ConfigurableIdentityService ids : idss) {
                // try register
                if (ids.isEnabled()) {
                    try {
                        // register directly with authority
                        IdentityServiceAuthority<?, ?, ?, ?, ?> ia = ias.get(ids.getAuthority());
                        if (ia == null) {
                            throw new IllegalArgumentException(
                                    "no authority for " + String.valueOf(ids.getAuthority()));
                        }

                        ia.registerProvider(ids);
                    } catch (Exception e) {
                        logger.error("error registering provider " + ids.getProvider() + " for realm "
                                + ids.getRealm() + ": " + e.getMessage());

                        if (logger.isTraceEnabled()) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void bootstrapCredentialsServices() {
        Map<String, CredentialsServiceAuthority<?, ?, ?, ?>> cas = credentialsServiceAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        // load all realm providers from storage
        Collection<Realm> realms = realmManager.listRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            String slug = realm.getSlug();
            Collection<ConfigurableCredentialsService> css = credentialsServiceService.listProviders(slug);

            // make sure there is always a password service available as default
            if (css.isEmpty()
                    || css.stream().noneMatch(i -> i.getAuthority().equals(SystemKeys.AUTHORITY_PASSWORD))) {
                ConfigurableCredentialsService cs = new ConfigurableCredentialsService(
                        SystemKeys.AUTHORITY_PASSWORD, null, slug);
                try {
                    cs = credentialsServiceService.addProvider(slug, cs);
                } catch (RegistrationException | SystemException | NoSuchAuthorityException e) {
                    // skip
                    logger.error("error creating service " + cs.getProvider() + " for realm "
                            + cs.getRealm() + ": " + e.getMessage());
                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }

                // re-read list
                css = credentialsServiceService.listProviders(slug);
            }

            for (ConfigurableCredentialsService cs : css) {
                // try register
                if (cs.isEnabled()) {
                    try {
                        // register directly with authority
                        CredentialsServiceAuthority<?, ?, ?, ?> ca = cas.get(cs.getAuthority());
                        if (ca == null) {
                            throw new IllegalArgumentException(
                                    "no authority for " + String.valueOf(cs.getAuthority()));
                        }

                        ca.registerProvider(cs);
                    } catch (Exception e) {
                        logger.error("error registering provider " + cs.getProvider() + " for realm "
                                + cs.getRealm() + ": " + e.getMessage());

                        if (logger.isTraceEnabled()) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void bootstrapIdentityProviders() {
        Map<String, IdentityProviderAuthority<?, ?, ?, ?>> ias = identityProviderAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        // load all realm providers from storage
        Collection<Realm> realms = realmManager.listRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(realm.getSlug());

            for (ConfigurableIdentityProvider idp : idps) {
                // try register
                if (idp.isEnabled()) {
                    try {
                        // register via authorityManager
//                        authorityManager.registerIdentityProvider(idp);

                        // register directly with authority
                        IdentityProviderAuthority<?, ?, ?, ?> ia = ias
                                .get(idp.getAuthority());
                        if (ia == null) {
                            throw new IllegalArgumentException(
                                    "no authority for " + String.valueOf(idp.getAuthority()));
                        }

                        ia.registerProvider(idp);
                    } catch (Exception e) {
                        logger.error("error registering provider " + idp.getProvider() + " for realm "
                                + idp.getRealm() + ": " + e.getMessage());

                        if (logger.isTraceEnabled()) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void bootstrapAttributeProviders() {
        Map<String, AttributeProviderAuthority<?, ?, ?>> ias = attributeProviderAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        // load all realm providers from storage
        Collection<Realm> realms = realmManager.listRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            Collection<ConfigurableAttributeProvider> providers = attributeProviderService
                    .listProviders(realm.getSlug());

            for (ConfigurableAttributeProvider provider : providers) {
                // try register
                if (provider.isEnabled()) {
                    try {
                        // register via authorityManager
//                        authorityManager.registerAttributeProvider(idp);

                        // register directly with authority
                        AttributeProviderAuthority<?, ?, ?> ia = ias.get(provider.getAuthority());
                        if (ia == null) {
                            throw new IllegalArgumentException(
                                    "no authority for " + String.valueOf(provider.getAuthority()));
                        }

                        ia.registerProvider(provider);
                    } catch (Exception e) {
                        logger.error("error registering provider " + provider.getProvider() + " for realm "
                                + provider.getRealm() + ": " + e.getMessage());

                        if (logger.isTraceEnabled()) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void bootstrapAdminUser(InternalPasswordIdentityProvider idp) {
        String repositoryId = idp.getConfig().getRepositoryId();
        if (CredentialsType.PASSWORD != idp.getConfig().getCredentialsType()) {
            // not supported
            logger.error("wrong idp config for system provider {}", idp.getConfig().getCredentialsType().getValue());
            return;
        }

        // create admin as superuser for system
        // TODO rewrite via idp
        logger.debug("create internal admin user " + adminUsername);
        UserEntity user = null;
        InternalUserAccount account = internalUserService.findAccountById(repositoryId, adminUsername);
        if (account != null) {
            // check if sub exists, recreate if needed
            String uuid = account.getUuid();
            if (!StringUtils.hasText(uuid)) {
                // generate uuid and register as subject
                uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
            }
            Subject s = subjectService.findSubject(uuid);
            if (s == null) {
                s = subjectService.addSubject(uuid, SystemKeys.REALM_SYSTEM, SystemKeys.RESOURCE_ACCOUNT,
                        adminUsername);
            }
            account.setUuid(uuid);

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

            // generate uuid and register as subject
            String uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
            Subject s = subjectService.addSubject(uuid, SystemKeys.REALM_SYSTEM, SystemKeys.RESOURCE_ACCOUNT,
                    adminUsername);

            account = new InternalUserAccount();
            account.setProvider(repositoryId);
            account.setUserId(userId);
            account.setUuid(s.getSubjectId());
            account.setRealm(SystemKeys.REALM_SYSTEM);
            account.setUsername(adminUsername);
            account.setEmail(adminEmail);
            account.setStatus(SubjectStatus.ACTIVE.getValue());
            account = internalUserService.addAccount(repositoryId, adminUsername, account);
        }

        String userId = account.getUserId();

        try {
            // update username
            user = userService.updateUser(userId, adminUsername, adminEmail);
            // ensure user is active
            user = userService.activateUser(userId);

            // re-set password if needed
            InternalPasswordIdentityCredentialsService passwordService = idp.getCredentialsService();
            if (!passwordService.verifyPassword(adminUsername, adminPassword)) {
                // set as non-expirable
                passwordService.setPassword(adminUsername, adminPassword, false, null);
            }

            // ensure account is active and unlocked
            account.setStatus(SubjectStatus.ACTIVE.getValue());
            account.setConfirmed(true);
            account.setConfirmationKey(null);
            account.setConfirmationDeadline(null);

            account = internalUserService.updateAccount(repositoryId, account.getUsername(), account);

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

        // TODO validation on imported beans

        /*
         * Realms creation
         */
        logger.debug("create bootstrap realms");

        // keep a cache of bootstrapped realms, we
        // will process only content related to these realms
        Map<String, Realm> realms = new HashMap<>();

        for (Realm r : config.getRealms()) {

            try {
                if (!StringUtils.hasText(r.getSlug())) {
                    // we ask id to be provided otherwise we create a new one every time
                    logger.error("error creating realm, missing slug");
                    throw new IllegalArgumentException("missing slug");
                }

                logger.debug("create or update realm " + r.getSlug());

                Realm realm = realmManager.findRealm(r.getSlug());
                if (realm == null) {
                    realm = realmManager.addRealm(r);
                } else {
                    realm = realmManager.updateRealm(r.getSlug(), r);
                }

                // keep in cache
                realms.put(realm.getSlug(), realm);

            } catch (Exception e) {
                logger.error("error creating provider " + String.valueOf(r.getSlug()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * IdP
         */
        // keep a cache, we'll load users only to these providers
        Map<String, ConfigurableProvider> providers = new HashMap<>();
        for (ConfigurableProvider cp : config.getProviders()) {

            try {
                if (!realms.containsKey(cp.getRealm())) {
                    // not managed here, skip
                    continue;
                }
                if (!StringUtils.hasText(cp.getProvider())) {
                    // we ask id to be provided otherwise we create a new one every time
                    logger.error("error creating provider, missing id");
                    throw new IllegalArgumentException("missing id");
                }
                // we support only idp for now
                // TODO refactor, this doesn't work oob
//                if (SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
//                    ConfigurableIdentityProvider ip = (ConfigurableIdentityProvider) cp;
//                    logger.debug("create or update provider " + cp.getProvider());
//                    ConfigurableIdentityProvider provider = identityProviderManager.findProvider(cp.getRealm(),
//                            cp.getProvider());
//
//                    if (provider == null) {
//                        provider = identityProviderManager.addProvider(cp.getRealm(), ip);
//                    } else {
//                        provider = identityProviderManager.unregisterProvider(cp.getRealm(), cp.getProvider());
//                        provider = identityProviderManager.updateProvider(cp.getRealm(), cp.getProvider(), ip);
//                    }
//
//                    if (cp.isEnabled()) {
//                        // register
//                        if (!identityProviderManager.isProviderRegistered(cp.getRealm(), provider)) {
//                            provider = identityProviderManager.registerProvider(provider.getRealm(),
//                                    provider.getProvider());
//                        }
//                    }
//
//                    // keep in cache
//                    providers.put(provider.getProvider(), provider);
//
//                }

            } catch (Exception e) {
                logger.error("error creating provider " + String.valueOf(cp.getProvider()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * Services
         */
        for (Service s : config.getServices()) {

            try {
                if (!StringUtils.hasText(s.getRealm()) || !realms.containsKey(s.getRealm())) {
                    // not managed here, skip
                    continue;
                }
                if (!StringUtils.hasText(s.getServiceId())) {
                    // we ask id to be provided otherwise we create a new one every time
                    logger.error("error creating service, missing serviceId");
                    throw new IllegalArgumentException("missing serviceId");
                }

                if (!StringUtils.hasText(s.getNamespace())) {
                    logger.error("error creating service, missing namespace");
                    throw new IllegalArgumentException("missing namespace");
                }

                logger.debug("create or update service " + s.getServiceId());
                Service service = serviceManager.findService(s.getRealm(), s.getServiceId());

                if (service == null) {
                    service = serviceManager.addService(s.getRealm(), s);
                } else {
                    service = serviceManager.updateService(s.getRealm(), s.getServiceId(), s);
                }

            } catch (Exception e) {
                logger.error("error creating service " + String.valueOf(s.getServiceId()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * ClientApp
         */
        for (ClientApp ca : config.getClients()) {

            try {
                if (!StringUtils.hasText(ca.getRealm()) || !realms.containsKey(ca.getRealm())) {
                    // not managed here, skip
                    continue;
                }
                if (!StringUtils.hasText(ca.getClientId())) {
                    // we ask id to be provided otherwise we create a new one every time
                    logger.error("error creating client, missing clientId");
                    throw new IllegalArgumentException("missing clientId");
                }

                logger.debug("create or update client " + ca.getClientId());
                ClientApp client = clientManager.findClientApp(ca.getRealm(), ca.getClientId());

                if (client == null) {
                    client = clientManager.registerClientApp(ca.getRealm(), ca);
                } else {
                    client = clientManager.updateClientApp(ca.getRealm(), ca.getClientId(), ca);
                }

            } catch (Exception e) {
                logger.error("error creating client " + String.valueOf(ca.getClientId()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

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
    public void initServices() throws Exception {
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

    }

//
//    public void executeMigrations() {
//
//    }

}
