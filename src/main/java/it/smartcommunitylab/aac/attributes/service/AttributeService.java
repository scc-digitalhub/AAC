package it.smartcommunitylab.aac.attributes.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.attributes.DefaultAttributesSet;
import it.smartcommunitylab.aac.attributes.model.AbstractAttribute;
import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.DateTimeAttribute;
import it.smartcommunitylab.aac.attributes.model.NumberAttribute;
import it.smartcommunitylab.aac.attributes.model.SerializableAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.attributes.model.TimeAttribute;
import it.smartcommunitylab.aac.attributes.persistence.AttributeEntity;
import it.smartcommunitylab.aac.attributes.persistence.AttributeSetEntity;
import it.smartcommunitylab.aac.common.NoSuchAttributeException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.model.AttributeType;

@Service
@Transactional
public class AttributeService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AttributeEntityService attributeService;

    private Map<String, AttributeSet> systemAttributeSets = Collections.emptyMap();

    @Autowired
    public void setAttributeSets(List<AttributeSet> sets) {
        systemAttributeSets = sets.stream().collect(Collectors.toMap(s -> s.getIdentifier(), s -> s));
    }

    /*
     * Attribute sets
     */

    public AttributeSet getAttributeSet(String identifier) throws NoSuchAttributeSetException {
        logger.debug("get attribute set for id " + StringUtils.trimAllWhitespace(identifier));

        if (systemAttributeSets.containsKey(identifier)) {
            return systemAttributeSets.get(identifier);
        }

        AttributeSetEntity se = attributeService.getAttributeSet(identifier);
        List<AttributeEntity> attributes = attributeService.listAttributes(identifier);

        return toSet(se, attributes);

    }

    public AttributeSet findAttributeSet(String identifier) {
        logger.debug("find attribute set for id " + identifier);
        if (systemAttributeSets.containsKey(identifier)) {
            return systemAttributeSets.get(identifier);
        }

        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            return null;
        }

        List<AttributeEntity> attributes = Collections.emptyList();
        try {
            attributes = attributeService.listAttributes(identifier);
        } catch (NoSuchAttributeSetException e) {
        }

        return toSet(se, attributes);
    }

    public Collection<AttributeSet> listSystemAttributeSets() {
        logger.debug("list system sets");
        return systemAttributeSets.values();
    }

    public Collection<AttributeSet> listAttributeSets() {
        logger.debug("list attribute sets");

        // TODO add static (internal) attribute sets to list
        return attributeService.listAttributeSets().stream().map(s -> {
            DefaultAttributesSet a = toSet(s);
            try {
                a.addAttributes(listAttributes(s.getIdentifier()));
            } catch (NoSuchAttributeSetException e) {
            }
            return a;
        }).collect(Collectors.toList());
    }

    public Collection<AttributeSet> listAttributeSets(String realm) {
        logger.debug("list attribute sets for realm " + StringUtils.trimAllWhitespace(realm));

        // TODO add static (internal) attribute sets to list
        return attributeService.listAttributeSets(realm).stream().map(s -> {
            DefaultAttributesSet a = toSet(s);
            try {
                a.addAttributes(listAttributes(s.getIdentifier()));
            } catch (NoSuchAttributeSetException e) {
            }
            return a;
        }).collect(Collectors.toList());
    }

    public AttributeSet addAttributeSet(String realm, DefaultAttributesSet set) {

        String identifier = set.getIdentifier();
        if (!StringUtils.hasText(identifier)) {
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
                            attr.getType(), attr.getIsMultiple(),
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

    public AttributeSet updateAttributeSet(String identifier, DefaultAttributesSet set)
            throws NoSuchAttributeSetException {

        logger.debug("update attribute set " + StringUtils.trimAllWhitespace(identifier));

        if (systemAttributeSets.containsKey(identifier)) {
            throw new IllegalArgumentException("system attribute sets are unmodifiable");
        }

        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            throw new NoSuchAttributeSetException();
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
                            attr.getType(), attr.getIsMultiple(),
                            attr.getName(), attr.getDescription());
                } else {
                    try {
                        ae = attributeService.updateAttribute(identifier, attr.getKey(),
                                attr.getType(), attr.getIsMultiple(),
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

    public void deleteAttributeSet(String identifier) throws NoSuchAttributeSetException {
        if (systemAttributeSets.containsKey(identifier)) {
            throw new IllegalArgumentException("system attribute sets are unmodifiable");
        }

        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            throw new NoSuchAttributeSetException();
        }

        attributeService.deleteAttributeSet(identifier);

    }

    /*
     * Attributes
     */
    public Collection<Attribute> listAttributes(String identifier) throws NoSuchAttributeSetException {
        if (systemAttributeSets.containsKey(identifier)) {
            return systemAttributeSets.get(identifier).getAttributes();
        }
        AttributeSetEntity se = attributeService.getAttributeSet(identifier);
        List<AttributeEntity> attributes = attributeService.listAttributes(se.getIdentifier());
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
        AbstractAttribute attr = null;

        if (type == AttributeType.STRING) {
            attr = new StringAttribute(ae.getKey());
        } else if (type == AttributeType.NUMBER) {
            attr = new NumberAttribute(ae.getKey());
        } else if (type == AttributeType.BOOLEAN) {
            attr = new BooleanAttribute(ae.getKey());
        } else if (type == AttributeType.DATE) {
            attr = new DateAttribute(ae.getKey());
        } else if (type == AttributeType.DATETIME) {
            attr = new DateTimeAttribute(ae.getKey());
        } else if (type == AttributeType.TIME) {
            attr = new TimeAttribute(ae.getKey());
        } else {
            attr = new SerializableAttribute(ae.getKey());
        }

        attr.setName(ae.getName());
        attr.setDescription(ae.getDescription());
        attr.setIsMultiple(ae.isMultiple());

        return attr;
    }
}
