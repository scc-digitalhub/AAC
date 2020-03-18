package it.smartcommunitylab.aac.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.runtime.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v1.util.ArrayUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.bootstrap.BootstrapClient;
import it.smartcommunitylab.aac.bootstrap.BootstrapUser;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.ServiceClaim;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.OAuthProviders;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@Component
public class AACBootstrap {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${admin.username}")
    private String adminUsername;

    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private RegistrationManager registrationManager;

    @Autowired
    private ClientDetailsManager clientManager;

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private BootstrapConfig config;

    // TODO rework with dedicated bootstrappers *per-manager*
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            // base initalization
            logger.debug("application init");
            initServices();

            // custom bootstrap
            logger.debug("application bootstrap");
            bootstrap();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bootstrap() throws Exception {

        /*
         * Admin
         */
//        Long userId = Long.valueOf(Config.ADMIN_ID);
//        final User admin = userManager.findOne(userId);
        final User admin = userManager.getUserByUsername(adminUsername);

        logger.trace("admin user id " + String.valueOf(admin.getId()));

        // DEPRECATE admin client, let regular bootstrap handle it
//            logger.debug("create admin client as " + adminClientId);
//            ClientAppBasic adminClient = createClient(admin.getId(),
//                    adminClientId, adminClientId,
//                    adminClientSecret, adminClientSecret,
//                    adminClientGrantTypes, adminClientScopes, adminClientRedirects,
//                    null, null, null,
//                    true);
//
//            logger.trace("admin client id " + adminClient.getClientId());

        /*
         * Users creation
         */
        List<User> users = new ArrayList<>();
        logger.debug("create bootstrap users");
        List<BootstrapUser> bootUsers = config.getUsers();
        for (BootstrapUser bu : bootUsers) {
            try {

                // all bootstrapped users are regular
                User u = createUser(bu.getUsername(), bu.getPassword(), bu.getRoles(), false);
                logger.trace("created user " + u.getName() + " with id " + u.getId());

                // cache
                users.add(u);

            } catch (Exception e) {
                logger.error("error creating user " + bu.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * Client creation
         */
        List<ClientAppBasic> clients = new ArrayList<>();
        logger.debug("create bootstrap clients");
        List<BootstrapClient> bootClients = config.getClients();
        for (BootstrapClient bc : bootClients) {
            try {

                // developer should match the name of one between:
                // - admin
                // - bootstrapped users
                Long developerId = null;
                if (bc.getDeveloper().equals(admin.getName())) {
                    developerId = admin.getId();
                } else {
                    for (User u : users) {
                        if (bc.getDeveloper().equals(u.getName())) {
                            developerId = u.getId();
                            break;
                        }
                    }
                }

                if (developerId != null) {
                    String clientName = StringUtils.hasText(bc.getName()) ? bc.getName() : bc.getId();
                    // only clients owned by admin can be trusted
                    boolean isTrusted = false;
                    if (bc.isTrusted() && developerId == admin.getId()) {
                        isTrusted = true;
                    }

                    // code is base64encoded
                    String claimMappingCode = null;
                    if (StringUtils.hasText(bc.getClaimMappingFunction())) {
                        claimMappingCode = new String(Base64.getDecoder().decode(
                                bc.getClaimMappingFunction()),
                                "UTF-8");
                    }

                    ClientAppBasic c = createClient(developerId,
                            bc.getId(), clientName,
                            bc.getSecret(), bc.getSecret(),
                            bc.getGrantTypes(), bc.getScopes(), bc.getRedirectUris(),
                            bc.getUniqueSpaces(), claimMappingCode, bc.getAfterApprovalWebhook(),
                            isTrusted);

                    logger.trace("created client " + c.getName() + " with id " + c.getClientId() + " developer "
                            + c.getUserName());

                    // cache
                    clients.add(c);
                }

            } catch (Exception e) {
                logger.error("error creating client " + bc.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * Service creation
         */
        try {
//                ServiceDTO[] services = new ObjectMapper().readValue(getClass().getResourceAsStream("services.json"),
//                        ServiceDTO[].class);

            // TODO define a format in bootstrap yaml for services
            ServiceDTO[] services = new ServiceDTO[0];
            for (ServiceDTO serviceDTO : services) {
                try {

                    String serviceId = serviceDTO.getServiceId();

                    // update existing or create new ones
                    serviceManager.saveService(admin, serviceDTO);

                    logger.trace("created service " + serviceId + " developer " + admin.getName());

                    if (serviceDTO.getClaims() != null) {

                        serviceDTO.getClaims().forEach(claim -> {
                            try {
                                serviceManager.saveServiceClaim(admin, serviceDTO.getServiceId(), claim);
                            } catch (IllegalArgumentException iex) {
                                // ignore, claim already exists
                            }
                        });
                    }
                    if (serviceDTO.getScopes() != null) {
                        serviceDTO.getScopes()
                                .forEach(scope -> serviceManager.saveServiceScope(admin, serviceDTO.getServiceId(),
                                        scope));
                    }
                } catch (Exception e) {
                    logger.error("error creating service " + serviceDTO.getServiceId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /*
         * Migrations?
         */

    }

    /*
     * Call init on each service we expect services to be independent and to execute
     * in their own transaction to avoid rollback issues across services
     */
    public void initServices() throws Exception {
        /*
         * Base user
         */
        logger.trace("init user");
        userManager.init();

        logger.trace("init registration");
        registrationManager.init();

        /*
         * Base roles
         */
        logger.trace("init roles");
        roleManager.init();

        /*
         * Base services
         */
        logger.trace("init services");
        serviceManager.init();

        /*
         * Base clients
         */
        logger.trace("init client");
        clientManager.init();

    }

    public User createUser(String username, String password, String[] roles, boolean isAdmin) throws Exception {
        Set<Role> userRoles = new HashSet<>();
        Role role = Role.systemUser();
        if (isAdmin) {
            role = Role.systemAdmin();
        }
        userRoles.add(role);

        User user = userManager.getUserByUsername(username);
        if (user == null) {
            logger.trace("create user as " + username);
            Registration reg = registrationManager.registerOffline(username, username, username, password, null, false,
                    null);
            user = userManager.findOne(Long.parseLong(reg.getUserId()));
        } else {
            // reset password
            registrationManager.updatePassword(username, password);
        }

        if (roles != null) {
            logger.trace("user " + username + " roles " + Arrays.toString(roles));
            Arrays.asList(roles).forEach(ctx -> userRoles.add(Role.parse(ctx)));
        }

        // merge roles
        roleManager.updateRoles(user.getId(), userRoles, null);
        return user;

    }

    public ClientAppBasic createClient(
            long ownerId,
            String clientId, String clientName,
            String clientSecret, String clientSecretMobile,
            String[] grantTypes,
            String[] scopes,
            String[] redirectUris,
            String[] uniqueSpaces,
            String claimMappingFunction,
            String afterApprovalWebhook,
            boolean isTrusted) throws Exception {

        ClientAppBasic client = clientManager.findByClientId(clientId);
        if (client == null) {

            if (isTrusted) {
                client = clientManager.createTrusted(clientId, ownerId,
                        clientName, clientSecret, clientSecretMobile,
                        grantTypes, scopes, redirectUris);
            } else {
                client = clientManager.create(clientId, ownerId,
                        clientName, clientSecret, clientSecretMobile,
                        grantTypes, scopes, redirectUris);
            }
//            // create
//            ClientAppBasic appData = new ClientAppBasic();
//            appData.setName(clientName);
//            appData.setGrantedTypes(new HashSet<>(Arrays.asList(grantTypes)));
//            appData.setScope(StringUtils.arrayToCommaDelimitedString(scopes));
//            appData.setRedirectUris(StringUtils.arrayToCommaDelimitedString(redirects));
//            // init providers
//            appData.setIdentityProviderApproval(new HashMap<String, Boolean>());
//            appData.setIdentityProviders(new HashMap<String, Boolean>());
//            
//            // always enable internal idp
//            appData.getIdentityProviders().put(Config.IDP_INTERNAL, true);
//
//            if (isTrusted) {
//                client = clientManager.createTrusted(appData, ownerId, clientId, clientSecret, clientSecretMobile);
//            } else {
//                client = clientManager.create(appData, ownerId, clientId, clientSecret, clientSecretMobile);
//            }
        }

        if (client != null) {

            clientId = client.getClientId();

            // update basic
            ClientAppBasic appData = client;
            appData.setName(clientName);
            if (grantTypes != null) {
                appData.setGrantedTypes(new HashSet<>(Arrays.asList(grantTypes)));
            }
            if (scopes != null) {
                appData.setScope(new HashSet<>(Arrays.asList((scopes))));
            }
            if (redirectUris != null) {
                appData.setRedirectUris(new HashSet<>(Arrays.asList((redirectUris))));
            }

            // always enable internal idp
            appData.getIdentityProviders().put(Config.IDP_INTERNAL, true);

            // update extended
            if (uniqueSpaces != null) {
                client.setUniqueSpaces(new HashSet<>(Arrays.asList(uniqueSpaces)));
            }

            if (claimMappingFunction != null) {
                client.setClaimMapping(claimMappingFunction);
            }

            if (afterApprovalWebhook != null) {
                client.setOnAfterApprovalWebhook(afterApprovalWebhook);
            }

            if (isTrusted) {
                client = clientManager.updateTrusted(clientId, appData, clientSecret, clientSecretMobile);
            } else {
                client = clientManager.update(clientId, appData, clientSecret, clientSecretMobile);
            }

            // approve idp and scopes
            client = clientManager.approveClientIdp(clientId);

            client = clientManager.approveClientScopes(clientId);

        }

        return client;

    }

//
//    public void executeMigrations() {
//
//    }

}
