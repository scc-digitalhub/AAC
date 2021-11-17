package it.smartcommunitylab.aac.spid.attributes;

import java.util.ArrayList;
import java.util.List;

import it.smartcommunitylab.aac.attributes.mapper.ExactAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;

public class SpidAttributesMapper extends ExactAttributesMapper {

    private static final SpidAttributesSet set;

    public SpidAttributesMapper() {
        super(set);
    }

    @Override
    public String getIdentifier() {
        return SpidAttributesSet.IDENTIFIER;
    }

    static {
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new StringAttribute(SpidAttributesSet.SPID_CODE));
        attributes.add(new StringAttribute(SpidAttributesSet.NAME));
        attributes.add(new StringAttribute(SpidAttributesSet.FAMILY_NAME));
        attributes.add(new StringAttribute(SpidAttributesSet.PLACE_OF_BIRTH));
        attributes.add(new StringAttribute(SpidAttributesSet.COUNTRY_OF_BIRTH));
        attributes.add(new DateAttribute(SpidAttributesSet.DATE_OF_BIRTH));
        attributes.add(new StringAttribute(SpidAttributesSet.GENDER));
        attributes.add(new StringAttribute(SpidAttributesSet.COMPANY_NAME));
        attributes.add(new StringAttribute(SpidAttributesSet.REGISTERED_OFFICE));
        attributes.add(new StringAttribute(SpidAttributesSet.FISCAL_NUMBER));
        attributes.add(new BooleanAttribute(SpidAttributesSet.IVA_CODE));
        attributes.add(new StringAttribute(SpidAttributesSet.ID_CARD));
        attributes.add(new BooleanAttribute(SpidAttributesSet.MOBILE_PHONE));
        attributes.add(new StringAttribute(SpidAttributesSet.EMAIL));
        attributes.add(new StringAttribute(SpidAttributesSet.DOMICILE_STREET_ADDRESS));
        attributes.add(new StringAttribute(SpidAttributesSet.DOMICILE_POSTAL_CODE));
        attributes.add(new StringAttribute(SpidAttributesSet.DOMICILE_MUNICIPALITY));
        attributes.add(new StringAttribute(SpidAttributesSet.DOMICILE_PROVINCE));
        attributes.add(new StringAttribute(SpidAttributesSet.DOMICILE_NATION));
        attributes.add(new StringAttribute(SpidAttributesSet.ADDRESS));
        attributes.add(new DateAttribute(SpidAttributesSet.EXPIRATION_DATE));
        attributes.add(new StringAttribute(SpidAttributesSet.DIGITAL_ADDRESS));

        set = new SpidAttributesSet(attributes);
    }

}
