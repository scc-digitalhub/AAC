package it.smartcommunitylab.aac.scope.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;

public class InternalApiResourceProviderConfig
        extends AbstractApiResourceProviderConfig<AbstractInternalApiResource, InternalApiResourceProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_INTERNAL;

    public InternalApiResourceProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, new InternalApiResourceProviderConfigMap());
    }

    public InternalApiResourceProviderConfig(ConfigurableApiResourceProvider cp,
            InternalApiResourceProviderConfigMap configMap) {
        super(cp, configMap);

    }

    public void setResource(AbstractInternalApiResource res) {
        this.resource = res;
    }
}
