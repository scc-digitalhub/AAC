package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public interface AttributeConfigurationProvider<M extends ConfigMap, C extends AttributeProviderConfig<M>>
    extends ConfigurationProvider<M, ConfigurableAttributeProvider, C> {}
