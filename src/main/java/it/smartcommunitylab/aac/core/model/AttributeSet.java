package it.smartcommunitylab.aac.core.model;

import java.util.Collection;

/*
 * An attribute set defining properties related to entities.
 * When used in protected scenarios, access to set content will be filtered based on scopes and authorizations
 */
public interface AttributeSet {

    /*
     * The set identifier should match a scope, which when approved will enable
     * access to this set
     */
    public String getIdentifier();

    /*
     * The set keys
     */
    public Collection<String> getKeys();

    /*
     * The attribute list
     */
    public Collection<Attribute> getAttributes();

}
