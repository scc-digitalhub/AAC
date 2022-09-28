package it.smartcommunitylab.aac.attributes.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

@Service
public class ScriptAttributeConfigurationProvider extends
        AbstractAttributeConfigurationProvider<ScriptAttributeProviderConfigMap, ScriptAttributeProviderConfig> {

    public ScriptAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_SCRIPT);
        setDefaultConfigMap(new ScriptAttributeProviderConfigMap());
    }

    @Override
    protected ScriptAttributeProviderConfig buildConfig(ConfigurableAttributeProvider cp) {
        return new ScriptAttributeProviderConfig(cp);
    }

}
