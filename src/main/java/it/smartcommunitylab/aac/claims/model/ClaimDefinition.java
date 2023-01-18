package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.model.AttributeType;

/*
 * A claim describes an assertion about a subject.
 * The definition defines the characteristics of the claim.
 */

public interface ClaimDefinition {

    // the claim key should be unique under the given namespace
    // when multiple are provided, the result will be a collection
    public String getKey();

    public AttributeType getAttributeType();

    // a human readable name for this claim
    // TODO evaluate removal
//    public String getName();
//
//    public String getDescription();

    // a flag signaling if this claim is single or multiple use
    public boolean isMultiple();

}
