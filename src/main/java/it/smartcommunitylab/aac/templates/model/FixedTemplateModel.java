package it.smartcommunitylab.aac.templates.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

public class FixedTemplateModel extends TemplateModel {

    public final Set<String> keys;

    public FixedTemplateModel(String authority, String realm, String provider, String template,
            Collection<String> keys) {
        super(authority, realm, provider, template);
        this.keys = Collections.unmodifiableSortedSet(new TreeSet<>(keys));
    }

    @Override
    public Collection<String> keys() {
        return keys;
    }

    @Override
    public String get(String key) {
        Assert.hasText(key, "key can not be null");
        if (!keys.contains(key)) {
            return null;
        }

        return super.get(key);
    }

    @Override
    public void set(String key, String value) {
        Assert.hasText(key, "key can not be null");
        if (keys.contains(key)) {
            super.set(key, value);
        }
    }

    @Override
    public void setContent(Map<String, String> content) {
        if (content != null) {
            Map<String, String> map = content.entrySet().stream()
                    .filter(e -> keys.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            super.setContent(map);
        }
    }
}
