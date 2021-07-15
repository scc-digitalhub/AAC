package it.smartcommunitylab.aac.attributes.mapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.base.DefaultAttributesImpl;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;

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

        return new DefaultAttributesImpl(model.getIdentifier(), attributes);

    }

    protected abstract Attribute getAttribute(Attribute attribute, Map<String, Serializable> attributes);

}
