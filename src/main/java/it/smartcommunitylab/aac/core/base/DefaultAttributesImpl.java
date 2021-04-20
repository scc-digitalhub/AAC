package it.smartcommunitylab.aac.core.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.Attribute;
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

    private Set<String> keys;

    private Set<Attribute> attributes;

    private boolean isMutable;

    public DefaultAttributesImpl(String identifier) {
        Assert.hasText(identifier, "a valid identifier is required");
        this.identifier = identifier;
        this.keys = new HashSet<>();
        this.attributes = new HashSet<>();
        this.isMutable = true;
    }

    public DefaultAttributesImpl(String identifier, String[] keys) {
        Assert.hasText(identifier, "a valid identifier is required");
        this.identifier = identifier;
        this.keys = new HashSet<>();
        this.keys.addAll(Arrays.asList(keys));
        this.attributes = new HashSet<>();
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

        this.keys = new HashSet<>();
        this.keys.addAll(keys);
    }

    public Set<Attribute> getAttributes() {
        // we protect our representation from modifications
        return Collections.unmodifiableSet(attributes);
    }

    public void setAttributes(Collection<Attribute> attributes) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.attributes = new HashSet<>();
        this.attributes.addAll(attributes);
        this.keys = new HashSet<>();
        this.keys.addAll(attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet()));
    }

    public void addAttributes(Collection<Attribute> attributes) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.attributes.addAll(attributes);
        this.keys.addAll(attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet()));
    }

    public void addAttribute(Attribute attr) {
        if (!isMutable) {
            throw new IllegalArgumentException("this definition is not mutable");
        }

        this.attributes.add(attr);
        this.keys.add(attr.getKey());
    }

}