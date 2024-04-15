/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.identity.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.base.service.AbstractConfigurableAuthorityService;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.identity.IdentityProviderAuthority;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderConfig;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.users.model.UserAuthenticatedPrincipal;
import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

//@Service
public class IdentityProviderAuthorityService
    extends AbstractConfigurableAuthorityService<
        IdentityProviderAuthority<
            ? extends IdentityProvider<
                ? extends UserIdentity,
                ? extends UserAccount,
                ? extends UserAuthenticatedPrincipal,
                ? extends ConfigMap,
                ? extends IdentityProviderConfig<? extends ConfigMap>
            >,
            ? extends IdentityProviderConfig<? extends ConfigMap>,
            ? extends ConfigMap
        >,
        IdentityProviderSettingsMap
    >
    implements InitializingBean {

    public IdentityProviderAuthorityService(
        Collection<
            IdentityProviderAuthority<
                ? extends IdentityProvider<
                    ? extends UserIdentity,
                    ? extends UserAccount,
                    ? extends UserAuthenticatedPrincipal,
                    ? extends ConfigMap,
                    ? extends IdentityProviderConfig<? extends ConfigMap>
                >,
                ? extends IdentityProviderConfig<? extends ConfigMap>,
                ? extends ConfigMap
            >
        > authorities
    ) {
        super(SystemKeys.RESOURCE_IDENTITY);
        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one identity provider authority is required");
    }
}
