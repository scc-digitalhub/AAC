package it.smartcommunitylab.aac.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.springframework.util.StringUtils;

import com.mchange.v1.util.ArrayUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig.BootstrapClient;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig.BootstrapUser;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.OAuthProviders;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@Component
public class AACBootstrap {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // TODO replace with userManager after userManager cleanup
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private RegistrationManager registrationManager;

    @Autowired
    private ClientDetailsRepository clientRepository;

    @Autowired
    private ClientDetailsManager clientManager;

    @Autowired
    private ServiceManager serviceManager;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.roles}")
    private String[] adminRoles;

//    @Value("${admin.contexts}")
//    private String[] defaultContexts;
//
//    @Value("${admin.contextSpaces}")
//    private String[] defaultContextSpaces;
//

    @Value("${adminClient.id:}")
    private String adminClientId;

    @Value("${adminClient.secret:}")
    private String adminClientSecret;

    @Value("${adminClient.grantTypes:}")
    private String[] adminClientGrantTypes;

    @Value("${adminClient.scopes:}")
    private String[] adminClientScopes;

    @Value("${adminClient.redirects:}")
    private String[] adminClientRedirects;

    @Autowired
    private BootstrapConfig config;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.debug("application bootstrap");

        /*
         * Admin
         */
        User admin = null;
        try {
            logger.debug("create admin user as " + adminUsername);
            admin = createUser(adminUsername, adminPassword, adminRoles, true);
            logger.trace("admin user id " + String.valueOf(admin.getId()));

            logger.debug("create admin client as " + adminClientId);
            ClientAppBasic adminClient = createClient(admin.getId(),
                    adminClientId, adminClientId,
                    adminClientSecret, adminClientSecret,
                    adminClientGrantTypes, adminClientScopes, adminClientRedirects,
                    true);

            logger.trace("admin client id " + adminClient.getClientId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
                // TODO Auto-generated catch block
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
                    // only clients owned by admin can be trusted
                    boolean isTrusted = false;
                    if (bc.isTrusted() && developerId == admin.getId()) {
                        isTrusted = true;
                    }

                    // all bootstrapped clients are regular
                    ClientAppBasic c = createClient(developerId,
                            bc.getId(), bc.getId(),
                            bc.getSecret(), bc.getSecret(),
                            bc.getGrantTypes(), bc.getScopes(), bc.getRedirectUris(),
                            isTrusted);

                    logger.trace("created client " + c.getName() + " with id " + c.getClientId() + " developer "
                            + c.getUserName());

                    // cache
                    clients.add(c);
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        /*
         * Service creation
         */
        try {
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /*
         * Migrations
         */

    }

    public User createUser(String username, String password, String[] roles, boolean isAdmin) throws Exception {
        Set<Role> userRoles = new HashSet<>();
        Role role = Role.systemUser();
        if (isAdmin) {
            role = Role.systemAdmin();
        }
        userRoles.add(role);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.trace("create user as " + username);
            user = registrationManager.registerOffline(username, username, username, password, null, false, null);
        } else {
            // reset password
            registrationManager.updatePassword(username, password);
        }

        if (roles != null) {
            logger.trace("user " + username + " roles " + Arrays.toString(roles));
            Arrays.asList(roles).forEach(ctx -> userRoles.add(Role.parse(ctx)));
        }

        // merge roles
        user.getRoles().addAll(userRoles);
        userRepository.saveAndFlush(user);

        return user;
    }

    public ClientAppBasic createClient(
            long ownerId,
            String clientId, String clientName,
            String clientSecret, String clientSecretMobile,
            String[] grantTypes,
            String[] scopes,
            String[] redirects,
            boolean isTrusted) throws Exception {

        ClientAppBasic client = clientManager.findByClientId(clientId);
        if (client == null) {
            // create
            ClientAppBasic appData = new ClientAppBasic();
            appData.setName(clientName);
            appData.setGrantedTypes(new HashSet<>(Arrays.asList(grantTypes)));
            appData.setScope(StringUtils.arrayToCommaDelimitedString(scopes));
            appData.setRedirectUris(StringUtils.arrayToCommaDelimitedString(redirects));

            // always enable internal idp
            appData.getIdentityProviders().put(Config.IDP_INTERNAL, true);

            if (isTrusted) {
                client = clientManager.createTrusted(appData, ownerId, clientId, clientSecret, clientSecretMobile);
            } else {
                client = clientManager.create(appData, ownerId, clientId, clientSecret, clientSecretMobile);
            }
        } else {
            // update
            ClientAppBasic appData = client;
            appData.setName(clientName);
            appData.setGrantedTypes(new HashSet<>(Arrays.asList(grantTypes)));
            appData.setScope(StringUtils.arrayToCommaDelimitedString(scopes));
            appData.setRedirectUris(StringUtils.arrayToCommaDelimitedString(redirects));

            // always enable internal idp
            appData.getIdentityProviders().put(Config.IDP_INTERNAL, true);
            if (isTrusted) {
                client = clientManager.updateTrusted(clientId, appData, clientSecret, clientSecretMobile);
            } else {
                client = clientManager.update(clientId, appData, clientSecret, clientSecretMobile);
            }
        }

        if (client != null) {

            clientId = client.getClientId();

            // approve idp and scopes
            client = clientManager.approveClientIdp(clientId);

            client = clientManager.approveClientScopes(clientId);

        }

        return client;

    }

    public Collection<Role> createContextAndSpaces(User owner, String[] contexts, String[] contextSpaces) {
        Set<Role> roles = new HashSet<>();

        if (contexts != null) {
            logger.trace("ADMIN default contexts " + Arrays.toString(contexts));
            Arrays.asList(contexts).forEach(ctx -> roles.add(Role.ownerOf(ctx)));
        }

        if (contextSpaces != null) {
            logger.trace("ADMIN default contexts spaces " + Arrays.toString(contextSpaces));
            Arrays.asList(contextSpaces).forEach(ctx -> roles.add(Role.ownerOf(ctx)));
        }

        // spaces are managed via roles on owner
        roleManager.updateRoles(owner, roles, null);

        return roles;
    }
//
//    public List<User> createUsers() {
//
//    }
//
//    public List<ServiceDTO> createServices() {
//
//    }
//
//    public List<ClientDetailsEntity> createClients() {
//
//    }
//
//    public void executeMigrations() {
//
//    }

}
