package it.smartcommunitylab.aac.scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AACApiResource;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.roles.scopes.RolesResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;
import it.smartcommunitylab.aac.scope.base.AbstractResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProvider;

public class InternalApiResourceAuthority implements
        ApiResourceProviderAuthority<AbstractResourceProvider<AbstractInternalApiResource, AbstractInternalApiScope>, AbstractInternalApiResource> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String baseUrl;
    private final Map<String, String> keys;

    public InternalApiResourceAuthority(String baseUrl) {
        Assert.hasText(baseUrl, "baseUrl can not be null or empty");
        this.baseUrl = baseUrl;
        this.keys = new HashMap<>();
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_API_RESOURCE;
    }

    @Override
    public boolean hasProvider(String providerId) {
        // internal providers match the schema
        // (resourceId + SystemKeys.URN_SEPARATOR + realm)
        if (!StringUtils.hasText(providerId) || !providerId.contains(SystemKeys.URN_SEPARATOR)) {
            return false;
        }

        // we expect providers to be registered in keys after load
        return keys.containsKey(providerId);
    }

    @Override
    public AbstractResourceProvider<AbstractInternalApiResource, AbstractInternalApiScope> findProvider(
            String providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AbstractResourceProvider<AbstractInternalApiResource, AbstractInternalApiScope> getProvider(
            String providerId) throws NoSuchProviderException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<AbstractResourceProvider<AbstractInternalApiResource, AbstractInternalApiScope>> getProvidersByRealm(
            String realm) {
        // TODO Auto-generated method stub
        return null;
    }

    private Map<String, AbstractResourceProvider<? extends AbstractInternalApiResource, ? extends AbstractInternalApiScope>> buildRealm(
            String realm) {
        // statically build all resources for realm
        Map<String, AbstractResourceProvider<? extends AbstractInternalApiResource, ? extends AbstractInternalApiScope>> res = new HashMap<>();
        InternalApiResourceProvider apiResourceProvider = new InternalApiResourceProvider(new AACApiResource(realm, baseUrl));
        res.put(apiResourceProvider.getProvider(), apiResourceProvider);
        return res;
    }
}
