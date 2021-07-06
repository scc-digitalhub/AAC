package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;

import it.smartcommunitylab.aac.model.AttributeType;

/*
 * An attribute is a typed property describing a value for a given resource.
 * 
 * While unusual, attributes may assume multiple values. 
 * In this case we expect multiple attributes with the same key define for the same entity
 */

public interface Attribute {

    public String getKey();

    public AttributeType getType();

    public Serializable getValue();

    public String getName();

    public String getDescription();
}
