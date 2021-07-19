package it.smartcommunitylab.aac.bootstrap;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.services.Service;

//@Configuration
//@PropertySource(factory = YamlPropertySourceFactory.class, ignoreResourceNotFound = true, value = "${bootstrap.file}")
//@ConfigurationProperties(prefix = "bootstrap")
@Validated
@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BootstrapConfig {

//    @NestedConfigurationProperty
    private List<Realm> realms;

//    @NestedConfigurationProperty
    private List<ConfigurableProvider> providers;

//    @NestedConfigurationProperty
    private List<ClientApp> clients;

    private List<Service> services;

//    @NestedConfigurationProperty
    private UsersConfig users;

    public BootstrapConfig() {
        this.realms = new ArrayList<>();
        this.clients = new ArrayList<>();
        this.providers = new ArrayList<>();
        this.users = new UsersConfig();
    }

    public List<Realm> getRealms() {
        return realms;
    }

    public void setRealms(List<Realm> realms) {
        this.realms = realms;
    }

    public List<ConfigurableProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<ConfigurableProvider> providers) {
        this.providers = providers;
    }

    public List<ClientApp> getClients() {
        return clients;
    }

    public void setClients(List<ClientApp> clients) {
        this.clients = clients;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public UsersConfig getUsers() {
        return users;
    }

    public void setUsers(UsersConfig users) {
        this.users = users;
    }

    public class UsersConfig {
        @NestedConfigurationProperty
        private List<InternalUserAccount> internal;

        public UsersConfig() {
            this.internal = new ArrayList<>();
        }

        public List<InternalUserAccount> getInternal() {
            return internal;
        }

        public void setInternal(List<InternalUserAccount> internal) {
            this.internal = internal;
        }

    }

}
