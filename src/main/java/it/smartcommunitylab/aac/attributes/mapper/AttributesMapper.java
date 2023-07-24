package it.smartcommunitylab.aac.attributes.mapper;

import it.smartcommunitylab.aac.core.model.AttributeSet;
import java.io.Serializable;
import java.util.Map;

/*
 * (User) attributes mapper
 * Converts from principal attributes to a given attribute set
 */

public interface AttributesMapper {
    public String getIdentifier();

    public AttributeSet mapAttributes(Map<String, Serializable> attributes);
}
