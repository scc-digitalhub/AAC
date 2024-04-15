/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.attributes;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * Base controller for attributeSets
 */
@PreAuthorize("hasAuthority(this.authority)")
public abstract class BaseAttributeSetsController implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected AttributeSetsManager attributeManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(attributeManager, "attribute manager is required");
    }

    @Autowired
    public void setAttributeManager(AttributeSetsManager attributeManager) {
        this.attributeManager = attributeManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Attribute sets
     *
     * When provided with a fully populated service model, related entities will be
     * updated accordingly.
     */

    @GetMapping("/attributeset/{realm}")
    @Operation(summary = "list attribute sets from a given realm")
    public Collection<AttributeSet> listAttributeSets(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(name = "system", required = false, defaultValue = "false") boolean includeSystem
    ) throws NoSuchRealmException {
        logger.debug("list attribute sets for realm {}", StringUtils.trimAllWhitespace(realm));

        return attributeManager.listAttributeSets(realm, includeSystem);
    }

    @GetMapping("/attributeset/{realm}/{setId}")
    @Operation(summary = "get a specific attribute set from a given realm")
    public AttributeSet getAttributeSet(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId
    ) throws NoSuchAttributeSetException, NoSuchRealmException {
        logger.debug(
            "get attribute set {} for realm {}",
            StringUtils.trimAllWhitespace(setId),
            StringUtils.trimAllWhitespace(realm)
        );

        return attributeManager.getAttributeSet(realm, setId);
    }

    @PostMapping("/attributeset/{realm}")
    @Operation(summary = "add a new attribute set to a given realm")
    public AttributeSet addAttributeSet(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull DefaultAttributesSet s
    ) throws NoSuchRealmException {
        logger.debug("add attribute set for realm {}", StringUtils.trimAllWhitespace(realm));

        if (logger.isTraceEnabled()) {
            logger.trace("attribute set bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return attributeManager.addAttributeSet(realm, s);
    }

    @PutMapping("/attributeset/{realm}/{setId}")
    @Operation(summary = "update a specific attribute set in a given realm")
    public AttributeSet updateAttributeSet(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId,
        @RequestBody @Valid @NotNull DefaultAttributesSet s
    ) throws NoSuchAttributeSetException, NoSuchRealmException {
        logger.debug(
            "update attribute set {} for realm {}",
            StringUtils.trimAllWhitespace(setId),
            StringUtils.trimAllWhitespace(realm)
        );

        if (logger.isTraceEnabled() && s != null) {
            logger.trace("attribute set bean {}", StringUtils.trimAllWhitespace(s.toString()));
        }
        return attributeManager.updateAttributeSet(realm, setId, s);
    }

    @DeleteMapping("/attributeset/{realm}/{setId}")
    @Operation(summary = "delete a specific attribute set from a given realm")
    public void deleteAttributeSet(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId
    ) throws NoSuchAttributeSetException {
        logger.debug(
            "delete attribute set {} for realm {}",
            StringUtils.trimAllWhitespace(setId),
            StringUtils.trimAllWhitespace(realm)
        );

        attributeManager.deleteAttributeSet(realm, setId);
    }
}
