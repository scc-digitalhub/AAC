package it.smartcommunitylab.aac.attributes.mapper;

import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import java.util.ArrayList;
import java.util.List;

public class OpenIdAttributesMapper extends DefaultAttributesMapper {

    private static final OpenIdAttributesSet set;

    public OpenIdAttributesMapper() {
        super(set);
    }

    @Override
    public String getIdentifier() {
        return OpenIdAttributesSet.IDENTIFIER;
    }

    static {
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new StringAttribute(OpenIdAttributesSet.NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.GIVEN_NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.FAMILY_NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.MIDDLE_NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.NICKNAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PREFERRED_USERNAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.EMAIL));
        attributes.add(new BooleanAttribute(OpenIdAttributesSet.EMAIL_VERIFIED));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PHONE_NUMBER));
        attributes.add(new BooleanAttribute(OpenIdAttributesSet.PHONE_NUMBER_VERIFIED));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PROFILE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PICTURE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.WEBSITE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.GENDER));
        attributes.add(new DateAttribute(OpenIdAttributesSet.BIRTHDATE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.ZONEINFO));
        attributes.add(new StringAttribute(OpenIdAttributesSet.LOCALE));

        set = new OpenIdAttributesSet(attributes);
    }
}
