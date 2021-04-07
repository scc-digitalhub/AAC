package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Map;

/*
 * A set of attributes for a given user, from an authority via a provider
 */
public interface UserAttributes extends AttributeSet, UserResource, Serializable {

    // mapper to attributeSet
    // TODO?

    // a globally unique identifier for this set for this user
    public String getAttributesId();

}
