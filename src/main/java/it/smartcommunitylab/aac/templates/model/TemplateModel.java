package it.smartcommunitylab.aac.templates.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.Template;

public class TemplateModel implements Template {

    private final String authority;
    private final String template;

    private String realm;
    private String language;
    private Map<String, String> content;

    public TemplateModel(String authority, String realm, String template) {
        this.authority = authority;
        this.realm = realm;
        this.template = template;
        this.content = new HashMap<>();
        this.language = null;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getId() {
        return template;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public Collection<String> keys() {
        return content != null ? content.keySet() : Collections.emptyList();
    }

    @Override
    public String get(String key) {
        Assert.hasText(key, "key can not be null");
        return content != null ? content.get(key) : null;
    }

    public void set(String key, String value) {
        Assert.hasText(key, "key can not be null");
        if (content == null) {
            content = new HashMap<>();
        }

        content.put(key, value);
    }

    public Map<String, String> getContent() {
        return content;
    }

    public void setContent(Map<String, String> content) {
        this.content = content;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

}
