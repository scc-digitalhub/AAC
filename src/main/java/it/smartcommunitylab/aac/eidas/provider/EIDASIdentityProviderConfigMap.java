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

/*
 * Configuration of an eIDAS provider.
 *
 * The italian eIDAS node is configured exactly like a SPID one, so the SPID map is inherited whole:
 *  - entityId, signingKey, signingCertificate: our own SP identity
 *  - organization*, contactPerson_*: metadata sections required by the node
 *  - idpMetadataUrl: the eIDAS node (mandatory, it is the single upstream idp)
 *  - authnContext, subAttributeName, usernameAttributeName: level of assurance and which attribute
 *    carries the local identifier (spidCode by default)
 *
 * `spidAttributes` is REPLACED by `eidasAttributes`, the catalogue of the eidas module (EIDASAttribute):
 * the same SPID names, restricted to the attributes an eIDAS node releases, with the four of the
 * Minimum Data Set as the required minimum. The SPID stack keeps reading getSpidAttributes(), which
 * returns the translation; the SPID field is out of json and schema. Effects: the console offers
 * exactly the attributes a node releases (the enum is the list), a configuration asking for a SPID
 * attribute outside the catalogue fails to deserialize, one missing the minimum fails validation -
 * both refused at registration, not accepted and broken at the first login. When no attribute is
 * configured the minimum is assumed.
 *
 * The `idps` field is dropped: SpidIdentityProviderConfig.setIdentityProviders() reads it only in
 * the branch that picks the upstream idps from the local SPID registry, and that branch is reached
 * only when `idpMetadataUrl` is empty. For eidas the node is always declared by url and no local
 * registry is passed, so whatever is configured there would be silently ignored.
 *
 * The `purpose` field (SPID Professionale, AgID notice n.18) is dropped as well: it qualifies
 * professional identities and legal person attributes, which no eIDAS node releases.
 *
 * Having a type of its own keeps the eidas provider form (and its persisted configuration) separate
 * from the SPID one, and gives eidas specific options a place to live without touching SPID.
 */
@Valid
/*
 * the SPID-only fields are excluded at class level: an @JsonIgnore on a single accessor is not
 * enough when the parent declares the property with an explicit annotation on another accessor
 * (Jackson then drops only the annotated accessor and keeps the property). The class level ignore
 * covers json, the persisted configuration and the generated schema alike.
 */
@JsonIgnoreProperties(value = { "spidAttributes", "idps", "purpose" }, ignoreUnknown = true)
public class EIDASIdentityProviderConfigMap extends SpidIdentityProviderConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_EIDAS_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_EIDAS;

    /*
     * the attributes requested to the node (SPID names, eidas catalogue); translated for the SPID stack
     */
    private Set<EIDASAttribute> eidasAttributes;

    /*
     * The certificate the node signs its metadata with, obtained out of band (FICEP). When set, the
     * metadata at idpMetadataUrl must verify against it; when empty, they are verified against the
     * certificate embedded in their own signature (integrity only, see EIDASMetadataSignatureVerifier).
     */
    private String idpMetadataSigningCertificate;

    /*
     * Diagnostic escape hatch, default true: the SPID rules require signed idp metadata, and a node
     * serving unsigned metadata is a finding to raise with the node operator, not a configuration
     * to accept silently. Set to false only to work against such a node while the finding is open.
     */
    private Boolean verifyIdpMetadataSignature;

    public Set<EIDASAttribute> getEidasAttributes() {
        return eidasAttributes;
    }

    public void setEidasAttributes(Set<EIDASAttribute> eidasAttributes) {
        this.eidasAttributes = eidasAttributes;
    }

    public String getIdpMetadataSigningCertificate() {
        return idpMetadataSigningCertificate;
    }

    public void setIdpMetadataSigningCertificate(String idpMetadataSigningCertificate) {
        this.idpMetadataSigningCertificate = idpMetadataSigningCertificate;
    }

    public Boolean getVerifyIdpMetadataSignature() {
        return verifyIdpMetadataSignature;
    }

    public void setVerifyIdpMetadataSignature(Boolean verifyIdpMetadataSignature) {
        this.verifyIdpMetadataSignature = verifyIdpMetadataSignature;
    }

    /* not a bean accessor on purpose: the Boolean getter above is the json property, this is the effective value */
    public boolean shouldVerifyIdpMetadataSignature() {
        return verifyIdpMetadataSignature == null || verifyIdpMetadataSignature.booleanValue();
    }

    /*
     * The SPID view of the requested attributes: what the SPID stack puts in the metadata and checks
     * in the assertion. Derived from eidasAttributes (the minimum when unset); the inherited field is
     * reachable only through the inherited setter, for the SPID code paths that use it, never from json.
     */
    @Override
    @JsonIgnore
    public Set<SpidAttribute> getSpidAttributes() {
        return EIDASAttribute.toSpidAttributes(getEidasAttributesOrMinimum());
    }

    private Set<EIDASAttribute> getEidasAttributesOrMinimum() {
        return eidasAttributes != null && !eidasAttributes.isEmpty() ? eidasAttributes : EIDASAttribute.mandatory();
    }

    @Override
    @JsonIgnore
    public void setSpidAttributes(Set<SpidAttribute> spidAttributes) {
        super.setSpidAttributes(spidAttributes);
    }

    /*
     * dropped from the eidas configuration: never read, see note above
     */
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

    /*
     * dropped from the eidas configuration: SPID Professionale only, see note above
     */
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

    /*
     * the parent copies its own fields; the eidas ones are copied here. Validation happens here too,
     * i.e. at registration: a configuration outside the eidas sets is refused.
     */
    public void setConfiguration(EIDASIdentityProviderConfigMap map) {
        super.setConfiguration(map);
        this.eidasAttributes = map.getEidasAttributes();
        this.idpMetadataSigningCertificate = map.getIdpMetadataSigningCertificate();
        this.verifyIdpMetadataSignature = map.getVerifyIdpMetadataSignature();

        assertValid();
    }

    /*
     * Registration-time checks, as a single call that throws with a message naming what is wrong.
     * Invoked by EIDASIdentityProviderConfig's constructor, so that - as SPID does with its signing
     * credentials - a provider on an invalid configuration cannot be built, hence cannot be activated;
     * saving a draft is allowed, and the error is reported when the provider is enabled.
     */
    public void assertValid() {
        requireMinimumAttributes(eidasAttributes);
        requireIdentifierAttribute("subAttributeName", getSubAttributeName());
        requireIdentifierAttribute("usernameAttributeName", getUsernameAttributeName());
        requireNotifiedLevel(getAuthnContext());
    }

    /*
     * The minimum a provider must request is the Minimum Data Set (the four mandatory attributes of
     * EIDASAttribute): a node releases them at every authentication and the SP metadata must ask for
     * them. Unset means the minimum; set and incomplete is refused, naming what is missing.
     */
    private static Set<EIDASAttribute> missingMinimumAttributes(Set<EIDASAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<EIDASAttribute> missing = EIDASAttribute.mandatory();
        missing.removeAll(attributes);
        return missing;
    }

    private static void requireMinimumAttributes(Set<EIDASAttribute> attributes) {
        Set<EIDASAttribute> missing = missingMinimumAttributes(attributes);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                "eidasAttributes must include the minimum data set, missing: " +
                missing.stream().map(EIDASAttribute::getValue).collect(Collectors.joining(", "))
            );
        }
    }

    /*
     * SPID is notified under eIDAS at all three levels of assurance (Cooperation Network Opinion No.
     * 1/2018 on the italian scheme: "high", "substantial" and "low"), so every SPID level has an eIDAS
     * level behind it: SpidL1 = low, SpidL2 = substantial, SpidL3 = high. All three are accepted; which
     * one to require is the Service Provider's choice (note that, among the notified schemes, only the
     * italian ones offer "low": towards foreign citizens SpidL1 and SpidL2 are served alike).
     * Verified on the italian node, pre-production (2026-10-05): SpidL3 requested by the Service Provider
     * leaves the node towards the eIDAS Connector as http://eidas.europa.eu/LoA/high, Comparison minimum.
     * Unset is left to the SPID default.
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
     * Which SPID attribute is the local identifier (subject) and which is the username.
     *
     * SPID leaves the choice open among spidCode, fiscalNumber, email, mobilePhone, username, ivaCode
     * and the SAML subject; for eidas only spidCode makes sense: it carries the unique and persistent
     * identifier (the eIDAS PersonIdentifier) a node releases for every citizen. The others are either
     * not eidas attributes at all (fiscalNumber, username, ivaCode), outside the catalogue (email,
     * mobilePhone) or transient (the SAML subject: the node issues a new NameID per session, as SPID
     * does, so nothing would ever match twice).
     *
     * When unset the identifier defaults to spidCode, instead of the SAML subject as in SPID.
     */
    public static final Set<SpidUserAttribute> IDENTIFIER_ATTRIBUTES = Collections.unmodifiableSet(
        EnumSet.of(SpidUserAttribute.SPID_CODE)
    );

    @Override
    public SpidUserAttribute getSubAttributeName() {
        SpidUserAttribute attribute = super.getSubAttributeName();
        return attribute != null ? attribute : SpidUserAttribute.SPID_CODE;
    }

    @Override
    public SpidUserAttribute getUsernameAttributeName() {
        SpidUserAttribute attribute = super.getUsernameAttributeName();
        return attribute != null ? attribute : SpidUserAttribute.SPID_CODE;
    }

    private static void requireIdentifierAttribute(String field, SpidUserAttribute attribute) {
        if (!IDENTIFIER_ATTRIBUTES.contains(attribute)) {
            throw new IllegalArgumentException(
                field +
                " must be one of " +
                IDENTIFIER_ATTRIBUTES.stream().map(SpidUserAttribute::getValue).collect(Collectors.joining(", ")) +
                " for an eidas provider, found " +
                attribute.getValue()
            );
        }
    }

    private static void narrowEnum(JsonSchema property, Set<String> values) {
        if (property != null && property.isValueTypeSchema()) {
            property.asValueTypeSchema().setEnums(values);
        }
    }

    /*
     * The generated schema, with the identifier attributes and the level narrowed to the eidas sets:
     * the console offers exactly what the validation above accepts.
     */
    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        JsonSchema schema = schemaGen.generateSchema(EIDASIdentityProviderConfigMap.class);

        Set<String> values = IDENTIFIER_ATTRIBUTES.stream()
            .map(SpidUserAttribute::getValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, JsonSchema> properties = schema.asObjectSchema().getProperties();
        for (String field : new String[] { "subAttributeName", "usernameAttributeName" }) {
            narrowEnum(properties.get(field), values);
        }

        // same for the level: the notified ones (all three, see NOTIFIED_LEVELS)
        narrowEnum(
            properties.get("authnContext"),
            NOTIFIED_LEVELS.stream().map(SpidAuthnContext::getValue).collect(Collectors.toCollection(LinkedHashSet::new))
        );

        return schema;
    }
}
