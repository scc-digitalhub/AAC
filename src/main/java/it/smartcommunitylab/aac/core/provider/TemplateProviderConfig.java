package it.smartcommunitylab.aac.core.provider;

import java.util.Set;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public interface TemplateProviderConfig<M extends ConfigMap> extends ProviderConfig<M> {

    public Set<String> getLanguages();

    public String getCustomStyle();

}
