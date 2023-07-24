package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import java.util.Set;

public interface TemplateProviderConfig<M extends ConfigMap> extends ProviderConfig<M> {
    public Set<String> getLanguages();

    public String getCustomStyle();
}
