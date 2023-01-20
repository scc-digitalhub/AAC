package it.smartcommunitylab.aac.api.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.AACApiResourceAuthority;
import it.smartcommunitylab.aac.api.scopes.AACApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;

public class AACApiResourceProviderConfig
        extends AbstractApiResourceProviderConfig<AACApiResource, InternalApiResourceProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + AACApiResourceAuthority.AUTHORITY;

    public AACApiResourceProviderConfig(AACApiResource res) {
        super(res, new InternalApiResourceProviderConfigMap());
    }
}
