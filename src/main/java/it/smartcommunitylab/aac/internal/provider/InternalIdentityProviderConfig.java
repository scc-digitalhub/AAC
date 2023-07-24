package it.smartcommunitylab.aac.internal.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public class InternalIdentityProviderConfig extends AbstractIdentityProviderConfig<InternalIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + InternalIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final int MAX_SESSION_DURATION = 24 * 60 * 60; // 24h

    //    public InternalIdentityProviderConfig(
    //            @JsonProperty("provider") String provider,
    //            @JsonProperty("realm") String realm,
    //            @JsonProperty("configMap") InternalIdentityProviderConfigMap configMap) {
    //        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, configMap);
    //    }

    public InternalIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        InternalIdentityProviderConfigMap configMap
    ) {
        super(cp, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private InternalIdentityProviderConfig() {
        this(
            new ConfigurableIdentityProvider((String) null, (String) null, (String) null),
            (InternalIdentityProviderConfigMap) null
        );
    }

    public String getRepositoryId() {
        // not configurable for now
        return getRealm();
    }

    /*
     * default config
     */

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null
            ? configMap.getMaxSessionDuration().intValue()
            : MAX_SESSION_DURATION;
    }
}
