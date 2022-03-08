package it.smartcommunitylab.aac.internal.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.mapper.ExactAttributesMapper;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalAttributeEntity;
import it.smartcommunitylab.aac.internal.service.InternalAttributeEntityService;

public class InternalAttributeService extends AbstractProvider
        implements it.smartcommunitylab.aac.core.provider.AttributeService {

    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";

    // services
    private final AttributeService attributeService;
    private final InternalAttributeEntityService attributeEntityService;

    private final InternalAttributeProviderConfig providerConfig;

    public InternalAttributeService(
            String providerId,
            AttributeService attributeService,
            InternalAttributeEntityService attributeEntityService,
            InternalAttributeProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(config, "provider config is mandatory");
        Assert.notNull(attributeService, "attribute service is mandatory");
        Assert.notNull(attributeEntityService, "attribute entity service is mandatory");

        this.attributeService = attributeService;
        this.attributeEntityService = attributeEntityService;

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.providerConfig = config;

        // validate attribute sets, if empty nothing to do
        if (providerConfig.getAttributeSets().isEmpty()) {
            throw new IllegalArgumentException("no attribute sets enabled");
        }
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public String getName() {
        return providerConfig.getName();
    }

    @Override
    public String getDescription() {
        return providerConfig.getDescription();
    }

    @Override
    public Collection<UserAttributes> convertAttributes(UserAuthenticatedPrincipal principal, String subjectId) {

        if (providerConfig.getAttributeSets().isEmpty()) {
            return Collections.emptyList();
        }

        // nothing to process, just fetch attributes already in store
        return getAttributes(subjectId);
    }

    @Override
    public Collection<UserAttributes> getAttributes(String subjectId) {
        List<UserAttributes> result = new ArrayList<>();

        // build sets from stored values
        for (String setId : providerConfig.getAttributeSets()) {
            try {
                AttributeSet as = attributeService.getAttributeSet(setId);
                // fetch from store
                List<InternalAttributeEntity> attributes = attributeEntityService.findAttributes(getProvider(),
                        subjectId,
                        setId);

                // translate to set
                if (!attributes.isEmpty()) {
                    // TODO handle repeatable as enum
                    Map<String, Serializable> principalAttributes = attributes.stream()
                            .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

                    // use exact mapper
                    ExactAttributesMapper mapper = new ExactAttributesMapper(as);
                    AttributeSet set = mapper.mapAttributes(principalAttributes);

                    // build result
                    result.add(new DefaultUserAttributesImpl(
                            getAuthority(), getProvider(), getRealm(), subjectId,
                            set));
                }
            } catch (NoSuchAttributeSetException | RuntimeException e) {
            }
        }

        return result;
    }

    @Override
    public void deleteAttributes(String subjectId) {
        // cleanup from store
        attributeEntityService.deleteAttributes(getProvider(), subjectId);
    }

    @Override
    public void deleteAttributes(String subjectId, String setId) {
        // cleanup matching from store
        attributeEntityService.deleteAttribute(getProvider(), subjectId, setId);
    }

    @Override
    public Collection<UserAttributes> putAttributes(String subjectId, Collection<AttributeSet> attributeSets) {
        List<UserAttributes> result = new ArrayList<>();

        // fetch sets and validate
        for (AttributeSet as : attributeSets) {
            if (!providerConfig.getAttributeSets().contains(as.getIdentifier())) {
                throw new IllegalArgumentException("set not enabled for this provider " + as.getIdentifier());
            }

            // unpack and save to store
            Collection<Attribute> attrs = as.getAttributes();

            // each set will overwrite previously stored values
            List<InternalAttributeEntity> attributes = attributeEntityService.setAttributes(getProvider(), subjectId,
                    as.getIdentifier(), attrs);
            // translate to set
            if (!attributes.isEmpty()) {
                // TODO handle repeatable as enum
                Map<String, Serializable> principalAttributes = attributes.stream()
                        .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

                // use exact mapper
                ExactAttributesMapper mapper = new ExactAttributesMapper(as);
                AttributeSet set = mapper.mapAttributes(principalAttributes);

                // build result
                result.add(new DefaultUserAttributesImpl(
                        getAuthority(), getProvider(), getRealm(), subjectId,
                        set));
            }

        }

        return result;

    }
}
