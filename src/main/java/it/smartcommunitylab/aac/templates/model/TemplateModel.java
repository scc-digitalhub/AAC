/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.templates.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.springframework.util.Assert;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateModel implements Template {

    @Size(max = 128)
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String id;

    @Size(max = 128)
    @NotBlank
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String authority;

    @Size(max = 128)
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String provider;

    @Size(max = 128)
    @NotBlank
    @Pattern(regexp = SystemKeys.URI_PATTERN)
    private String template;

    @Size(max = 128)
    @NotBlank
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String realm;

    @Size(max = 32)
    @NotBlank
    private String language;

    private Map<String, String> content;

    @JsonIgnore
    private Map<String, Object> modelAttributes;

    public TemplateModel() {}

    public TemplateModel(String authority, String realm, String provider, String template) {
        this.authority = authority;
        this.realm = realm;
        this.template = template;
        this.content = new HashMap<>();
        this.language = null;
        this.modelAttributes = null;
    }

    public void setId(String id) {
        this.id = id;
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
    public String getProvider() {
        return provider;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResourceId() {
        return template + "." + language;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @JsonIgnore
    public String buildKey() {
        // this key is unique
        StringBuilder sb = new StringBuilder();
        sb.append(authority).append(".");
        sb.append(realm).append(".");
        sb.append(template).append(".");
        sb.append(language);

        return sb.toString();
    }

    @Override
    @JsonGetter("keys")
    public Collection<String> keys() {
        return content != null ? content.keySet() : Collections.emptyList();
    }

    @Override
    public String get(String key) {
        Assert.hasText(key, "key can not be null");
        return content != null ? content.get(key) : null;
    }

    @JsonIgnore
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

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, Object> getModelAttributes() {
        return modelAttributes;
    }

    public void setModelAttributes(Map<String, Object> modelAttributes) {
        this.modelAttributes = modelAttributes;
    }

    public void setModelAttribute(String key, Object attribute) {
        Assert.hasText(key, "key can not be null");
        if (modelAttributes == null) {
            modelAttributes = new HashMap<>();
        }

        modelAttributes.put(key, attribute);
    }

    @Override
    public String toString() {
        return (
            "TemplateModel [id=" +
            id +
            ", authority=" +
            authority +
            ", provider=" +
            provider +
            ", template=" +
            template +
            ", realm=" +
            realm +
            ", language=" +
            language +
            ", content=" +
            content +
            "]"
        );
    }
}
