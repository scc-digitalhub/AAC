package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.Client;
import java.io.Serializable;
import org.springframework.util.Assert;

public abstract class AbstractClient implements Client, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String realm;

    protected final String clientId;

    public AbstractClient(String realm, String clientId) {
        Assert.notNull(realm, "realm is mandatory");
        Assert.hasText(clientId, "clientId can not be null or empty");
        this.clientId = clientId;
        this.realm = realm;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractClient() {
        this(null, null);
    }

    @Override
    @JsonIgnore
    public String getRealm() {
        return realm;
    }

    @JsonIgnore
    public String getClientId() {
        return clientId;
    }
}
