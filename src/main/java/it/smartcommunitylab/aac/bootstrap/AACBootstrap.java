package it.smartcommunitylab.aac.bootstrap;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.AccountCredentialsService;
import it.smartcommunitylab.aac.core.service.AttributeProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.CredentialsServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.CredentialsServiceService;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.IdentityServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityServiceService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.AccountServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.AccountServiceService;
import it.smartcommunitylab.aac.core.service.SubjectService;
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
    private RealmService realmService;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private ServicesManager serviceManager;

    @Autowired
    private UserEntityService userService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private AttributeProviderAuthorityService attributeProviderAuthorityService;

    @Autowired
    private IdentityServiceAuthorityService identityServiceAuthorityService;

    @Autowired
    private AccountServiceAuthorityService accountServiceAuthorityService;

    @Autowired
    private CredentialsServiceAuthorityService credentialsServiceAuthorityService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private IdentityServiceService identityServiceService;

    @Autowired
    private AccountServiceService accountServiceService;

    @Autowired
    private CredentialsServiceService credentialsServiceService;

    @Autowired
    private SpaceRoleService roleService;

    @Autowired
    private InternalAccountServiceAuthority internalAccountServiceAuthority;

    @Autowired
    private PasswordCredentialsAuthority passwordCredentialsAuthority;

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

            // bootstrap realm providers
            bootstrapAccountServices();
            bootstrapCredentialsServices();
            bootstrapIdentityServices();
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

    private void bootstrapSystemProviders()
            throws NoSuchRealmException {
        // idps
        Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(SystemKeys.REALM_SYSTEM);
        registerIdentityProviders(idps);

        // nothing else to register for system
    }

    private void bootstrapAccountServices() {
        // load all realm providers from storage
        Collection<Realm> realms = realmService.listUserRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            String slug = realm.getSlug();
            Collection<ConfigurableAccountService> idss = accountServiceService.listProviders(slug);
            // register
            registerAccountServices(idss);
        });
    }

    private List<AccountService<?, ?, ?>> registerAccountServices(
            Collection<ConfigurableAccountService> idss) {
        Map<String, AccountServiceAuthority<?, ?, ?, ?>> ias = accountServiceAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        List<AccountService<?, ?, ?>> services = new ArrayList<>();

        for (ConfigurableAccountService ids : idss) {
            // try register
            if (ids.isEnabled()) {
                try {
                    // register directly with authority
                    AccountServiceAuthority<?, ?, ?, ?> ia = ias.get(ids.getAuthority());
                    if (ia == null) {
                        throw new IllegalArgumentException("no authority for " + String.valueOf(ids.getAuthority()));
                    }

                    AccountService<?, ?, ?> p = ia.registerProvider(ids);
                    services.add(p);
                } catch (Exception e) {
                    logger.error("error registering provider " + ids.getProvider() + " for realm "
                            + ids.getRealm() + ": " + e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return services;
    }

    private void bootstrapCredentialsServices() {
        // load all realm providers from storage
        Collection<Realm> realms = realmService.listUserRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            String slug = realm.getSlug();
            Collection<ConfigurableCredentialsService> css = credentialsServiceService.listProviders(slug);
            registerCredentialsServices(css);
        });
    }

    private List<AccountCredentialsService<?, ?, ?>> registerCredentialsServices(
            Collection<ConfigurableCredentialsService> css) {
        Map<String, CredentialsServiceAuthority<?, ?, ?, ?>> cas = credentialsServiceAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        List<AccountCredentialsService<?, ?, ?>> services = new ArrayList<>();

        for (ConfigurableCredentialsService cs : css) {
            // try register
            if (cs.isEnabled()) {
                try {
                    // register directly with authority
                    CredentialsServiceAuthority<?, ?, ?, ?> ca = cas.get(cs.getAuthority());
                    if (ca == null) {
                        throw new IllegalArgumentException("no authority for " + String.valueOf(cs.getAuthority()));
                    }

                    AccountCredentialsService<?, ?, ?> s = ca.registerProvider(cs);
                    services.add(s);
                } catch (Exception e) {
                    logger.error("error registering provider " + cs.getProvider() + " for realm "
                            + cs.getRealm() + ": " + e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return services;
    }

    private void bootstrapIdentityServices() {
        // load all realm providers from storage
        Collection<Realm> realms = realmService.listUserRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            String slug = realm.getSlug();
            Collection<ConfigurableIdentityService> idss = identityServiceService.listProviders(slug);
            // register
            registerIdentityServices(idss);
        });
    }

    private List<IdentityService<?, ?, ?, ?>> registerIdentityServices(
            Collection<ConfigurableIdentityService> idss) {
        Map<String, IdentityServiceAuthority<?, ?, ?, ?, ?>> ias = identityServiceAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        List<IdentityService<?, ?, ?, ?>> services = new ArrayList<>();

        for (ConfigurableIdentityService ids : idss) {
            // try register
            if (ids.isEnabled()) {
                try {
                    // register directly with authority
                    IdentityServiceAuthority<?, ?, ?, ?, ?> ia = ias.get(ids.getAuthority());
                    if (ia == null) {
                        throw new IllegalArgumentException("no authority for " + String.valueOf(ids.getAuthority()));
                    }

                    IdentityService<?, ?, ?, ?> p = ia.registerProvider(ids);
                    services.add(p);
                } catch (Exception e) {
                    logger.error("error registering provider " + ids.getProvider() + " for realm "
                            + ids.getRealm() + ": " + e.getMessage());

                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return services;
    }

    private void bootstrapIdentityProviders() {
        // load all realm providers from storage
        Collection<Realm> realms = realmService.listUserRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(realm.getSlug());
            // register
            registerIdentityProviders(idps);
        });
    }

    private List<IdentityProvider<?, ?, ?, ?, ?>> registerIdentityProviders(
            Collection<ConfigurableIdentityProvider> idps) {
        Map<String, IdentityProviderAuthority<?, ?, ?, ?>> ias = identityProviderAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        List<IdentityProvider<?, ?, ?, ?, ?>> providers = new ArrayList<>();

        for (ConfigurableIdentityProvider idp : idps) {
            // try register
            if (idp.isEnabled()) {
                try {
                    // register directly with authority
                    IdentityProviderAuthority<?, ?, ?, ?> ia = ias.get(idp.getAuthority());
                    if (ia == null) {
                        throw new IllegalArgumentException("no authority for " + String.valueOf(idp.getAuthority()));
                    }

                    IdentityProvider<?, ?, ?, ?, ?> p = ia.registerProvider(idp);
                    providers.add(p);
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

    private void bootstrapAttributeProviders() {
        // load all realm providers from storage
        Collection<Realm> realms = realmService.listUserRealms();

        // we iterate by realm to load consistently each realm
        // we use parallel to leverage default threadpool, loading should be thread-safe
        realms.parallelStream().forEach(realm -> {
            Collection<ConfigurableAttributeProvider> providers = attributeProviderService
                    .listProviders(realm.getSlug());

            registerAttributeProviders(providers);
        });
    }

    private List<AttributeProvider<?, ?>> registerAttributeProviders(Collection<ConfigurableAttributeProvider> as) {
        Map<String, AttributeProviderAuthority<?, ?, ?>> ias = attributeProviderAuthorityService
                .getAuthorities().stream()
                .collect(Collectors.toMap(a -> a.getAuthorityId(), a -> a));

        List<AttributeProvider<?, ?>> providers = new ArrayList<>();

        for (ConfigurableAttributeProvider ap : as) {
            // try register
            if (ap.isEnabled()) {
                try {
                    // register directly with authority
                    AttributeProviderAuthority<?, ?, ?> ia = ias.get(ap.getAuthority());
                    if (ia == null) {
                        throw new IllegalArgumentException(
                                "no authority for " + String.valueOf(ap.getAuthority()));
                    }

                    AttributeProvider<?, ?> p = ia.registerProvider(ap);
                    providers.add(p);
                } catch (Exception e) {
                    logger.error("error registering provider " + ap.getProvider() + " for realm "
                            + ap.getRealm() + ": " + e.getMessage());

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

                Realm realm = realmService.findRealm(r.getSlug());
                if (realm == null) {
                    realm = realmService.addRealm(r.getSlug(), r.getName(), r.isEditable(), r.isPublic());
                } else {
                    // skip config maps
                    // TODO put in dedicated providers + config
                    realm = realmService.updateRealm(r.getSlug(), r.getName(), r.isEditable(), r.isPublic(), null);
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
