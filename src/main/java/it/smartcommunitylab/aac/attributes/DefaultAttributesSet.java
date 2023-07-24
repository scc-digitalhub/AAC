package it.smartcommunitylab.aac.attributes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.DefaultAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultAttributesSet implements AttributeSet {

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String identifier;

    private String realm;

    private String name;
    private String description;

    private Collection<DefaultAttribute> attributes = Collections.emptyList();

    public DefaultAttributesSet() {
        this.attributes = new ArrayList<>();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Collection<String> getKeys() {
        return attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet());
    }

    @Override
    public Collection<Attribute> getAttributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    public void setAttributes(Collection<DefaultAttribute> attributes) {
        this.attributes = attributes;
    }

    public void addAttributes(Collection<Attribute> attributes) {
        this.attributes = new HashSet<>();
        if (attributes != null) {
            this.attributes.addAll(
                    attributes
                        .stream()
                        .map(a -> {
                            DefaultAttribute attr = new DefaultAttribute();
                            attr.setKey(a.getKey());
                            attr.setType(a.getType());
                            attr.setName(a.getName());
                            attr.setDescription(a.getDescription());
                            attr.setIsMultiple(a.getIsMultiple());
                            return attr;
                        })
                        .collect(Collectors.toSet())
                );
        }
    }

    public void addAttribute(Attribute attribute) {
        if (attribute != null) {
            // translate to std attr dropping value
            DefaultAttribute attr = new DefaultAttribute();
            attr.setKey(attribute.getKey());
            attr.setType(attribute.getType());
            attr.setName(attribute.getName());
            attr.setDescription(attribute.getDescription());
            attr.setIsMultiple(attribute.getIsMultiple());
            attributes.add(attr);
        }
    }

    public static DefaultAttributesSet from(AttributeSet set) {
        DefaultAttributesSet aset = new DefaultAttributesSet();
        aset.identifier = set.getIdentifier();
        aset.realm = null;
        aset.name = set.getName();
        aset.description = set.getDescription();
        if (set.getAttributes() != null) {
            aset.attributes =
                set
                    .getAttributes()
                    .stream()
                    .map(a -> {
                        DefaultAttribute attr = new DefaultAttribute();
                        attr.setKey(a.getKey());
                        attr.setType(a.getType());
                        attr.setName(a.getName());
                        attr.setDescription(a.getDescription());
                        attr.setIsMultiple(a.getIsMultiple());
                        return attr;
                    })
                    .collect(Collectors.toSet());
        }

        return aset;
    }

    @Override
    public String toString() {
        return (
            "DefaultAttributesSet [identifier=" +
            identifier +
            ", realm=" +
            realm +
            ", name=" +
            name +
            ", description=" +
            description +
            ", attributes=" +
            attributes +
            "]"
        );
    }
}
