package it.smartcommunitylab.aac.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.config.AttributeSetsProperties;
import it.smartcommunitylab.aac.config.AttributeSetsProperties.AttributeSetDefinition;
import it.smartcommunitylab.aac.core.base.DefaultAttributesImpl;
import it.smartcommunitylab.aac.core.model.AttributeSet;

/*
 * Attribute manager 
 * 
 * deprecated with current design, should handle only ADDITIONAL attribute sets
 */

@Service
@Deprecated
public class AttributeManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // in memory attribute sets definitions
    private final Map<String, AttributeSet> attributeSets;

    public AttributeManager(AttributeSetsProperties sytemAttributeSets) {
        this.attributeSets = new HashMap<>();

        // unwrap system definitions and register
        // TODO evaluate separate repository for system definitions
        if (sytemAttributeSets != null) {
            for (AttributeSetDefinition definition : sytemAttributeSets.getSets()) {
                DefaultAttributesImpl set = new DefaultAttributesImpl(definition.getIdentifier(), definition.getKeys());
                this.attributeSets.put(set.getIdentifier(), set);
            }
        }

    }

    /*
     * Attribute sets
     */
    public AttributeSet getAttributeSet(String identifier) throws NoSuchAttributeSetException {
        logger.debug("get attribute set for id " + identifier);

        AttributeSet set = attributeSets.get(identifier);
        if (set == null) {
            throw new NoSuchAttributeSetException("no set found for identifier " + identifier);
        }

        return set;
    }

    public AttributeSet findAttributeSet(String identifier) {
        logger.debug("find attribute set for id " + identifier);

        return attributeSets.get(identifier);
    }

    public Collection<AttributeSet> listAttributeSets() {
        logger.debug("list attribute sets");

        return Collections.unmodifiableCollection(attributeSets.values());
    }

    /*
     * Manage sets
     */

    public void addAttributeSet(AttributeSet set) {
        // set with replace existing if mutable
        // evaluate split system from user definitions
        String identifier = set.getIdentifier();
        if (attributeSets.get(identifier) != null) {
            // TODO evaluate if system definition (not mutable)
        }

        logger.debug("add attribute sets");

        attributeSets.put(set.getIdentifier(), set);
    }

    public void removeAttributeSet(String identifier) {
        if (attributeSets.get(identifier) != null) {
            // TODO evaluate if system definition (not mutable)
        }

        attributeSets.remove(identifier);

    }

}
