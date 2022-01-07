package it.smartcommunitylab.aac.attributes.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.Attribute;

public abstract class AbstractAttribute implements Attribute, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    protected String key;

    protected String name;
    protected String description;

    @JsonProperty("multiple")
    private Boolean isMultiple;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

	public Boolean getIsMultiple() {
		return isMultiple;
	}

	public void setIsMultiple(Boolean isMultiple) {
		this.isMultiple = isMultiple;
	}
    
}
