/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.eidas.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.eidas.model.EIDASAttribute;
import it.smartcommunitylab.aac.eidas.model.EIDASAttributeSet;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidPurpose;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Configuration of an eIDAS provider: the SPID map inherited whole (SP identity, organization, contact
 * person, idpMetadataUrl for the node, authnContext), with three eidas replacements:
 *  - attributeSet (minimum/index 99, full/index 100; Avviso eIDAS n. 01-2018, par. 4) in place of
 *    spidAttributes and attributeConsumingServiceIndex: attributes and index come from the set;
 *  - usernameAttribute, on the eidas catalogue, in place of usernameAttributeName; the subject is always
 *    spidCode (the eIDAS PersonIdentifier, the only unique and persistent attribute), not configurable;
 *  - idps and purpose dropped: SPID registry and SPID Professionale, nothing for an eIDAS node.
 * The SPID stack keeps reading the SPID getters, which answer from the eidas fields; the SPID fields
 * are out of json, persisted configuration and schema.
 */
@Valid
/*
 * SPID-only fields excluded at class level: @JsonIgnore on one accessor is not enough when the parent
 * annotates another accessor of the same property. Covers json, persisted configuration and schema.
 */
@JsonIgnoreProperties(
    value = { "spidAttributes", "attributeConsumingServiceIndex", "subAttributeName", "usernameAttributeName", "idps", "purpose" },
    ignoreUnknown = true
)
public class EIDASIdentityProviderConfigMap extends SpidIdentityProviderConfigMap {

    private static final Logger logger = LoggerFactory.getLogger(EIDASIdentityProviderConfigMap.class);

    private static final long serialVersionUID = SystemKeys.AAC_EIDAS_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_EIDAS;

    /*
     * The set requested to the node (EIDASAttributeSet), minimum by default, never null. Mind "full": its three
     * further attributes are optional in eIDAS and a scheme lacking them omits them (seen on the pre-production
     * node, 2026-10-28), while the SPID assertion validator rejects a Response missing a requested attribute.
     */
    private EIDASAttributeSet attributeSet = EIDASAttributeSet.MINIMUM;

    public EIDASAttributeSet getAttributeSet() {
        return attributeSet;
    }

    public void setAttributeSet(EIDASAttributeSet attributeSet) {
        this.attributeSet = attributeSet != null ? attributeSet : EIDASAttributeSet.MINIMUM;
    }

    /*
     * The username, among the attributes of the configured set; spidCode by default, and whenever the choice
     * falls outside the set (see assertValid). The subject is not configurable: always spidCode.
     */
    private EIDASAttribute usernameAttribute = EIDASAttribute.SPID_CODE;

    public EIDASAttribute getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(EIDASAttribute usernameAttribute) {
        this.usernameAttribute = usernameAttribute != null ? usernameAttribute : EIDASAttribute.SPID_CODE;
    }

    /* the SPID view of the requested attributes (metadata, assertion checks): those of the chosen set */
    @Override
    @JsonIgnore
    public Set<SpidAttribute> getSpidAttributes() {
        return toSpidAttributes(attributeSet.getAttributes());
    }

    /* the eidas catalogue translated for the SPID stack; kept with the overrides that feed it (and the config) */
    static Set<SpidAttribute> toSpidAttributes(Set<EIDASAttribute> attributes) {
        Set<SpidAttribute> spidAttributes = EnumSet.noneOf(SpidAttribute.class);
        if (attributes != null) {
            attributes.forEach(a -> spidAttributes.add(a.getAlias()));
        }
        return spidAttributes;
    }

    @Override
    @JsonIgnore
    public void setSpidAttributes(Set<SpidAttribute> spidAttributes) {
        super.setSpidAttributes(spidAttributes);
    }

    /* the AuthnRequest index is the one prescribed for the chosen set (99 or 100): derived, never configured */
    @Override
    @JsonIgnore
    public Integer getAttributeConsumingServiceIndex() {
        return attributeSet.getIndex();
    }

    @Override
    @JsonIgnore
    public void setAttributeConsumingServiceIndex(Integer attributeConsumingServiceIndex) {
        super.setAttributeConsumingServiceIndex(attributeConsumingServiceIndex);
    }

    /* dropped: read by SPID only when idpMetadataUrl is empty, never the case for eidas */
    @Override
    @JsonIgnore
    public Set<String> getIdps() {
        return super.getIdps();
    }

    @Override
    @JsonIgnore
    public void setIdps(Set<String> idps) {
        super.setIdps(idps);
    }

    /* dropped: SPID Professionale only (AgID notice n.18), nothing an eIDAS node releases */
    @Override
    @JsonIgnore
    public SpidPurpose getPurpose() {
        return super.getPurpose();
    }

    @Override
    @JsonIgnore
    public void setPurpose(SpidPurpose purpose) {
        super.setPurpose(purpose);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        EIDASIdentityProviderConfigMap map = mapper.convertValue(props, EIDASIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    /* the parent copies its fields, the eidas ones are copied here; validation runs here, i.e. at registration */
    public void setConfiguration(EIDASIdentityProviderConfigMap map) {
        super.setConfiguration(map);
        setAttributeSet(map.getAttributeSet());
        setUsernameAttribute(map.getUsernameAttribute());

        assertValid();
    }

    /*
     * Registration-time checks, invoked by EIDASIdentityProviderConfig's constructor too: a provider on an invalid
     * configuration cannot be built, hence cannot be enabled (a draft can still be saved).
     */
    public void assertValid() {
        this.usernameAttribute = inConfiguredSet("usernameAttribute", usernameAttribute);
        requireNotifiedLevel(getAuthnContext());
    }

    /* an identifier outside the configured set can never be in a Response: spidCode instead, with a warning */
    private EIDASAttribute inConfiguredSet(String field, EIDASAttribute attribute) {
        if (attribute == null || attributeSet.getAttributes().contains(attribute)) {
            return attribute != null ? attribute : EIDASAttribute.SPID_CODE;
        }
        logger.warn(
            "eidas {} {} is not in the {} attribute set, falling back to {}",
            field,
            attribute.getValue(),
            attributeSet.getValue(),
            EIDASAttribute.SPID_CODE.getValue()
        );
        return EIDASAttribute.SPID_CODE;
    }

    /*
     * SPID is notified under eIDAS at all three levels (Cooperation Network Opinion No. 1/2018: SpidL1 = low,
     * SpidL2 = substantial, SpidL3 = high), so all three are accepted; verified on the pre-production node,
     * 2026-10-05: SpidL3 leaves the node as http://eidas.europa.eu/LoA/high. Unset is left to the SPID default.
     */
    public static final Set<SpidAuthnContext> NOTIFIED_LEVELS = Collections.unmodifiableSet(
        EnumSet.of(SpidAuthnContext.SPID_L1, SpidAuthnContext.SPID_L2, SpidAuthnContext.SPID_L3)
    );

    private static void requireNotifiedLevel(SpidAuthnContext level) {
        if (level != null && !NOTIFIED_LEVELS.contains(level)) {
            throw new IllegalArgumentException(
                "authnContext " +
                level.getValue() +
                " has no eIDAS level of assurance: an eidas provider requires " +
                NOTIFIED_LEVELS.stream().map(SpidAuthnContext::getValue).collect(Collectors.joining(" or "))
            );
        }
    }

    /*
     * The SPID identifiers as the SPID stack reads them: always spidCode, the only SPID identifier that is an eidas
     * attribute. The username choice is applied afterwards on the principal by EIDASAuthenticationProvider.
     */
    @Override
    @JsonIgnore
    public SpidUserAttribute getSubAttributeName() {
        return SpidUserAttribute.SPID_CODE;
    }

    @Override
    @JsonIgnore
    public SpidUserAttribute getUsernameAttributeName() {
        return SpidUserAttribute.SPID_CODE;
    }

    @Override
    @JsonIgnore
    public void setSubAttributeName(SpidUserAttribute subAttributeName) {
        super.setSubAttributeName(subAttributeName);
    }

    @Override
    @JsonIgnore
    public void setUsernameAttributeName(SpidUserAttribute usernameAttributeName) {
        super.setUsernameAttributeName(usernameAttributeName);
    }

    private static void narrowEnum(JsonSchema property, Set<String> values) {
        if (property != null && property.isValueTypeSchema()) {
            property.asValueTypeSchema().setEnums(values);
        }
    }

    /* the generated schema, narrowed to what the validation accepts: identifiers, level, attribute set */
    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        JsonSchema schema = schemaGen.generateSchema(EIDASIdentityProviderConfigMap.class);

        Map<String, JsonSchema> properties = schema.asObjectSchema().getProperties();

        // the username, by the SPID names of the catalogue (the enum constant names otherwise)
        Set<String> values = EnumSet.allOf(EIDASAttribute.class)
            .stream()
            .map(EIDASAttribute::getValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        JsonSchema usernameProperty = properties.get("usernameAttribute");
        narrowEnum(usernameProperty, values);
        if (usernameProperty != null && usernameProperty.isSimpleTypeSchema()) {
            usernameProperty.asSimpleTypeSchema().setDefault(EIDASAttribute.SPID_CODE.getValue());
        }

        // same for the level: the notified ones (all three, see NOTIFIED_LEVELS)
        narrowEnum(
            properties.get("authnContext"),
            NOTIFIED_LEVELS.stream().map(SpidAuthnContext::getValue).collect(Collectors.toCollection(LinkedHashSet::new))
        );

        // the attribute set by value, minimum as default; never "required": jackson emits it draft-3 style
        // ("required": true on the property) and the console validator (ajv, draft-7) rejects the schema
        JsonSchema attributeSetProperty = properties.get("attributeSet");
        narrowEnum(
            attributeSetProperty,
            EnumSet.allOf(EIDASAttributeSet.class)
                .stream()
                .map(EIDASAttributeSet::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new))
        );
        if (attributeSetProperty != null && attributeSetProperty.isSimpleTypeSchema()) {
            attributeSetProperty.asSimpleTypeSchema().setDefault(EIDASAttributeSet.MINIMUM.getValue());
        }

        return schema;
    }
}
