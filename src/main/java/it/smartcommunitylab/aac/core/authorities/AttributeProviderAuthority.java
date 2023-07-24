package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProviderConfig;

public interface AttributeProviderAuthority<
    S extends AttributeProvider<M, C>, M extends ConfigMap, C extends AttributeProviderConfig<M>
>
    extends ConfigurableProviderAuthority<S, UserAttributes, ConfigurableAttributeProvider, M, C> {
    //
    //    /*
    //     * identify
    //     */
    //    public String getAuthorityId();
    //
    //    /*
    //     * Providers
    //     */
    //    public boolean hasAttributeProvider(String providerId);
    //
    //    public AttributeProvider getAttributeProvider(String providerId) throws NoSuchProviderException;
    //
    //    public List<AttributeProvider> getAttributeProviders(String realm);
    //
    //    /*
    //     * Manage providers
    //     *
    //     * we expect providers to be registered and usable, or removed. To update config
    //     * implementations should unregister+register
    //     */
    //    public AttributeProvider registerAttributeProvider(ConfigurableAttributeProvider cp)
    //            throws IllegalArgumentException, RegistrationException, SystemException;
    //
    //    public void unregisterAttributeProvider(String providerId) throws SystemException;
    //
    //    /*
    //     * Services
    //     */
    //    public AttributeService getAttributeService(String providerId);
    //
    //    public List<AttributeService> getAttributeServices(String realm);

    /*
     * Config provider exposes configuration validation and schema
     */
    //    public AttributeConfigurationProvider<C, P> getConfigurationProvider();
}
