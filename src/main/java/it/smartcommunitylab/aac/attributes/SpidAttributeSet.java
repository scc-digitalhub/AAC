package it.smartcommunitylab.aac.attributes;

import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;

import java.util.*;

public class SpidAttributeSet implements AttributeSet {

    public static final String IDENTIFIER = "aac.spid";
    private static final List<String> keys;
    private Map<String, Attribute> attributes;

    public SpidAttributeSet() {
        attributes = new HashMap<>();
    }

    public SpidAttributeSet(Collection<Attribute> attrs) {
        attributes = new HashMap<>();
        if (attrs != null) {
            attrs.forEach(a -> attributes.put(a.getKey(), a));
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
        return "SPID (SAML) user attribute set";
    }

    @Override
    public String getDescription() {
        return "SPID (SAML) default user attribute set";
    }

    public static final String SPID_CODE = "spidCode";
    public static final String NAME = "name";
    public static final String FAMILY_NAME = "familyName";
    public static final String PLACE_OF_BIRTH = "placeOfBirth";
    public static final String COUNTRY_OF_BIRTH="countyOfBirth";
    public static final String DATE_OF_BIRTH="dateOfBirth";
    public static final String GENDER="gender";
    public static final String COMPANY_NAME="companyName";
    public static final String REGISTERED_OFFICE="registeredOffice";
    public static final String FISCAL_NUMBER="fiscalNumber";
    public static final String IVA_CODE="ivaCode";
    public static final String ID_CARD="idCard";
    public static final String MOBILE_PHONE="mobilePhone";
    public static final String EMAIL="email";
    public static final String DOMICILE_STREET_ADDRESS="domicileStreetAddress";
    public static final String DOMICILE_POSTAL_CODE="domicilePostalCode";
    public static final String DOMICILE_MUNICIPALITY="domicileMunicipality";
    public static final String DOMICILE_PROVINCE="domicileProvince";
    public static final String ADDRESS="address";
    public static final String DOMICILE_NATION="domicileNation";
    public static final String EXPIRATION_DATE="expirationDate";
    public static final String DIGITAL_ADDRESS="digitalAddress";

    static {
        List<String> k = new ArrayList<>() {{
            add(SPID_CODE);
            add(NAME);
            add(FAMILY_NAME);
            add(PLACE_OF_BIRTH);
            add(COUNTRY_OF_BIRTH);
            add(DATE_OF_BIRTH);
            add(GENDER);
            add(COMPANY_NAME);
            add(REGISTERED_OFFICE);
            add(FISCAL_NUMBER);
            add(IVA_CODE);
            add(ID_CARD);
            add(MOBILE_PHONE);
            add(EMAIL);
            add(DOMICILE_STREET_ADDRESS);
            add(DOMICILE_POSTAL_CODE);
            add(DOMICILE_MUNICIPALITY);
            add(DOMICILE_PROVINCE);
            add(ADDRESS);
            add(DOMICILE_NATION);
            add(EXPIRATION_DATE);
            add(DIGITAL_ADDRESS);
        }};
        keys = Collections.unmodifiableList(k);
    }
}
