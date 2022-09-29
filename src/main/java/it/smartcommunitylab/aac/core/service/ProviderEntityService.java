package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;

public interface ProviderEntityService<P extends ProviderEntity> {

    public List<P> listProviders();

    public List<P> listProvidersByAuthority(String authority);

    public List<P> listProvidersByRealm(String realm);

    public List<P> listProvidersByAuthorityAndRealm(String authority, String realm);

    public P findProvider(String providerId);

    public P getProvider(String providerId) throws NoSuchProviderException;

    public P saveProvider(String providerId, P reg, Map<String, Serializable> configuration);

    public void deleteProvider(String providerId);
}
