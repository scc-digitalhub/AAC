package it.smartcommunitylab.aac.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractRegistrableProviderAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oauth.store.SearchableApprovalStore;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfigMap;
import it.smartcommunitylab.aac.services.model.ApiService;
import it.smartcommunitylab.aac.services.provider.ApiServiceConfigurationProvider;
import it.smartcommunitylab.aac.services.provider.ApiServiceResourceProvider;
import it.smartcommunitylab.aac.services.provider.ApiServiceResourceProviderConfig;

@Service
public class ApiServiceResourceAuthority extends
        AbstractRegistrableProviderAuthority<ApiServiceResourceProvider, ApiService, ConfigurableApiResourceProvider, InternalApiResourceProviderConfigMap, ApiServiceResourceProviderConfig>
        implements ApiResourceProviderAuthority<ApiServiceResourceProvider, ApiService> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORITY = SystemKeys.AUTHORITY_SERVICE;

    private ApiServiceConfigurationProvider configProvider;

    // services
    private ScriptExecutionService executionService;
    private SearchableApprovalStore approvalStore;

    public ApiServiceResourceAuthority(
            ProviderConfigRepository<ApiServiceResourceProviderConfig> registrationRepository) {
        super(AUTHORITY, registrationRepository);
    }

    @Autowired
    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Autowired
    public void setApprovalStore(SearchableApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    public void setConfigProvider(ApiServiceConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(executionService, "execution service is required");
        Assert.notNull(approvalStore, "approval store is required");
    }

    @Override
    public String getAuthorityId() {
        return AUTHORITY;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_API_RESOURCE;
    }

    @Override
    public ApiServiceConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    public ApiServiceResourceProviderConfig registerProvider(ApiServiceResourceProviderConfig providerConfig)
            throws RegistrationException {
        if (providerConfig == null) {
            throw new RegistrationException("invalid config");
        }

        String providerId = providerConfig.getProvider();
        String realm = providerConfig.getRealm();

        try {

            // check if exists or id clashes with another provider from a different realm
            ApiServiceResourceProviderConfig e = registrationRepository.findByProviderId(providerId);
            if (e != null) {
                if (!realm.equals(e.getRealm())) {
                    // name clash
                    throw new RegistrationException(
                            "a provider with the same id already exists under a different realm");
                }

                // evaluate version against current
                if (e.getVersion() >= providerConfig.getVersion()) {
                    // increment version to force reload
                    providerConfig.setVersion(e.getVersion() + 1);
                }
            } else {
                // reset version to 1, not found in current repository
                providerConfig.setVersion(1);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("provider active config v{}: {}", providerConfig.getVersion(),
                        String.valueOf(providerConfig.getConfigMap().getConfiguration()));
            }

            // register, we defer loading
            // should update if existing
            registrationRepository.addRegistration(providerConfig);

            // load to warm local cache
            ApiServiceResourceProvider rp = providers.get(providerId);

            // return effective config
            return rp.getConfig();
        } catch (Exception ex) {
            // cleanup
            registrationRepository.removeRegistration(providerId);
            logger.error("error registering provider {}: {}", providerId, ex.getMessage());

            throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected ApiServiceResourceProvider buildProvider(ApiServiceResourceProviderConfig config) {
        return new ApiServiceResourceProvider(executionService, approvalStore, config);
    }

}
