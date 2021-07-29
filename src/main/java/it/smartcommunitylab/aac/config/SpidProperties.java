package it.smartcommunitylab.aac.config;

import java.util.List;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import it.smartcommunitylab.aac.spid.model.SpidRegistration;

public class SpidProperties {

    @NestedConfigurationProperty
    private List<SpidRegistration> idps;

    public List<SpidRegistration> getIdps() {
        return idps;
    }

    public void setIdps(List<SpidRegistration> idps) {
        this.idps = idps;
    }

}
