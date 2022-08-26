package it.smartcommunitylab.aac.attributes.provider;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

@Service
public class ScriptAttributeConfigurationProvider extends
        AbstractAttributeConfigurationProvider<ScriptAttributeProviderConfig, ScriptAttributeProviderConfigMap> {

    public ScriptAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_MAPPER);
        setDefaultConfigMap(new ScriptAttributeProviderConfigMap());
    }

    @Override
    protected ScriptAttributeProviderConfig buildConfig(ConfigurableProvider cp) {
        Assert.isInstanceOf(ConfigurableAttributeProvider.class, cp);
        return new ScriptAttributeProviderConfig((ConfigurableAttributeProvider) cp);
    }

}
