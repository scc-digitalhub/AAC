package it.smartcommunitylab.aac.profiles.extractor;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.DateTimeAttribute;
import it.smartcommunitylab.aac.attributes.model.NumberAttribute;
import it.smartcommunitylab.aac.attributes.model.TimeAttribute;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.User;

public abstract class AbstractUserProfileExtractor implements UserProfileExtractor {

    // lookup an attribute in multiple sets, return first match
    protected Attribute getAttribute(Collection<UserAttributes> attributes, String key, String... identifier) {
        return getAttribute(attributes, key, Arrays.asList(identifier));
    }

    protected Attribute getAttribute(Collection<UserAttributes> attributes, String key, Collection<String> identifier) {
        Set<UserAttributes> sets = attributes.stream()
                .filter(a -> identifier.contains(a.getIdentifier()))
                .collect(Collectors.toSet());

        for (UserAttributes uattr : sets) {
            Optional<Attribute> attr = uattr.getAttributes().stream().filter(a -> a.getKey().equals(key)).findFirst();
            if (attr.isPresent()) {
                return attr.get();
            }
        }

        return null;

    }

    protected Collection<UserAttributes> mergeAttributes(User user, String identityId) {
        // fetch user attributes to merge properties
        Collection<UserAttributes> userAttributes = user.getAttributes(false);
        Collection<UserAttributes> idAttributes = user.getIdentities().stream()
                .filter(i -> i.getId().equals(identityId))
                .map(i -> i.getAttributes())
                .flatMap(Collection::stream).collect(Collectors.toList());
        Collection<UserAttributes> additionalAttributes = user.getIdentities().stream()
                .filter(i -> !i.getId().equals(identityId))
                .map(i -> i.getAttributes())
                .flatMap(Collection::stream).collect(Collectors.toList());

        return mergeAttributes(idAttributes, userAttributes, additionalAttributes, true);
    }

    protected Collection<UserAttributes> mergeAttributes(
            Collection<UserAttributes> identityAttributes,
            Collection<UserAttributes> userAttributes,
            Collection<UserAttributes> additionalAttributes,
            boolean mergeSets) {

        // merge attributes into a single collection
        Map<String, UserAttributes> attributesMap = new HashMap<>();

        // add all identity attributes, we expect a single set per id here
        identityAttributes.forEach(ua -> attributesMap.put(ua.getIdentifier(), ua));

        // always add or merge all sets from user
        userAttributes.forEach(ua -> {
            if (!attributesMap.containsKey(ua.getIdentifier())) {
                attributesMap.put(ua.getIdentifier(), ua);
                return;
            }

            // build a new set merging single attributes
            // user attributes have higher priority
            // do note that when identity attributes are required this method *should not*
            // be used at all
            String id = ua.getIdentifier();
            UserAttributes u = attributesMap.get(id);

            DefaultUserAttributesImpl uma = new DefaultUserAttributesImpl(ua.getAuthority(), ua.getProvider(),
                    ua.getRealm(),
                    ua.getUserId(), id);
            uma.addAttributes(ua.getAttributes());
            // add identity attributes if missing
            u.getAttributes().forEach(a -> {
                boolean exists = ua.getAttributes().stream()
                        .anyMatch(e -> e.getKey().equals(a.getKey()));

                if (!exists) {
                    uma.addAttribute(a);
                }
            });

            attributesMap.put(uma.getIdentifier(), uma);
        });

        // add or merge additional sets
        additionalAttributes.forEach(ua -> {
            if (!attributesMap.containsKey(ua.getIdentifier())) {
                attributesMap.put(ua.getIdentifier(), ua);
                return;
            }
            if (!mergeSets) {
                return;
            }

            // build a new set merging single attributes
            // additional attributes have low priority
            String id = ua.getIdentifier();
            UserAttributes u = attributesMap.get(id);

            DefaultUserAttributesImpl uma = new DefaultUserAttributesImpl(ua.getAuthority(), ua.getProvider(),
                    ua.getRealm(),
                    ua.getUserId(), id);
            // keep all existing
            uma.addAttributes(u.getAttributes());
            // add additional attributes if missing
            ua.getAttributes().forEach(a -> {
                boolean exists = u.getAttributes().stream()
                        .anyMatch(e -> e.getKey().equals(a.getKey()));

                if (!exists) {
                    uma.addAttribute(a);
                }
            });

            attributesMap.put(uma.getIdentifier(), uma);
        });

        return attributesMap.values();
    }

    protected String getStringAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.STRING == attr.getType()) {
            return (String) attr.getValue();
        }

        return String.valueOf(attr.getValue());
    }

    protected Boolean getBooleanAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.BOOLEAN == attr.getType()) {
            return (Boolean) attr.getValue();
        }

        try {
            return BooleanAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected Number getNumberAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.NUMBER == attr.getType()) {
            return (Number) attr.getValue();
        }

        try {
            return NumberAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected LocalDate getDateAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.DATE == attr.getType()) {
            return (LocalDate) attr.getValue();
        }

        try {
            return DateAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected LocalDateTime getDateTimeAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.DATETIME == attr.getType()) {
            return (LocalDateTime) attr.getValue();
        }

        try {
            return DateTimeAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    protected LocalTime getTimeAttribute(Attribute attr) {
        if (attr == null) {
            return null;
        }

        if (attr.getValue() == null) {
            return null;
        }

        if (AttributeType.TIME == attr.getType()) {
            return (LocalTime) attr.getValue();
        }

        try {
            return TimeAttribute.parseValue(attr.getValue());
        } catch (ParseException e) {
            return null;
        }
    }

}
