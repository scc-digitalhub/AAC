package it.smartcommunitylab.aac.bootstrap;

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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.mchange.v1.util.ArrayUtils;

import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
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

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.debug("application bootstrap");

        /*
         * Admin
         */
        logger.debug("create admin user as " + adminUsername);

        User admin = this.createAdminUser(adminUsername, adminPassword, adminRoles);
        logger.trace("admin user id " + String.valueOf(admin.getId()));

        /*
         * Users creation
         */

        /*
         * Client creation
         */

        /*
         * Service creation
         */

        /*
         * Migrations
         */

    }

    public User createAdminUser(String username, String password, String[] roles) {
        Set<Role> adminRoles = new HashSet<>();
        Role role = Role.systemAdmin();
        adminRoles.add(role);

        if (roles != null) {
            logger.trace("ADMIN default roles " + Arrays.toString(roles));
            Arrays.asList(roles).forEach(ctx -> adminRoles.add(Role.parse(ctx)));
        }

        User admin = userRepository.findByUsername(username);
        if (admin == null) {
            logger.trace("create ADMIN user as " + username);
            admin = registrationManager.registerOffline(username, username, username, password, null, false, null);
        }
        // merge roles
        admin.getRoles().addAll(adminRoles);
        userRepository.saveAndFlush(admin);

        return admin;
    }

    public ClientDetailsEntity createAdminClient(
            long ownerId,
            String clientId, String clientSecret, String clientName,
            String[] grantTypes,
            String[] scopes,
            String[] redirects) {

        ClientDetailsEntity client = clientRepository.findByClientId(clientId);
        if (client == null) {
            // create
        } else {
            // update
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
