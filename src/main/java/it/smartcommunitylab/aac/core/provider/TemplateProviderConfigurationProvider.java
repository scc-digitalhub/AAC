package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;

public interface TemplateProviderConfigurationProvider<M extends ConfigMap, C extends TemplateProviderConfig<M>>
    extends ConfigurationProvider<M, ConfigurableTemplateProvider, C> {}
