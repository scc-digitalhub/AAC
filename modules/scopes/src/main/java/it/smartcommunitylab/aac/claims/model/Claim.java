package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;
import org.springframework.lang.Nullable;

/*
 * A claim describes an assertion about a subject
 */

public interface Claim {
    // unique identifier
    public String getClaimId();

    // the claim key should be unique under the given namespace
    // when multiple are provided, the result will be a collection
    public String getKey();

    public AttributeType getAttributeType();

    //    // a human readable name for this claim
    //    // TODO evaluate removal
    //    public String getName();
    //
    //    public String getDescription();
    //
    //    // a flag signaling if this claim is single or multiple use
    //    public boolean isMultiple();

    // the value in a serializable format, ready for store
    public @Nullable Serializable getValue();

    // the value in an export-ready format, compatible with parsers
    public @Nullable Serializable exportValue();
}
