package it.smartcommunitylab.aac.spid.attributes;

import java.io.Serializable;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.AttributesMapper;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;

public class OpenIdAttributesMapper implements AttributesMapper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String getIdentifier() {
        return OpenIdAttributesSet.IDENTIFIER;
    }

    @Override
    public AttributeSet mapAttributes(Map<String, Serializable> attributes) {
        // map from spid attributes to openId, exact match only
        OpenIdAttributesSet set = new OpenIdAttributesSet();

        // fetch all attributes, skip null
        try {
            String name = attributes.containsKey(SpidAttributesSet.NAME)
                    ? StringAttribute.parseValue(attributes.get(SpidAttributesSet.NAME))
                    : null;

            String givenName = name;
            set.setGivenName(givenName);

            String familyName = attributes.containsKey(SpidAttributesSet.FAMILY_NAME)
                    ? StringAttribute.parseValue(attributes.get(SpidAttributesSet.FAMILY_NAME))
                    : null;
            set.setFamilyName(familyName);

            if (StringUtils.hasText(givenName) && StringUtils.hasText(familyName)) {
                name = givenName + " " + familyName;
            }
            set.setName(name);

            String email = attributes.containsKey(SpidAttributesSet.EMAIL)
                    ? StringAttribute.parseValue(attributes.get(SpidAttributesSet.EMAIL))
                    : null;
            set.setEmail(email);
            if (StringUtils.hasText(email)) {
                set.setEmailVerified(true);
                set.setPreferredUsername(email);
            }

            String phoneNumber = attributes.containsKey(SpidAttributesSet.MOBILE_PHONE)
                    ? StringAttribute.parseValue(attributes.get(SpidAttributesSet.MOBILE_PHONE))
                    : null;
            set.setPhoneNumber(phoneNumber);
            if (StringUtils.hasText(phoneNumber)) {
                set.setPhoneVerified(true);
            }

            String gender = attributes.containsKey(SpidAttributesSet.GENDER)
                    ? StringAttribute.parseValue(attributes.get(SpidAttributesSet.GENDER))
                    : null;
            set.setGender(gender);

            LocalDate birthdate = attributes.containsKey(SpidAttributesSet.DATE_OF_BIRTH)
                    ? DateAttribute.parseValue(attributes.get(SpidAttributesSet.DATE_OF_BIRTH))
                    : null;
            set.setBirthdate(birthdate);

            // assume spid users are IT
            String zoneinfo = "Europe/Rome";
            set.setZoneinfo(zoneinfo);
            String locale = Locale.ITALIAN.getLanguage();
            set.setLocale(locale);

        } catch (ParseException e) {
            logger.debug("parse error for field " + e.getMessage());

        }

        return set;
    }

}
