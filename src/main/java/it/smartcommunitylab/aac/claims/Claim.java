package it.smartcommunitylab.aac.claims;

import java.io.Serializable;

import it.smartcommunitylab.aac.model.AttributeType;

/*
 * A claim describes an assertion about a principal made by an actor.
 * 
 * The assertion needs to have a valid type and a non empty value to be used.
 */

public interface Claim {

    // the claim key should be unique under the given namespace
    // when multiple are provided, the result will be a collection
    public String getKey();

    public AttributeType getType();

//    // a claim can be namespaced. When empty claims will be merged top level
//    // TODO remove, claimsSet are namespaced, single claim need to belong to a
//    // claimSet to be exposed
//    public String getNamespace();

    // a humane readable name for this claim
    // TODO evaluate removal
    public String getName();

    public String getDescription();

//    // a flag signaling if this claim is single or multiple use
//    // TODO evaluate removal from here, should be left to definitions
//    public boolean isMultiple();

    // the value in a serializable format, ready for store or translation
    public Serializable getValue();

}
