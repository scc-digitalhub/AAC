package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.claims.Claim;

public abstract class AbstractClaim implements Claim {

    protected String key;
    protected String namespace;

    protected String name;
    protected String description;

    
    
    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
