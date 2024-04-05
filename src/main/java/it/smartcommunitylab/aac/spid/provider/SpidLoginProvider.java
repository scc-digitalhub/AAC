/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.identity.base.AbstractLoginProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import org.apache.commons.lang3.ArrayUtils;

public class SpidLoginProvider extends AbstractLoginProvider {

    public static final String[] ICONS = { "google", "facebook", "github", "microsoft", "apple", "instagram" };

    private List<SpidIdpButton> idpButtons;

    public SpidLoginProvider(String authority, String providerId, String realm, String name) {
        super(authority, providerId, realm, name);
        setTemplate("button-spid");

        // TODO: tutto quanto sotto probabilmente è da rivedere
        // no custom icon for now, default on authority or key
        String icon = "it-key";
        if (ArrayUtils.contains(ICONS, getAuthority())) {
            icon = "logo-" + getAuthority();
        } else if (ArrayUtils.contains(ICONS, getKey())) {
            icon = "logo-" + getKey();
        }

        String iconUrl = icon.startsWith("logo-") ? "svg/sprite.svg#" + icon : "italia/svg/sprite.svg#" + icon;

        setIcon(icon);
        setIconUrl(iconUrl);
        this.idpButtons = new LinkedList<>();
    }

    public List<SpidIdpButton> getIdpButtons() {
        List<SpidIdpButton> entries = new ArrayList<>(idpButtons);
        Collections.shuffle(entries);
        return entries;
    }

    public void setIdpButtons(List<SpidIdpButton> spidIdpButtons) {
        this.idpButtons = spidIdpButtons;
    }

    /*
     * DTO object for an upstream SPID identity provider available in the login page
     */
    public static class SpidIdpButton {

        @NotBlank
        private String entityId;

        @NotBlank
        private String entityName;

        @NotBlank
        private String metadataUrl;

        private String entityLabel;
        private String iconUrl;
        private String loginUrl;

        public SpidIdpButton(
            String entityId,
            String entityName,
            String metadataUrl,
            String entityLabel,
            String iconUrl,
            String loginUrl
        ) {
            this.entityId = entityId;
            this.entityName = entityName;
            this.metadataUrl = metadataUrl;
            this.entityLabel = entityLabel;
            this.iconUrl = iconUrl;
            this.loginUrl = loginUrl;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public String getMetadataUrl() {
            return metadataUrl;
        }

        public void setMetadataUrl(String metadataUrl) {
            this.metadataUrl = metadataUrl;
        }

        public String getEntityLabel() {
            return entityLabel;
        }

        public void setEntityLabel(String entityLabel) {
            this.entityLabel = entityLabel;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public void setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }

        public String getLoginUrl() {
            return loginUrl;
        }

        public void setLoginUrl(String loginUrl) {
            this.loginUrl = loginUrl;
        }
    }
}
