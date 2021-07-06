package it.smartcommunitylab.aac.core;

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
import it.smartcommunitylab.aac.common.NoSuchAttributeException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.base.DefaultAttributesImpl;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.core.persistence.AttributeSetEntity;
import it.smartcommunitylab.aac.core.service.AttributeService;
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
    public AttributeSet getAttributeSet(String identifier) throws NoSuchAttributeSetException {
        logger.debug("get attribute set for id " + identifier);

        AttributeSetEntity se = attributeService.getAttributeSet(identifier);
        List<AttributeEntity> attributes = attributeService.listAttributes(identifier);

        return toSet(se, attributes);

    }

    public AttributeSet findAttributeSet(String identifier) {
        logger.debug("find attribute set for id " + identifier);

        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            return null;
        }

        Set<String> keys = Collections.emptySet();
        try {
            List<AttributeEntity> attributes = attributeService.listAttributes(identifier);
            keys = attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet());
        } catch (NoSuchAttributeSetException e) {
        }
        DefaultAttributesImpl a = toSet(se);
        a.setKeys(keys);

        return a;
    }

    public Collection<AttributeSet> listAttributeSets() {
        logger.debug("list attribute sets");

        // TODO add static (internal) attribute sets to list
        return attributeService.listAttributeSets().stream().map(s -> {
            DefaultAttributesImpl a = toSet(s);
            try {
                a.setAttributes(listAttributes(s.getSet()));
            } catch (NoSuchAttributeSetException e) {
            }
            return a;
        })
                .collect(Collectors.toList());

    }

    public AttributeSet addAttributeSet(AttributeSet set) {

        String identifier = set.getIdentifier();
        if (!StringUtils.hasText(identifier) || isReserved(identifier)) {
            throw new IllegalArgumentException("invalid set identifier");
        }

        logger.debug("add attribute set " + identifier);
        AttributeSetEntity se = attributeService.addAttributeSet(identifier, set.getName(), set.getDescription());
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

        DefaultAttributesImpl a = toSet(se);
        a.setAttributes(attrs);

        return a;

    }

    public AttributeSet updateAttributeSet(String identifier, AttributeSet set) throws NoSuchAttributeSetException {

        logger.debug("update attribute set " + identifier);

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

        DefaultAttributesImpl a = toSet(se);
        a.setAttributes(attrs);

        return a;

    }

    public void deleteAttributeSet(String identifier) throws NoSuchAttributeSetException {
        AttributeSetEntity se = attributeService.findAttributeSet(identifier);
        if (se == null) {
            throw new NoSuchAttributeSetException();
        }

        attributeService.deleteAttributeSet(identifier);

    }

    /*
     * Attributes
     */
    public List<Attribute> listAttributes(String identifier) throws NoSuchAttributeSetException {
        AttributeSetEntity se = attributeService.getAttributeSet(identifier);
        List<AttributeEntity> attributes = attributeService.listAttributes(identifier);

        return attributes.stream().map(a -> toAttribute(a)).collect(Collectors.toList());

    }

    /*
     * Builders
     */
    private DefaultAttributesImpl toSet(AttributeSetEntity se) {
        DefaultAttributesImpl s = new DefaultAttributesImpl(se.getSet());
        s.setName(se.getName());
        s.setDescription(se.getDescription());
        return s;
    }

    private DefaultAttributesImpl toSet(AttributeSetEntity se, List<AttributeEntity> attributes) {
        DefaultAttributesImpl s = toSet(se);

        if (attributes != null) {
            attributes.forEach(ae -> s.addAttribute(toAttribute(ae)));
            // TODO add field to describle attribute is multiple
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
