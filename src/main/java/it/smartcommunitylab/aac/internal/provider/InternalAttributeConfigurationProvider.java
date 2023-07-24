package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import org.springframework.stereotype.Service;

@Service
public class InternalAttributeConfigurationProvider
    extends AbstractAttributeConfigurationProvider<InternalAttributeProviderConfigMap, InternalAttributeProviderConfig> {

    public InternalAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_INTERNAL);
        setDefaultConfigMap(new InternalAttributeProviderConfigMap());
    }

    @Override
    protected InternalAttributeProviderConfig buildConfig(ConfigurableAttributeProvider cp) {
        return new InternalAttributeProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
