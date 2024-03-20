package it.smartcommunitylab.aac.spid.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SpidAttribute {
    SPID_CODE("spidCode"),
    NAME("name"),
    FAMILY_NAME("familyName"),
    PLACE_OF_BIRTH("placeOfBirth"),
    COUNTRY_OF_BIRTH("countyOfBirth"),
    DATE_OF_BIRTH("dateOfBirth"),
    GENDER("gender"),
    COMPANY_NAME("companyName"),
    REGISTERED_OFFICE("registeredOffice"),
    FISCAL_NUMBER("fiscalNumber"),
    IVA_CODE("ivaCode"),
    ID_CARD("idCard"),
    MOBILE_PHONE("mobilePhone"),
    EMAIL("email"),
    DOMICILE_STREET_ADDRESS("domicileStreetAddress"),
    DOMICILE_POSTAL_CODE("domicilePostalCode"),
    DOMICILE_MUNICIPALITY("domicileMunicipality"),
    DOMICILE_PROVINCE("domicileProvince"),
    ADDRESS("address"),
    DOMICILE_NATION("domicileNation"),
    EXPIRATION_DATE("expirationDate"),
    DIGITAL_ADDRESS("digitalAddress");

    private final String value;

    SpidAttribute(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String toString() {
        return value;
    }

    public static SpidAttribute parse(String value) {
        for (SpidAttribute t : SpidAttribute.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
