package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.Client;

public abstract class AbstractClient implements Client, Serializable {

    private final String realm;
    protected final String clientId;

    public AbstractClient(String realm, String clientId) {
        Assert.notNull(realm, "realm is mandatory");
        Assert.hasText(clientId, "clientId can not be null or empty");
        this.clientId = clientId;
        this.realm = realm;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    public String getClientId() {
        return clientId;
    }

}
