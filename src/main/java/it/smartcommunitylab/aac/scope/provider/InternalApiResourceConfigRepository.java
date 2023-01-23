package it.smartcommunitylab.aac.scope.provider;

import java.util.function.Function;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.repository.AutoProviderConfigRepository;
import it.smartcommunitylab.aac.scope.base.AbstractApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;

public class InternalApiResourceConfigRepository<R extends AbstractInternalApiResource<?, ?>, C extends AbstractApiResourceProviderConfig<R, InternalApiResourceProviderConfigMap>>
        extends
        AutoProviderConfigRepository<C, InternalApiResourceProviderConfigMap, ConfigurableApiResourceProvider> {

    public InternalApiResourceConfigRepository(
            ProviderConfigRepository<C> baseRepository, String resourceId,
            Function<String, C> builder) {
        super(baseRepository);
        Assert.notNull(builder, "builder can not be null");

        // set factories
        this.setCreator((providerId) -> {
            // id should match schema on resource
            // resourceId + SystemKeys.URN_SEPARATOR + realm
            if (!providerId.startsWith(resourceId) || !providerId.contains(SystemKeys.URN_SEPARATOR)) {
                return null;
            }

            String[] s = providerId.split(SystemKeys.URN_SEPARATOR);
            if (s.length != 2) {
                return null;
            }

            // build resource and config
            return builder.apply(s[1]);
        });

        this.setFactory((realm) -> {
            // build resource and config
            return builder.apply(realm);
        });

    }

}
