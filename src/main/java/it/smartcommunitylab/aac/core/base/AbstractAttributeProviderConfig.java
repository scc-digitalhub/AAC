package it.smartcommunitylab.aac.core.base;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfig;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = MapperAttributeProviderConfig.class, name = MapperAttributeProviderConfig.RESOURCE_TYPE),
        @Type(value = ScriptAttributeProviderConfig.class, name = ScriptAttributeProviderConfig.RESOURCE_TYPE),
        @Type(value = WebhookAttributeProviderConfig.class, name = WebhookAttributeProviderConfig.RESOURCE_TYPE),
        @Type(value = InternalAttributeProviderConfig.class, name = InternalAttributeProviderConfig.RESOURCE_TYPE),
})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractAttributeProviderConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableAttributeProvider>
        implements AttributeProviderConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String persistence;
    protected String events;

    protected Set<String> attributeSets;

    protected AbstractAttributeProviderConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
        this.attributeSets = Collections.emptySet();
    }

    protected AbstractAttributeProviderConfig(ConfigurableAttributeProvider cp, M configMap) {
        super(cp, configMap);

        this.persistence = cp.getPersistence();
        this.events = cp.getEvents();

        this.attributeSets = (cp.getAttributeSets() != null ? cp.getAttributeSets() : Collections.emptySet());
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

    public Set<String> getAttributeSets() {
        return attributeSets;
    }

    public void setAttributeSets(Set<String> attributeSets) {
        this.attributeSets = attributeSets;
    }

}
