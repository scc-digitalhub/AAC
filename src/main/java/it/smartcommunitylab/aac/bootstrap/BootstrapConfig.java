package it.smartcommunitylab.aac.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;

//@Configuration
//@PropertySource(factory = YamlPropertySourceFactory.class, ignoreResourceNotFound = true, value = "${bootstrap.file}")
//@ConfigurationProperties(prefix = "bootstrap")
@Validated
public class BootstrapConfig {

    @NestedConfigurationProperty
    private List<Realm> realms;

    @NestedConfigurationProperty
    private List<ConfigurableProvider> providers;

    @NestedConfigurationProperty
    private List<ClientApp> clients;

    public BootstrapConfig() {
        this.realms = new ArrayList<>();
        this.clients = new ArrayList<>();
        this.providers = new ArrayList<>();
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

}
