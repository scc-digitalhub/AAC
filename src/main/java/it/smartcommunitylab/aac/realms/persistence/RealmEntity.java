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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
