package it.smartcommunitylab.aac.core.base;

public abstract class AbstractClient {
    private String realm;
    private String name;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String getId();

    public abstract String getType();

}
