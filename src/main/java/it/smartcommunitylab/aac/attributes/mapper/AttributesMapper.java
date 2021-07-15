package it.smartcommunitylab.aac.attributes.mapper;

import java.io.Serializable;
import java.util.Map;

import it.smartcommunitylab.aac.core.model.AttributeSet;

/*
 * (User) attributes mapper
 * Converts from principal attributes to a given attribute set
 */

public interface AttributesMapper {

    public String getIdentifier();

    public AttributeSet mapAttributes(Map<String, Serializable> attributes);

}
