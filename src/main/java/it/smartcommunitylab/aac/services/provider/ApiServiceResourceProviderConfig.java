package it.smartcommunitylab.aac.services.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.services.model.ApiService;

public class ApiServiceResourceProviderConfig
        extends AbstractApiResourceProviderConfig<ApiService, InternalApiResourceProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + SystemKeys.RESOURCE_API_RESOURCE + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SERVICE;

    public ApiServiceResourceProviderConfig(ApiService res) {
        super(res, new InternalApiResourceProviderConfigMap());
    }
}