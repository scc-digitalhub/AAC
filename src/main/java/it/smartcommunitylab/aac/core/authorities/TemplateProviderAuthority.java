package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.core.provider.TemplateProvider;
import it.smartcommunitylab.aac.core.provider.TemplateProviderConfig;

public interface TemplateProviderAuthority<S extends TemplateProvider<T, M, C>, T extends Template, M extends ConfigMap, C extends TemplateProviderConfig<M>>
        extends ProviderAuthority<S, T, ConfigurableTemplateProvider, M, C> {

    public S findProviderByRealm(String realm);

    public S getProviderByRealm(String realm) throws NoSuchProviderException;

}
