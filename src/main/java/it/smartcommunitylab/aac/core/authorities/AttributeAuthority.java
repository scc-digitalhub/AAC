package it.smartcommunitylab.aac.core.authorities;

import java.util.List;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeService;

public interface AttributeAuthority {

    /*
     * identify
     */
    public String getAuthorityId();

    /*
     * Providers
     */
    public boolean hasAttributeProvider(String providerId);

    public AttributeProvider getAttributeProvider(String providerId) throws NoSuchProviderException;

    public List<AttributeProvider> getAttributeProviders(String realm);

    /*
     * Manage providers
     * 
     * we expect providers to be registered and usable, or removed. To update config
     * implementations should unregister+register
     */
    public AttributeProvider registerAttributeProvider(ConfigurableAttributeProvider cp)
            throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterAttributeProvider(String providerId) throws SystemException;

    /*
     * Services
     */
    public AttributeService getAttributeService(String providerId);

    public List<AttributeService> getAttributeServices(String realm);

}
