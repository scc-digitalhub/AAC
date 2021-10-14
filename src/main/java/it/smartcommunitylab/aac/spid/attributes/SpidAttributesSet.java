package it.smartcommunitylab.aac.spid.attributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;

@Component
public class SpidAttributesSet implements AttributeSet {
    public static final String IDENTIFIER = "aac.spid";
    public static final List<String> keys;

    static {
        List<String> k = Arrays.stream(SpidAttribute.values()).map(a -> a.getValue()).collect(Collectors.toList());
        keys = Collections.unmodifiableList(k);
    }

    private Map<String, Attribute> attributes;

    public SpidAttributesSet() {
        this.attributes = new HashMap<>();
    }

    public SpidAttributesSet(Collection<Attribute> attrs) {
        this.attributes = new HashMap<>();
        if (attrs != null) {
            attrs.forEach(a -> this.attributes.put(a.getKey(), a));
        }
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Collection<String> getKeys() {
        return keys;
    }

    @Override
    public Collection<Attribute> getAttributes() {
        return attributes.values();
    }

    @Override
    public String getName() {
        // TODO i18n
        return "Spid user attribute set";
    }

    @Override
    public String getDescription() {
        return "Spid user attribute set";
    }

    public void setSpidCode(String spidCode) {
        if (spidCode == null) {
            attributes.remove(SPID_CODE);
            return;
        }

        StringAttribute attr = new StringAttribute(SPID_CODE);
        attr.setValue(spidCode);

        attributes.put(SPID_CODE, attr);
    }

    public void setName(String value) {
        if (value == null) {
            attributes.remove(NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(NAME);
        attr.setValue(value);

        attributes.put(NAME, attr);
    }

    public void setFamilyName(String value) {
        if (value == null) {
            attributes.remove(FAMILY_NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(FAMILY_NAME);
        attr.setValue(value);

        attributes.put(FAMILY_NAME, attr);
    }

    public void setPlaceOfBirth(String value) {
        if (value == null) {
            attributes.remove(PLACE_OF_BIRTH);
            return;
        }

        StringAttribute attr = new StringAttribute(PLACE_OF_BIRTH);
        attr.setValue(value);

        attributes.put(PLACE_OF_BIRTH, attr);
    }

    public void setCountryOfBirth(String value) {
        if (value == null) {
            attributes.remove(COUNTRY_OF_BIRTH);
            return;
        }

        StringAttribute attr = new StringAttribute(COUNTRY_OF_BIRTH);
        attr.setValue(value);

        attributes.put(COUNTRY_OF_BIRTH, attr);
    }

    public void setDateOfBirth(LocalDate value) {
        if (value == null) {
            attributes.remove(DATE_OF_BIRTH);
            return;
        }

        DateAttribute attr = new DateAttribute(DATE_OF_BIRTH);
        attr.setValue(value);

        attributes.put(DATE_OF_BIRTH, attr);
    }

    public void setGender(String value) {
        if (value == null) {
            attributes.remove(GENDER);
            return;
        }

        StringAttribute attr = new StringAttribute(GENDER);
        attr.setValue(value);

        attributes.put(GENDER, attr);
    }

    public void setCompanyName(String value) {
        if (value == null) {
            attributes.remove(COMPANY_NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(COMPANY_NAME);
        attr.setValue(value);

        attributes.put(COMPANY_NAME, attr);
    }

    public void setRegisteredOffice(String value) {
        if (value == null) {
            attributes.remove(REGISTERED_OFFICE);
            return;
        }

        StringAttribute attr = new StringAttribute(REGISTERED_OFFICE);
        attr.setValue(value);

        attributes.put(REGISTERED_OFFICE, attr);
    }

    public void setFiscalNumber(String value) {
        if (value == null) {
            attributes.remove(FISCAL_NUMBER);
            return;
        }

        StringAttribute attr = new StringAttribute(FISCAL_NUMBER);
        attr.setValue(value);

        attributes.put(FISCAL_NUMBER, attr);
    }

    public void setIvaCode(String value) {
        if (value == null) {
            attributes.remove(IVA_CODE);
            return;
        }

        StringAttribute attr = new StringAttribute(IVA_CODE);
        attr.setValue(value);

        attributes.put(IVA_CODE, attr);
    }

    public void setIdCard(String value) {
        if (value == null) {
            attributes.remove(ID_CARD);
            return;
        }

        StringAttribute attr = new StringAttribute(ID_CARD);
        attr.setValue(value);

        attributes.put(ID_CARD, attr);
    }

    public void setMobilePhone(String value) {
        if (value == null) {
            attributes.remove(MOBILE_PHONE);
            return;
        }

        StringAttribute attr = new StringAttribute(MOBILE_PHONE);
        attr.setValue(value);

        attributes.put(MOBILE_PHONE, attr);
    }

    public void setEmail(String value) {
        if (value == null) {
            attributes.remove(EMAIL);
            return;
        }

        StringAttribute attr = new StringAttribute(EMAIL);
        attr.setValue(value);

        attributes.put(EMAIL, attr);
    }

    public static final String SPID_CODE = SpidAttribute.SPID_CODE.getValue();
    public static final String NAME = SpidAttribute.NAME.getValue();
    public static final String FAMILY_NAME = SpidAttribute.FAMILY_NAME.getValue();
    public static final String PLACE_OF_BIRTH = SpidAttribute.PLACE_OF_BIRTH.getValue();
    public static final String COUNTRY_OF_BIRTH = SpidAttribute.COUNTRY_OF_BIRTH.getValue();
    public static final String DATE_OF_BIRTH = SpidAttribute.DATE_OF_BIRTH.getValue();
    public static final String GENDER = SpidAttribute.GENDER.getValue();
    public static final String COMPANY_NAME = SpidAttribute.COMPANY_NAME.getValue();
    public static final String REGISTERED_OFFICE = SpidAttribute.REGISTERED_OFFICE.getValue();
    public static final String FISCAL_NUMBER = SpidAttribute.FISCAL_NUMBER.getValue();
    public static final String IVA_CODE = SpidAttribute.IVA_CODE.getValue();
    public static final String ID_CARD = SpidAttribute.ID_CARD.getValue();
    public static final String MOBILE_PHONE = SpidAttribute.MOBILE_PHONE.getValue();
    public static final String EMAIL = SpidAttribute.EMAIL.getValue();
    public static final String DOMICILE_STREET_ADDRESS = SpidAttribute.DOMICILE_STREET_ADDRESS.getValue();
    public static final String DOMICILE_POSTAL_CODE = SpidAttribute.DOMICILE_POSTAL_CODE.getValue();
    public static final String DOMICILE_MUNICIPALITY = SpidAttribute.DOMICILE_MUNICIPALITY.getValue();
    public static final String DOMICILE_PROVINCE = SpidAttribute.DOMICILE_PROVINCE.getValue();
    public static final String ADDRESS = SpidAttribute.ADDRESS.getValue();
    public static final String DOMICILE_NATION = SpidAttribute.DOMICILE_NATION.getValue();
    public static final String EXPIRATION_DATE = SpidAttribute.EXPIRATION_DATE.getValue();
    public static final String DIGITAL_ADDRESS = SpidAttribute.DIGITAL_ADDRESS.getValue();

}