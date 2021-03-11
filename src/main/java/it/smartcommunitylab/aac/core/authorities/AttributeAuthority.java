package it.smartcommunitylab.aac.core.authorities;

import java.util.List;

import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;

public interface AttributeAuthority {

    /*
     * identify
     */
    public String getAuthorityId();

    /*
     * Providers
     */

    public AttributeProvider getAttributeProvider(String providerId);

    public List<AttributeProvider> getAttributeProviders(String realm);

    /*
     * Manage providers
     * 
     * we expect providers to be registered and usable, or removed. To update config
     * implementations should unregister+register
     */
    public void registerAttributeProvider(ConfigurableProvider idp);

    public void unregisterAttributeProvider(String providerId);

}
