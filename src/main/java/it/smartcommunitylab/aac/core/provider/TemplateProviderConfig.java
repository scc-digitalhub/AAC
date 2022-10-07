package it.smartcommunitylab.aac.core.provider;

import java.util.Set;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;

public interface TemplateProviderConfig<M extends ConfigMap> extends ProviderConfig<M, ConfigurableTemplateProvider> {

    public Set<String> getLanguages();

    public String getCustomStyle();

}
