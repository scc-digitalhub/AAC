package it.smartcommunitylab.aac.core.model;

import java.util.Collection;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * A template handles localizable and customizable content for the UI 
 */
public interface Template extends Resource {

    public String getTemplate();

    public String getLanguage();

    public Collection<String> keys();

    public String get(String key);

    default String getProvider() {
        // single provider per authority/realm
        return getAuthority() + "." + getRealm();
    }

    default String getType() {
        return SystemKeys.RESOURCE_TEMPLATE;
    }
}
