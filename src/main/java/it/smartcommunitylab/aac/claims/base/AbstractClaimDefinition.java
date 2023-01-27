package it.smartcommunitylab.aac.claims.base;

import it.smartcommunitylab.aac.claims.model.ClaimDefinition;

public abstract class AbstractClaimDefinition implements ClaimDefinition, Comparable<AbstractClaimDefinition> {

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractClaimDefinition other = (AbstractClaimDefinition) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

    @Override
    public int compareTo(AbstractClaimDefinition o) {
        if (this.key == null)
            return 0;
        if (o == null)
            return -1;
        return this.key.compareTo(o.key);
    }

}
