package it.smartcommunitylab.aac.test.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;

import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.User;

public abstract class OAuth2BaseTest {

    private final String server = "http://localhost";

    @LocalServerPort
    private int port;

    @Value("${server.contextPath}")
    private String contextPath;

    @Value("${jwt.issuer}")
    private String issuer;

    @Autowired
    protected ClientDetailsManager clientManager;

    @Autowired
    protected RegistrationManager registrationManager;

    @Autowired
    protected UserManager userManager;

    private static Map<String, ClientAppBasic> clients = new HashMap<>();

    private static Map<String, User> users = new HashMap<>();

    private static Map<String, String> userNames = new HashMap<>();
    private static Map<String, String> passwords = new HashMap<>();

//    @Before
//    public void init() {
//        String endpoint = server + ":" + port;
//        if (user == null) {
//            try {
//                user = createUser(getUserName(), getUserPassword(), getUserFirstName(), getUserLastName());
//            } catch (Exception e) {
//                e.printStackTrace();
//                user = null;
//            }
//        }
//
//        if (client == null && user != null) {
//            try {
//                // use local address as redirect
//                client = clientManager.createTrusted(UUID.randomUUID().toString(), user.getId(),
//                        getClass().getName(), null, null,
//                        getGrantTypes(), getScopes(), new String[] { endpoint });
//            } catch (Exception e) {
//                e.printStackTrace();
//                client = null;
//            }
//        }
//    }

    public void init() {
        try {
            User user = getUser();
            ClientAppBasic client = getClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Base
     */
    protected User getUser() throws RuntimeException {
        if (!users.containsKey(getClassName())) {
            User user = createUser(getUserName(), getUserPassword(), getUserFirstName(), getUserLastName());
            users.put(getClassName(), user);
        }

        return users.get(getClassName());
    }

    protected ClientAppBasic getClient() throws RuntimeException {
        if (!clients.containsKey(getClassName())) {
            String endpoint = server + ":" + port;
            User user = getUser();

            ClientAppBasic client = clientManager.createTrusted(UUID.randomUUID().toString(), user.getId(),
                    getClass().getName(), null,
                    getGrantTypes(), getScopes(), new String[] { endpoint });
            clients.put(getClassName(), client);
        }

        return clients.get(getClassName());
    }

    protected String getUserName() {
        if (!userNames.containsKey(getClassName())) {
            userNames.put(getClassName(), UUID.randomUUID().toString());
        }

        return userNames.get(getClassName());
    }

    protected String getUserPassword() {
        if (!passwords.containsKey(getClassName())) {
            passwords.put(getClassName(), UUID.randomUUID().toString());
        }

        return passwords.get(getClassName());
    }

    protected String getUserFirstName() {
        return "TestName";
    }

    protected String getUserLastName() {
        return "TestSurname";
    }

    protected abstract String[] getScopes();

    protected abstract String[] getGrantTypes();

    protected User createUser(String userName, String password, String name, String surname) {
        // username == email
        Registration reg = registrationManager.registerOffline(name, surname, userName, password, null, false, null);
        return userManager.findOne(Long.parseLong(reg.getUserId()));
    }

    private String getClassName() {
        return getClass().getName();
    }
}
