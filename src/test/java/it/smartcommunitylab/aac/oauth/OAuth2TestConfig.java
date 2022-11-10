package it.smartcommunitylab.aac.oauth;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.oauth.client.OAuth2ClientConfigMap;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;

public final class OAuth2TestConfig {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ClientConverter clientConverter = new ClientConverter();

    private final RealmConfig rc;

    private List<ClientRegistration> clients;
    private List<UserRegistration> users;

    OAuth2TestConfig(RealmConfig rc) {
        Assert.notNull(rc, "realm config can not be null");
        this.rc = rc;
        this.clients = null;
        this.users = null;
    }

    private void buildClients() {
        if (rc == null || rc.getClientApps() == null) {
            throw new IllegalArgumentException("missing config");
        }

        this.clients = rc.getClientApps().stream()
                .map(a -> {
                    // convert from config map and then align
                    OAuth2ClientConfigMap e = mapper.convertValue(a.getConfiguration(),
                            OAuth2ClientConfigMap.class);

                    ClientRegistration c = clientConverter.convert(e);
                    c.setClientId(a.getClientId());

                    // manually map secrets
                    String clientSecret = (String) a.getConfiguration().get("clientSecret");
                    c.setClientSecret(clientSecret);

                    String clientJwks = (String) a.getConfiguration().get("jwks");
                    c.setJwks(clientJwks);

                    return c;
                }).collect(Collectors.toList());
    }

    private void buildUsers() {
        if (rc == null || rc.getUsers() == null || rc.getCredentials() == null) {
            throw new IllegalArgumentException("missing config");
        }

        // map only internal users with password
        Map<String, String> passwords = rc.getCredentials().stream()
                .filter(c -> (c instanceof InternalUserPassword))
                .collect(Collectors.toMap(c -> c.getAccountId(), c -> ((InternalUserPassword) c).getPassword()));

        this.users = rc.getUsers().stream()
                .map(a -> {
                    UserRegistration r = new UserRegistration(a.getUserId());
                    r.setUsername(a.getUsername());
                    r.setPassword(passwords.get(a.getAccountId()));
                    return r;
                }).collect(Collectors.toList());
    }

    public String realm() {
        return rc.getRealm().getSlug();
    }

    public List<ClientRegistration> clients() {
        if (clients == null) {
            buildClients();
        }

        return clients;
    }

    public ClientRegistration client() {
        List<ClientRegistration> list = clients();
        if (list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }

    public List<UserRegistration> users() {
        if (users == null) {
            buildUsers();
        }

        return users;
    }

    public UserRegistration user() {
        List<UserRegistration> list = users();
        if (list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }

    private static class ClientConverter implements Converter<OAuth2ClientConfigMap, ClientRegistration> {

        @Override
        public ClientRegistration convert(OAuth2ClientConfigMap e) {
            ClientRegistration c = new ClientRegistration();
            // TODO map all fields
            return c;
        }

    }

    public class UserRegistration {
        private String userId;
        private String username;
        private String password;

        public UserRegistration(String userId) {
            this.userId = userId;
            this.username = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

    }
}