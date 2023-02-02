package it.smartcommunitylab.aac.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;

import it.smartcommunitylab.aac.scope.Scope;

public class ConnectedDevice {

    private String id;

    @NotBlank
    private String subjectId;

    @NotBlank
    private String clientId;

    @NotBlank
    private String realm;

    private String appName;
    private String appDescription;

    private List<Scope> scopes;

    private Date expiration;

    public ConnectedDevice() {
    }

    public ConnectedDevice(String subjectId, String clientId, String realm, Collection<Scope> scopes) {
        this.subjectId = subjectId;
        this.clientId = clientId;
        this.realm = realm;

        this.scopes = new ArrayList<>();
        this.scopes.addAll(scopes);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppDescription() {
        return appDescription;
    }

    public void setAppDescription(String appDescription) {
        this.appDescription = appDescription;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

}
