package it.smartcommunitylab.aac.claims.base;

import it.smartcommunitylab.aac.claims.model.ClaimDefinition;

public abstract class AbstractClaimDefinition implements ClaimDefinition {

    protected final String key;
    protected Boolean isMultiple;

    protected AbstractClaimDefinition(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean isMultiple() {
        return isMultiple != null ? isMultiple.booleanValue() : false;
    }

    public Boolean getIsMultiple() {
        return isMultiple;
    }

    public void setIsMultiple(Boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

}
