package it.smartcommunitylab.aac.model;

public class RealmRole {

    private String realm;
    private String role;

    public RealmRole() {
    }

    public RealmRole(String realm, String role) {
        this.realm = realm;
        this.role = role;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}
