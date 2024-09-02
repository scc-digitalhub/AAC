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

package it.smartcommunitylab.aac.realms.persistence;

import it.smartcommunitylab.aac.repository.HashMapConverter;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "realms")
@EntityListeners(AuditingEntityListener.class)
public class RealmEntity {

    @Id
    @NotNull
    @Column(length = 128, unique = true)
    private String slug;

    @NotNull
    private String name;

    @Column(name = "email_address")
    private String email;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    // configuration
    @Column(name = "is_editable")
    private boolean editable;

    @Column(name = "is_public")
    private boolean isPublic;

    // TODO move to oauth2ConfigProvider
    @Lob
    @Column(name = "oauth_configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> oauthConfigurationMap;

    // TODO move to termsOfServiceConfigProvider
    @Lob
    @Column(name = "tos_configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> tosConfigurationMap;

    // TODO move to localizationConfigProvider
    @Lob
    @Column(name = "localization_configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> localizationConfigurationMap;

    // TODO move to templatesConfigProvider
    @Lob
    @Column(name = "templates_configuration_map")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Serializable> templatesConfigurationMap;    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Map<String, Serializable> getOAuthConfigurationMap() {
        return oauthConfigurationMap;
    }

    public void setOAuthConfigurationMap(Map<String, Serializable> oauthConfigurationMap) {
        this.oauthConfigurationMap = oauthConfigurationMap;
    }

    public Map<String, Serializable> getTosConfigurationMap() {
        return tosConfigurationMap;
    }

    public void setTosConfigurationMap(Map<String, Serializable> tosConfigurationMap) {
        this.tosConfigurationMap = tosConfigurationMap;
    }

    public Map<String, Serializable> getLocalizationConfigurationMap() {
        return localizationConfigurationMap;
    }

    public void setLocalizationConfigurationMap(Map<String, Serializable> localizationConfigurationMap) {
        this.localizationConfigurationMap = localizationConfigurationMap;
    }

    public Map<String, Serializable> getTemplatesConfigurationMap() {
        return templatesConfigurationMap;
    }

    public void setTemplatesConfigurationMap(Map<String, Serializable> templatesConfigurationMap) {
        this.templatesConfigurationMap = templatesConfigurationMap;
    }
    
}
