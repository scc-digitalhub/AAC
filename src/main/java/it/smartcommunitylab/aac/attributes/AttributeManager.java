package it.smartcommunitylab.aac.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.NumberAttribute;
import it.smartcommunitylab.aac.attributes.model.SerializableAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.attributes.persistence.AttributeEntity;
import it.smartcommunitylab.aac.attributes.persistence.AttributeSetEntity;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.NoSuchAttributeException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.model.AttributeType;

/*
 * Attribute manager 
 * 
 * handles attribute sets definition
 * TODO handle attribute sets per *realm*
 * 
 * TODO handle attribute mapping 
 */

@Service
public class AttributeManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AttributeService attributeService;

    /*
     * Attribute sets
     */
    public AttributeSet getAttributeSet(String realm, String identifier) throws NoSuchAttributeSetException {
        logger.debug("get attribute set for id " + identifier);

        AttributeSetEntity se = attributeService.getAttributeSet(identifier);
        if (!realm.equals(se.getRealm())) {
            throw new IllegalArgumentException("set does not match realm");
        }

        List<AttributeEntity> attributes = attributeService.listAttributes(identifier);

        return toSet(se, attributes);

    }

    public AttributeSet findAttributeSet(String realm, String identifier) {
        logger.debug("find attribute set for id " + identifier);

        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            return null;
        }

        if (!realm.equals(se.getRealm())) {
            throw new IllegalArgumentException("set does not match realm");
        }

        List<AttributeEntity> attributes = Collections.emptyList();
        try {
            attributes = attributeService.listAttributes(identifier);
        } catch (NoSuchAttributeSetException e) {
        }
        DefaultAttributesSet a = toSet(se, attributes);

        return a;
    }

    public Collection<AttributeSet> listAttributeSets(String realm) {
        logger.debug("list attribute sets");

        // TODO add static (internal) attribute sets to list
        return attributeService.listAttributeSets(realm).stream().map(s -> {
            DefaultAttributesSet a = toSet(s);
            try {
                a.addAttributes(listAttributes(realm, s.getIdentifier()));
            } catch (NoSuchAttributeSetException e) {
            }
            return a;
        })
                .collect(Collectors.toList());

    }

    public AttributeSet addAttributeSet(String realm, AttributeSet set) {

        String identifier = set.getIdentifier();
        if (!StringUtils.hasText(identifier) || isReserved(identifier)) {
            throw new IllegalArgumentException("invalid set identifier");
        }

        logger.debug("add attribute set " + identifier);
        AttributeSetEntity se = attributeService.addAttributeSet(realm, identifier, set.getName(),
                set.getDescription());
        List<Attribute> attrs = new ArrayList<>();
        if (set.getAttributes() != null) {
            try {
                for (Attribute attr : set.getAttributes()) {

                    AttributeEntity ae = attributeService.addAttribute(identifier, attr.getKey(),
                            attr.getType(), null,
                            attr.getName(), attr.getDescription());

                    attrs.add(toAttribute(ae));
                }
            } catch (NoSuchAttributeSetException e) {
            }
        }

        DefaultAttributesSet a = toSet(se);
        a.addAttributes(attrs);

        return a;

    }

    public AttributeSet updateAttributeSet(String realm, String identifier, AttributeSet set)
            throws NoSuchAttributeSetException {

        logger.debug("update attribute set " + identifier);

        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            throw new NoSuchAttributeSetException();
        }
        if (!realm.equals(se.getRealm())) {
            throw new IllegalArgumentException("set does not match realm");
        }

        se = attributeService.updateAttributeSet(identifier, set.getName(), set.getDescription());
        List<Attribute> attrs = new ArrayList<>();
        if (set.getAttributes() != null) {

            Set<String> toRemove = attributeService.listAttributes(identifier).stream().map(a -> a.getKey())
                    .collect(Collectors.toSet());

            for (Attribute attr : set.getAttributes()) {
                AttributeEntity ae = attributeService.findAttribute(identifier, attr.getKey());
                if (ae == null) {
                    ae = attributeService.addAttribute(identifier, attr.getKey(),
                            attr.getType(), null,
                            attr.getName(), attr.getDescription());
                } else {
                    try {
                        ae = attributeService.updateAttribute(identifier, attr.getKey(),
                                attr.getType(), null,
                                attr.getName(), attr.getDescription());
                    } catch (NoSuchAttributeException e) {
                    }
                }

                attrs.add(toAttribute(ae));
                toRemove.remove(ae.getKey());
            }

            // remove orphans
            toRemove.forEach(k -> {
                try {
                    attributeService.deleteAttribute(identifier, k);
                } catch (NoSuchAttributeSetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

        }

        DefaultAttributesSet a = toSet(se);
        a.addAttributes(attrs);

        return a;

    }

    public void deleteAttributeSet(String realm, String identifier) throws NoSuchAttributeSetException {
        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            throw new NoSuchAttributeSetException();
        }
        if (!realm.equals(se.getRealm())) {
            throw new IllegalArgumentException("set does not match realm");
        }

        attributeService.deleteAttributeSet(identifier);

    }

    /*
     * Attributes
     */
    public List<Attribute> listAttributes(String realm, String identifier) throws NoSuchAttributeSetException {
        AttributeSetEntity se = attributeService.getAttributeSet(identifier);
        if (!realm.equals(se.getRealm())) {
            throw new IllegalArgumentException("set does not match realm");
        }

        List<AttributeEntity> attributes = attributeService.listAttributes(identifier);

        return attributes.stream().map(a -> toAttribute(a)).collect(Collectors.toList());

    }

    /*
     * Builders
     */
    private DefaultAttributesSet toSet(AttributeSetEntity se) {
        DefaultAttributesSet s = new DefaultAttributesSet();
        s.setIdentifier(se.getIdentifier());
        s.setRealm(se.getRealm());
        s.setName(se.getName());
        s.setDescription(se.getDescription());
        return s;
    }

    private DefaultAttributesSet toSet(AttributeSetEntity se, List<AttributeEntity> attributes) {
        DefaultAttributesSet s = toSet(se);

        if (attributes != null) {
            attributes.forEach(ae -> s.addAttribute(toAttribute(ae)));
            // TODO add field to describe attribute is multiple
        }

        return s;
    }

    private Attribute toAttribute(AttributeEntity ae) {
        if (ae == null) {
            return null;
        }

        AttributeType type = AttributeType.parse(ae.getType());

        if (type == AttributeType.STRING) {
            return new StringAttribute(ae.getKey());
        } else if (type == AttributeType.BOOLEAN) {
            return new BooleanAttribute(ae.getKey());
        }
        if (type == AttributeType.DATE) {
            return new DateAttribute(ae.getKey());
        }
        if (type == AttributeType.NUMBER) {
            return new NumberAttribute(ae.getKey());
        }

        return new SerializableAttribute(ae.getKey());

    }

    private boolean isReserved(String identifier) {
        for (String prefix : RESERVED_PREFIXES) {
            if (identifier.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static final String[] RESERVED_PREFIXES = {
            "aac.", "idp.", "adp.", "openid", "default."
    };

}
