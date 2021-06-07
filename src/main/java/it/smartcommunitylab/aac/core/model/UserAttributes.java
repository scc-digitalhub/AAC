package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Map;

/*
 * A set of attributes for a given user, from an authority via a provider
 * 
 * Access to content should be protected via a scope "user.<setId>.me"
 * The scope protects access to content and is not directly related to claims.
 * 
 * When used to build "profiles" we expected either a 1-to-1 or a many-to-1
 * relationship between attributeSets and a given profile.
 * Scopes binded to profiles should require implicit/explicit approval of set scope
 * 
 */
public interface UserAttributes extends AttributeSet, UserResource, Serializable {

    // mapper to attributeSet
    // TODO?

    // a globally unique identifier for this set for this user
    public String getAttributesId();

}
