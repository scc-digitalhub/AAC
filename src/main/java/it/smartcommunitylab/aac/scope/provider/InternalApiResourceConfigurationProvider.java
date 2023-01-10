package it.smartcommunitylab.aac.scope.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AACApiResource;
import it.smartcommunitylab.aac.api.scopes.AdminApiResource;
import it.smartcommunitylab.aac.groups.scopes.GroupsResource;
import it.smartcommunitylab.aac.oauth.scope.OAuth2DCRResource;
import it.smartcommunitylab.aac.openid.scope.OpenIdResource;
import it.smartcommunitylab.aac.profiles.scope.OpenIdUserInfoResource;
import it.smartcommunitylab.aac.roles.scopes.RolesResource;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceConfigurationProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;

@Service
public class InternalApiResourceConfigurationProvider extends
        AbstractApiResourceConfigurationProvider<AbstractInternalApiResource, InternalApiResourceProviderConfigMap, InternalApiResourceProviderConfig> {

    private final String baseUrl;

    // TODO replace bifunction with interface + inner class on resources
    private static final Map<String, BiFunction<String, String, AbstractInternalApiResource>> builders;

    static {
        Map<String, BiFunction<String, String, AbstractInternalApiResource>> m = new HashMap<>();
        m.put(AACApiResource.RESOURCE_ID, AACApiResource::new);
        m.put(AdminApiResource.RESOURCE_ID, AdminApiResource::new);
        m.put(GroupsResource.RESOURCE_ID, GroupsResource::new);
        m.put(OAuth2DCRResource.RESOURCE_ID, OAuth2DCRResource::new);
        m.put(OpenIdResource.RESOURCE_ID, OpenIdResource::new);
        m.put(OpenIdUserInfoResource.RESOURCE_ID, OpenIdUserInfoResource::new);
        m.put(RolesResource.RESOURCE_ID, RolesResource::new);
        builders = m;
    }

    public InternalApiResourceConfigurationProvider(@Value("application.url") String baseUrl) {
        super(SystemKeys.AUTHORITY_INTERNAL);
        Assert.hasText(baseUrl, "baseUrl can not be null or empty");
        this.baseUrl = baseUrl;
    }

    @Override
    protected InternalApiResourceProviderConfig buildConfig(ConfigurableApiResourceProvider cp) {
        InternalApiResourceProviderConfig config = new InternalApiResourceProviderConfig(cp,
                getConfigMap(cp.getConfiguration()));

        // instantiate resource based on name, via builders
        String name = cp.getResource();
        if (name == null) {
            throw new IllegalArgumentException("invalid resource");
        }

        // builder uses 2 parameters
        BiFunction<String, String, AbstractInternalApiResource> builder = builders.get(name);
        if (builder == null) {
            throw new IllegalArgumentException("invalid resource");
        }

        AbstractInternalApiResource res = builder.apply(config.getRealm(), baseUrl);
        config.setResource(res);

        return config;
    }
}
