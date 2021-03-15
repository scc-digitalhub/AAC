package it.smartcommunitylab.aac.core.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.AttributeSet;

/*
 * Default Attributes describes an attributeSet as a model definition, detached from user resources.
 * Keys are required and should present the whole list of attributes available from the set.
 * The collection of attributes can contain descriptive or sample values.
 * 
 * Providers are not required to fulfill all keys, consumers should be able to handle null and react accordingly
 * 
 * Models do not persist a relation with a provider, but provider-specific collections should be namespaced.
 */
public class DefaultAttributesImpl implements AttributeSet {

    private final String identifier;

    private Collection<String> keys;

    private Map<String, String> attributes;

    private boolean isMutable;

    public DefaultAttributesImpl(String identifier) {
        Assert.hasText(identifier, "a valid identifier is required");
        this.identifier = identifier;
        this.keys = new HashSet<>();
        this.attributes = Collections.emptyMap();
        this.isMutable = true;
    }

    public DefaultAttributesImpl(String identifier, String[] keys) {
        Assert.hasText(identifier, "a valid identifier is required");
        this.identifier = identifier;
        this.keys = new HashSet<>();
        this.keys.addAll(Arrays.asList(keys));
        this.attributes = Collections.emptyMap();
        this.isMutable = true;

    }

    public String getIdentifier() {
        return identifier;
    }

    public Collection<String> getKeys() {
        // we protect our representation from modifications
        return Collections.unmodifiableCollection(keys);
    }

    public void setKeys(Collection<String> keys) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.keys = keys;

    }

    public Map<String, String> getAttributes() {
        // we protect our representation from modifications
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, String> attributes) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.attributes = attributes;
    }

}
