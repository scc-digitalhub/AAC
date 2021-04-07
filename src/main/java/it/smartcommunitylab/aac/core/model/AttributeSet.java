package it.smartcommunitylab.aac.core.model;

import java.util.Collection;

public interface AttributeSet {

    public String getIdentifier();

    public Collection<String> getKeys();

    public Collection<Attribute> getAttributes();

}
