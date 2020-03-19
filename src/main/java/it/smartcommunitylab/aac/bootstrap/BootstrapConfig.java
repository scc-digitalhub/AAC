package it.smartcommunitylab.aac.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import it.smartcommunitylab.aac.dto.ServiceDTO;

@Configuration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:bootstrap.yml")
@ConfigurationProperties(prefix = "bootstrap")
@Validated
public class BootstrapConfig {
    @NestedConfigurationProperty
    private List<BootstrapClient> clients;

    @NestedConfigurationProperty
    private List<BootstrapUser> users;

    @NestedConfigurationProperty
    private List<BootstrapService> services;

    public BootstrapConfig() {
        this.users = new ArrayList<>();
        this.clients = new ArrayList<>();
        this.services = new ArrayList<>();
    }

    public List<BootstrapClient> getClients() {
        return clients;
    }

    public void setClients(List<BootstrapClient> clients) {
        this.clients = clients;
    }

    public List<BootstrapUser> getUsers() {
        return users;
    }

    public void setUsers(List<BootstrapUser> users) {
        this.users = users;
    }

    public List<BootstrapService> getServices() {
        return services;
    }

    public void setServices(List<BootstrapService> services) {
        this.services = services;
    }

}
