package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.model.AttributeType;

/*
 * An attribute is a typed property describing a value for a given resource.
 * 
 * While unusual, attributes may assume multiple values. 
 */

public interface Attribute extends Serializable {

    public String getKey();

    public AttributeType getType();

    public Serializable getValue();

    public String exportValue();

    public String getName();

    public String getDescription();

    public Boolean getIsMultiple();
}
