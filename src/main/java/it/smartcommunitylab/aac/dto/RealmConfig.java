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

package it.smartcommunitylab.aac.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import it.smartcommunitylab.aac.base.model.AbstractUserAccount;
import it.smartcommunitylab.aac.base.model.AbstractUserCredentials;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.services.Service;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.util.Assert;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealmConfig {

    // realm
    @JsonUnwrapped
    @NotNull
    private Realm realm;

    // providers config
    private List<ConfigurableIdentityProvider> identityProviders;
    private List<ConfigurableAttributeProvider> attributeProviders;
    private ConfigurableTemplateProvider templates;

    // services
    private List<Service> services;

    // clientApps
    private List<ClientApp> clientApps;

    // user accounts
    private List<AbstractUserAccount> users;

    // credentials
    private List<AbstractUserCredentials> credentials;

    public RealmConfig() {}

    public RealmConfig(Realm r) {
        Assert.notNull(r, "realm can not be null");
        this.realm = r;
    }

    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public List<ConfigurableIdentityProvider> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<ConfigurableIdentityProvider> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public List<ConfigurableAttributeProvider> getAttributeProviders() {
        return attributeProviders;
    }

    public void setAttributeProviders(List<ConfigurableAttributeProvider> attributeProviders) {
        this.attributeProviders = attributeProviders;
    }

    public ConfigurableTemplateProvider getTemplates() {
        return templates;
    }

    public void setTemplates(ConfigurableTemplateProvider templates) {
        this.templates = templates;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public List<ClientApp> getClientApps() {
        return clientApps;
    }

    public void setClientApps(List<ClientApp> clientApps) {
        this.clientApps = clientApps;
    }

    public List<AbstractUserAccount> getUsers() {
        return users;
    }

    public void setUsers(List<AbstractUserAccount> users) {
        this.users = users;
    }

    public List<AbstractUserCredentials> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<AbstractUserCredentials> credentials) {
        this.credentials = credentials;
    }
}
