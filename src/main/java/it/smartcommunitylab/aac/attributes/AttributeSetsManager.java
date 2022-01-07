package it.smartcommunitylab.aac.attributes;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.model.AttributeSet;

/*
 * Attribute manager 
 * 
 * handles attribute sets definition
 * TODO handle attribute sets per *realm*
 * 
 * TODO handle attribute mapping 
 */

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class AttributeSetsManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AttributeService attributeService;

    /*
     * Attribute sets
     */
    public AttributeSet getAttributeSet(String realm, String identifier) throws NoSuchAttributeSetException {
        // TODO add realm match check
        return attributeService.getAttributeSet(identifier);
    }

    public AttributeSet findAttributeSet(String realm, String identifier) {
        // TODO add realm match check
        return attributeService.findAttributeSet(identifier);
    }

    public Collection<AttributeSet> listAttributeSets(String realm) {
        return listAttributeSets(realm, false);
    }

    public Collection<AttributeSet> listAttributeSets(String realm, boolean includeSystem) {
        logger.debug("list attribute sets");

        // TODO add static (internal) attribute sets to list
        Stream<AttributeSet> sets = attributeService.listAttributeSets(realm).stream();
        if (includeSystem) {
            return Stream.concat(attributeService.listSystemAttributeSets().stream(), sets)
                    .collect(Collectors.toList());
        } else {
            return sets.collect(Collectors.toList());
        }
    }

    public AttributeSet addAttributeSet(String realm, DefaultAttributesSet set) {

        String identifier = set.getIdentifier();
        if (!StringUtils.hasText(identifier) || isReserved(identifier)) {
            throw new IllegalArgumentException("invalid set identifier");
        }

        logger.debug("add attribute set " + identifier);
        // TODO move back here registration of attributes for the set
        AttributeSet se = attributeService.addAttributeSet(realm, set);
        return se;

    }

    public AttributeSet updateAttributeSet(String realm, String identifier, DefaultAttributesSet set)
            throws NoSuchAttributeSetException {

        if (!StringUtils.hasText(identifier) || isReserved(identifier)) {
            throw new IllegalArgumentException("invalid set identifier");
        }

        logger.debug("update attribute set " + StringUtils.trimAllWhitespace(identifier));

        // TODO move back here registration of attributes for the set
        AttributeSet se = attributeService.updateAttributeSet(identifier, set);
        return se;

    }

    public void deleteAttributeSet(String realm, String identifier) throws NoSuchAttributeSetException {
        if (!StringUtils.hasText(identifier) || isReserved(identifier)) {
            throw new IllegalArgumentException("invalid set identifier");
        }

        attributeService.deleteAttributeSet(identifier);

    }

    /*
     * Attributes
     */
//    public Collection<Attribute> listAttributes(String realm, String identifier) throws NoSuchAttributeSetException {
//
//        AttributeSet se = attributeService.getAttributeSet(identifier);
//        if (se.getAttributes() != null && !se.getAttributes().isEmpty()) {
//            return se.getAttributes();
//        }
//
//        return attributeService.listAttributes(identifier);
//    }

    /*
     * Helpers
     */

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
