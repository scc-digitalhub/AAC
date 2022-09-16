package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;

public interface CredentialsServiceAuthority<S extends UserCredentialsService<R, M, C>, R extends UserCredentials, M extends ConfigMap, C extends CredentialsServiceConfig<M>>
        extends ProviderAuthority<S, UserCredentials, ConfigurableCredentialsService, M, C> {

}
