package it.smartcommunitylab.aac.core.base;

public abstract class AbstractUser {

    private String realm;
    private String username;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public abstract String getId();

}
