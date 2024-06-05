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

import it.smartcommunitylab.aac.accounts.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderConfig;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderSettingsMap;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.base.service.AbstractConfigurableProviderService;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountProviderService
    extends AbstractConfigurableProviderService<
        AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>>,
        ConfigurableAccountProvider,
        AccountProviderConfig<ConfigMap>,
        AccountProviderSettingsMap
    > {

    public AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>> findAccountProvider(
        String providerId
    ) {
        return findResourceProvider(providerId);
    }

    public AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>> getAccountProvider(
        String providerId
    ) throws NoSuchProviderException, NoSuchAuthorityException {
        return getResourceProvider(providerId);
    }

    public Collection<
        AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>>
    > listAccountProviders() {
        return listResourceProviders();
    }

    public Collection<
        AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>>
    > listAccountProvidersByRealm(String realm) {
        return listResourceProvidersByRealm(realm);
    }

    @SuppressWarnings("unchecked")
    public AccountService<
        ? extends UserAccount,
        ? extends EditableUserAccount,
        ConfigMap,
        AccountProviderConfig<ConfigMap>
    > findAccountService(String providerId) {
        AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>> p = findResourceProvider(
            providerId
        );
        if (p instanceof AccountService) {
            return (AccountService<
                    ? extends UserAccount,
                    ? extends EditableUserAccount,
                    ConfigMap,
                    AccountProviderConfig<ConfigMap>
                >) p;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public AccountService<
        ? extends UserAccount,
        ? extends EditableUserAccount,
        ConfigMap,
        AccountProviderConfig<ConfigMap>
    > getAccountService(String providerId) throws NoSuchProviderException, NoSuchAuthorityException {
        AccountProvider<? extends UserAccount, ConfigMap, AccountProviderConfig<ConfigMap>> p = getResourceProvider(
            providerId
        );
        if (p instanceof AccountService) {
            return (AccountService<
                    ? extends UserAccount,
                    ? extends EditableUserAccount,
                    ConfigMap,
                    AccountProviderConfig<ConfigMap>
                >) p;
        }

        throw new NoSuchProviderException();
    }

    @SuppressWarnings("unchecked")
    public Collection<
        AccountService<
            ? extends UserAccount,
            ? extends EditableUserAccount,
            ConfigMap,
            AccountProviderConfig<ConfigMap>
        >
    > listAccountServices() {
        return listResourceProviders()
            .stream()
            .filter(AccountService.class::isInstance)
            .map(
                p ->
                    (AccountService<
                            ? extends UserAccount,
                            ? extends EditableUserAccount,
                            ConfigMap,
                            AccountProviderConfig<ConfigMap>
                        >) p
            )
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Collection<
        AccountService<
            ? extends UserAccount,
            ? extends EditableUserAccount,
            ConfigMap,
            AccountProviderConfig<ConfigMap>
        >
    > listAccountServicesByRealm(String realm) {
        return listResourceProvidersByRealm(realm)
            .stream()
            .filter(AccountService.class::isInstance)
            .map(
                p ->
                    (AccountService<
                            ? extends UserAccount,
                            ? extends EditableUserAccount,
                            ConfigMap,
                            AccountProviderConfig<ConfigMap>
                        >) p
            )
            .collect(Collectors.toList());
    }
}
