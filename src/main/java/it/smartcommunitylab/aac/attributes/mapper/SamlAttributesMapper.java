package it.smartcommunitylab.aac.attributes.mapper;

import it.smartcommunitylab.aac.attributes.SamlAttributesSet;
import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import java.util.ArrayList;
import java.util.List;

public class SamlAttributesMapper extends DefaultAttributesMapper {

    private static final SamlAttributesSet set;

    public SamlAttributesMapper() {
        super(set);
    }

    @Override
    public String getIdentifier() {
        return SamlAttributesSet.IDENTIFIER;
    }

    static {
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new StringAttribute(SamlAttributesSet.NAME));
        attributes.add(new StringAttribute(SamlAttributesSet.SURNAME));
        attributes.add(new StringAttribute(SamlAttributesSet.USERNAME));
        attributes.add(new StringAttribute(SamlAttributesSet.EMAIL));
        attributes.add(new BooleanAttribute(SamlAttributesSet.EMAIL_VERIFIED));

        set = new SamlAttributesSet(attributes);
    }
}
