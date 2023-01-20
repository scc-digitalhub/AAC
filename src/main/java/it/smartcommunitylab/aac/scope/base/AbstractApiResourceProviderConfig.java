package it.smartcommunitylab.aac.scope.base;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderConfig;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;
import it.smartcommunitylab.aac.scope.provider.InternalApiResourceProviderConfig;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
//        @Type(value = InternalApiResourceProviderConfig.class, name = InternalApiResourceProviderConfig.RESOURCE_TYPE),
})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractApiResourceProviderConfig<A extends AbstractApiResource<?>, M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableApiResourceProvider> implements ApiResourceProviderConfig<A, M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected A resource;

    protected AbstractApiResourceProviderConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
        this.resource = null;
    }

    protected AbstractApiResourceProviderConfig(ConfigurableApiResourceProvider cp, M configMap) {
        super(cp, configMap);
        this.resource = null;
    }

    protected AbstractApiResourceProviderConfig(A res, M configMap) {
        super(res.getAuthority(), res.getProvider(), res.getRealm(), configMap);
        Assert.notNull(res, "resource can not be null");
        this.resource = res;
    }

    public A getResource() {
        return resource;
    }

}
