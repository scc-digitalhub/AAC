package it.smartcommunitylab.aac.internal;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.core.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeService;
import it.smartcommunitylab.aac.internal.service.InternalAttributeEntityService;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfigMap;

@Service
public class InternalAttributeAuthority extends
        AbstractAttributeAuthority<InternalAttributeService, InternalAttributeProviderConfigMap, InternalAttributeProviderConfig> {

    private final InternalAttributeEntityService attributeEntityService;

    public InternalAttributeAuthority(
            AttributeService attributeService,
            InternalAttributeEntityService attributeEntityService,
            ProviderConfigRepository<InternalAttributeProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_INTERNAL, attributeService, registrationRepository);
        Assert.notNull(attributeEntityService, "attribute entity service is mandatory");

        this.attributeEntityService = attributeEntityService;
    }

    @Override
    protected InternalAttributeService buildProvider(InternalAttributeProviderConfig config) {
        InternalAttributeService ap = new InternalAttributeService(
                config.getProvider(),
                attributeService, attributeEntityService,
                config,
                config.getRealm());

        return ap;
    }
}
