package it.smartcommunitylab.aac.core.model;

import java.util.Collection;
import java.util.Map;

public interface AttributeSet {

    public String getIdentifier();

    public Collection<String> getKeys();

    public Map<String, String> getAttributes();

}
