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
     * The set keys, as per definition *not content*
     */
    public Collection<String> getKeys();

    /*
     * The attribute list (content)
     */
    public Collection<Attribute> getAttributes();

    /*
     * Human readable
     */
    public String getName();

    public String getDescription();

}
