package it.smartcommunitylab.aac.core.model;

import java.util.Set;

import javax.validation.Valid;

import org.springframework.boot.context.properties.ConstructorBinding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorBinding
public class ConfigurableAttributeProvider extends ConfigurableProvider {

    private Set<String> attributeSets;

    private String persistence;
    private String events;

    public ConfigurableAttributeProvider(String authority, String provider, String realm) {
        super(authority, provider, realm, SystemKeys.RESOURCE_ATTRIBUTES);
        this.persistence = SystemKeys.PERSISTENCE_LEVEL_NONE;
        this.events = SystemKeys.EVENTS_LEVEL_DETAILS;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableAttributeProvider() {
        this((String) null, (String) null, (String) null);
    }

    @Override
    public void setType(String type) {
        // not supported
    }

    public Set<String> getAttributeSets() {
        return attributeSets;
    }

    public void setAttributeSets(Set<String> attributeSets) {
        this.attributeSets = attributeSets;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

}
