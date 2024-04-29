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

package it.smartcommunitylab.aac.accounts.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.AccountServiceAuthority;
import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceConfig;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.base.service.AbstractConfigurableAuthorityService;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class AccountServiceAuthorityService
    extends AbstractConfigurableAuthorityService<
        AccountServiceAuthority<
            ? extends AccountService<
                ? extends UserAccount,
                ? extends EditableUserAccount,
                ? extends ConfigMap,
                ? extends AccountServiceConfig<? extends ConfigMap>
            >,
            ? extends UserAccount,
            ? extends EditableUserAccount,
            ? extends AccountServiceConfig<? extends ConfigMap>,
            ? extends ConfigMap
        >,
        AccountServiceSettingsMap
    >
    implements InitializingBean {

    public AccountServiceAuthorityService(
        Collection<
            AccountServiceAuthority<
                ? extends AccountService<
                    ? extends UserAccount,
                    ? extends EditableUserAccount,
                    ? extends ConfigMap,
                    ? extends AccountServiceConfig<? extends ConfigMap>
                >,
                ? extends UserAccount,
                ? extends EditableUserAccount,
                ? extends AccountServiceConfig<? extends ConfigMap>,
                ? extends ConfigMap
            >
        > authorities
    ) {
        super(SystemKeys.RESOURCE_ACCOUNT);
        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one provider authority is required");
    }
}
