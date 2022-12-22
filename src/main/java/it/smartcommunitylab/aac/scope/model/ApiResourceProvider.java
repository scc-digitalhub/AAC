package it.smartcommunitylab.aac.scope.model;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public interface ApiResourceProvider<R extends ApiResource> extends ResourceProvider<R> {

    public R findResourceByIdentifier(String resource);

    public R findResource(String resourceId);

    public R getResource(String resourceId) throws NoSuchResourceException;

    public Collection<R> listResources();

}
