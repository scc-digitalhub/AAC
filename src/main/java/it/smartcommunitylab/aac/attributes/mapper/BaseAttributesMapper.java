package it.smartcommunitylab.aac.attributes.mapper;

import it.smartcommunitylab.aac.core.base.DefaultAttributesImpl;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

public abstract class BaseAttributesMapper implements AttributesMapper {

    protected final AttributeSet model;

    public BaseAttributesMapper(AttributeSet attributeSet) {
        Assert.notNull(attributeSet, "destination attribute set can not be null");
        this.model = attributeSet;
    }

    @Override
    public String getIdentifier() {
        return model.getIdentifier();
    }

    @Override
    public AttributeSet mapAttributes(Map<String, Serializable> attributesMap) {
        // create new set

        List<Attribute> attributes = new ArrayList<>();
        for (Attribute a : model.getAttributes()) {
            // TODO handle multiple
            Attribute attr = getAttribute(a, attributesMap);
            if (attr != null) {
                attributes.add(attr);
            }
        }

        DefaultAttributesImpl ua = new DefaultAttributesImpl(model.getIdentifier(), attributes);
        ua.setName(model.getName());
        ua.setDescription(model.getDescription());
        return ua;
    }

    protected abstract Attribute getAttribute(Attribute attribute, Map<String, Serializable> attributes);
}
