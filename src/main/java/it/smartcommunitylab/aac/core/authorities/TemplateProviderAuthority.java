package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.TemplateProvider;
import it.smartcommunitylab.aac.core.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;

public interface TemplateProviderAuthority<S extends TemplateProvider, M extends ConfigMap, C extends TemplateProviderConfig<M>>
        extends ProviderAuthority<S, Template, ConfigurableTemplateProvider, M, C> {

}
