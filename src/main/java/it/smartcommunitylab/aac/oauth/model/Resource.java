package it.smartcommunitylab.aac.oauth.model;

import org.springframework.util.Assert;

public class Resource {

    private String identifier;

    private String namespace;

    public Resource(String identifier) {
        Assert.hasText(identifier, "identifier can not be null or empty");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}
