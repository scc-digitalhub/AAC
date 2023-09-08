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

package it.smartcommunitylab.aac.templates.persistence;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.repository.HashMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

@Entity
@Table(
    name = "templates",
    uniqueConstraints = @UniqueConstraint(columnNames = { "authority", "realm", "template", "language" })
)
public class TemplateEntity implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    // id is internal
    @Id
    @NotBlank
    @Column(name = "id", length = 128)
    private String id;

    @NotBlank
    @Column(name = "authority", length = 128)
    private String authority;

    @NotBlank
    @Column(name = "realm", length = 128)
    private String realm;

    // template name
    @NotBlank
    @Column(name = "template", length = 128)
    private String template;

    @NotBlank
    @Column(name = "language")
    private String language;

    @Lob
    @Column(name = "content")
    @Convert(converter = HashMapConverter.class)
    private Map<String, String> content;

    public TemplateEntity() {}

    public TemplateEntity(String id, String authority, String realm) {
        this.id = id;
        this.authority = authority;
        this.realm = realm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public void setContent(Map<String, String> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return (
            "TemplateEntity [id=" +
            id +
            ", authority=" +
            authority +
            ", realm=" +
            realm +
            ", template=" +
            template +
            ", language=" +
            language +
            ", content=" +
            content +
            "]"
        );
    }
}
