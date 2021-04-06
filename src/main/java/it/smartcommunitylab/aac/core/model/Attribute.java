package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.model.AttributeType;

/*
 * An attribute is a typed property describing a value for a given resource.
 * 
 * While unusual, attributes may assume multiple values. 
 * In this case we expect the value to be a Collection of same-type content
 */

public interface Attribute {

    public String getKey();

    public AttributeType getType();

    public boolean isMultiple();

    public Serializable getValue();

}
