package it.smartcommunitylab.aac.bootstrap;

public class BootstrapUser {
    private String realm;
    private String email;
    private String username;
    private String password;
    private String[] roles;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    // TODO set a standard
    public String getId() {
        return "realm" + "|" + username;
    }

}