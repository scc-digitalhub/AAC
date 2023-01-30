package it.smartcommunitylab.aac.services.provider;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderConfigurationProvider;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.services.model.ApiService;
import it.smartcommunitylab.aac.services.service.ServicesService;

@Service
public class ApiServiceConfigurationProvider
        extends
        AbstractConfigurationProvider<InternalApiResourceProviderConfigMap, ConfigurableApiResourceProvider, ApiServiceResourceProviderConfig>
        implements
        ApiResourceProviderConfigurationProvider<ApiService, InternalApiResourceProviderConfigMap, ApiServiceResourceProviderConfig> {

    // services
    private final ServicesService service;

    public ApiServiceConfigurationProvider(ServicesService service) {
        super(SystemKeys.AUTHORITY_SERVICE);
        Assert.notNull(service, "services service can not be null");
        this.service = service;
    }

    @Override
    public ConfigurableApiResourceProvider getConfigurable(ApiServiceResourceProviderConfig providerConfig) {
        ConfigurableApiResourceProvider cp = new ConfigurableApiResourceProvider(providerConfig.getAuthority(),
                providerConfig.getProvider(), providerConfig.getRealm());

        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        // resource is serviceId
        cp.setResource(providerConfig.getResource().getServiceId());

        // provider config are active by definition
        cp.setEnabled(true);

        // keep version
        cp.setVersion(providerConfig.getVersion());

        return cp;
    }

    @Override
    protected ApiServiceResourceProviderConfig buildConfig(ConfigurableApiResourceProvider cp) {
        // resource is serviceId
        String serviceId = cp.getResource();
        if (!StringUtils.hasText(serviceId)) {
            throw new IllegalArgumentException("invalid resource id");
        }

        try {
            ApiService as = service.getService(serviceId);

            // check realm
            if (!as.getRealm().equals(cp.getRealm())) {
                throw new IllegalArgumentException("realm mismatch");
            }

            // build
            // this will effectively serialize the api service definition
            ApiServiceResourceProviderConfig providerConfig = new ApiServiceResourceProviderConfig(as);

            // increase version every time to make sure we reload *everywhere*
            // we do it here because we load service from DB here
            int version = providerConfig.getVersion() + 1;
            providerConfig.setVersion(version);

            return providerConfig;
        } catch (NoSuchServiceException e) {
            throw new IllegalArgumentException("invalid resource id");
        }

    }

}
