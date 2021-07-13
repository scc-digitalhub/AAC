package it.smartcommunitylab.aac.api;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiAttributesScope;
import it.smartcommunitylab.aac.attributes.AttributeManager;
import it.smartcommunitylab.aac.attributes.DefaultAttributesSet;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.model.AttributeSet;

@RestController
@RequestMapping("api")
public class AttributesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AttributeManager attributeManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Attribute sets
     * 
     * When provided with a fully populated service model, related entities will be
     * updated accordingly.
     */

    @GetMapping("/attributes/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiAttributesScope.SCOPE + "')")
    public Collection<AttributeSet> listAttributeSets(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        logger.debug("list attribute sets for realm " + String.valueOf(realm));
        return attributeManager.listAttributeSets(realm);
    }

    @GetMapping("/attributes/{realm}/{setId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiAttributesScope.SCOPE + "')")
    public AttributeSet getAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId)
            throws NoSuchAttributeSetException, NoSuchRealmException {
        logger.debug("get attribute set " + String.valueOf(setId) + " for realm " + String.valueOf(realm));
        return attributeManager.getAttributeSet(realm, setId);
    }

    @PostMapping("/attributes/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiAttributesScope.SCOPE + "')")
    public AttributeSet addAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid DefaultAttributesSet s) throws NoSuchRealmException {
        logger.debug("add attribute set for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("attribute set bean " + String.valueOf(s));
        }
        return attributeManager.addAttributeSet(realm, s);
    }

    @PutMapping("/attributes/{realm}/{setId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiAttributesScope.SCOPE + "')")
    public AttributeSet updateAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId,
            @RequestBody @Valid DefaultAttributesSet s) throws NoSuchAttributeSetException, NoSuchRealmException {
        logger.debug("update attribute set " + String.valueOf(setId) + " for realm " + String.valueOf(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("attribute set bean " + String.valueOf(s));
        }
        return attributeManager.updateAttributeSet(realm, setId, s);
    }

    @DeleteMapping("/attributes/{realm}/{setId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiAttributesScope.SCOPE + "')")
    public void deleteAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId)
            throws NoSuchAttributeSetException {
        logger.debug("delete attribute set " + String.valueOf(setId) + " for realm " + String.valueOf(realm));
        attributeManager.deleteAttributeSet(realm, setId);
    }

    @PutMapping("/attributes/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiAttributesScope.SCOPE + "')")
    public AttributeSet importAttributeSet(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        logger.debug("import attribute set to realm " + String.valueOf(realm));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString()) &&
                        !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            DefaultAttributesSet s = yamlObjectMapper.readValue(file.getInputStream(),
                    DefaultAttributesSet.class);

            if (logger.isTraceEnabled()) {
                logger.trace("attribute set bean: " + String.valueOf(s));
            }

            return attributeManager.addAttributeSet(realm, s);

        } catch (Exception e) {
            logger.error("import attribute set error: " + e.getMessage());
            throw e;
        }

    }

}
