package it.smartcommunitylab.aac.attributes.mapper;

import it.smartcommunitylab.aac.attributes.SpidAttributeSet;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;

import java.util.ArrayList;
import java.util.List;

public class SpidAttributesMapper extends DefaultAttributesMapper {

    private static final SpidAttributeSet set;

    public SpidAttributesMapper() {
        super(set);
    }

    @Override
    public String getIdentifier() {
        return SpidAttributeSet.IDENTIFIER;
    }

    static {
        List<Attribute> attributes = new ArrayList<>(){{
            add(new StringAttribute(SpidAttributeSet.NAME));
            add(new StringAttribute(SpidAttributeSet.SPID_CODE));
            add(new StringAttribute(SpidAttributeSet.FAMILY_NAME));
            add(new StringAttribute(SpidAttributeSet.PLACE_OF_BIRTH));
            add(new StringAttribute(SpidAttributeSet.COUNTRY_OF_BIRTH));
            add(new DateAttribute(SpidAttributeSet.DATE_OF_BIRTH));
            add(new StringAttribute(SpidAttributeSet.GENDER));
            add(new StringAttribute(SpidAttributeSet.COMPANY_NAME));
            add(new StringAttribute(SpidAttributeSet.REGISTERED_OFFICE));
            add(new StringAttribute(SpidAttributeSet.FISCAL_NUMBER));
            add(new StringAttribute(SpidAttributeSet.IVA_CODE));
            add(new StringAttribute(SpidAttributeSet.ID_CARD));
            add(new StringAttribute(SpidAttributeSet.MOBILE_PHONE));
            add(new StringAttribute(SpidAttributeSet.EMAIL));
            add(new StringAttribute(SpidAttributeSet.DOMICILE_STREET_ADDRESS));
            add(new StringAttribute(SpidAttributeSet.DOMICILE_POSTAL_CODE));
            add(new StringAttribute(SpidAttributeSet.DOMICILE_MUNICIPALITY));
            add(new StringAttribute(SpidAttributeSet.DOMICILE_PROVINCE));
            add(new StringAttribute(SpidAttributeSet.ADDRESS));
            add(new StringAttribute(SpidAttributeSet.DOMICILE_NATION));
            add(new DateAttribute(SpidAttributeSet.EXPIRATION_DATE));
            add(new StringAttribute(SpidAttributeSet.DIGITAL_ADDRESS));
        }};
        set = new SpidAttributeSet(attributes);

    }
}
