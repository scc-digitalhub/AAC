package it.smartcommunitylab.aac.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.dto.RealmConfig;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

//@Configuration
//@PropertySource(factory = JacksonPropertySourceFactory.class, ignoreResourceNotFound = true, value = "${bootstrap.file}")
//@ConfigurationProperties
//@Validated
@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BootstrapConfig {

    private List<RealmConfig> realms;

    public BootstrapConfig() {
        this.realms = new ArrayList<>();
    }

    public List<RealmConfig> getRealms() {
        return realms;
    }

    public void setRealms(List<RealmConfig> realms) {
        this.realms = realms;
    }
}
