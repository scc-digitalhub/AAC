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

package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.provider.LoginProvider;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.Assert;

public abstract class AbstractLoginProvider implements LoginProvider, Comparable<LoginProvider> {

    public static final String DEFAULT_ICON = "it-key";
    protected static final String ICON_PATH = "italia/svg/sprite.svg#";
    protected static final String TEMPLATE_PATH = "login/";
    protected static final int MAX_POSITION = 10000000;

    private final String authority;
    private final String realm;
    private final String provider;

    private String template;
    private String loginUrl;
    private Integer position;

    private String name;
    private Map<String, String> titleMap;
    private Map<String, String> descriptionMap;
    private String icon;
    private String iconUrl;

    private String cssClass;

    private ConfigurableProperties configuration;

    public AbstractLoginProvider(String authority, String providerId, String realm, String name) {
        Assert.hasText(name, "name can not be null or empty");
        this.authority = authority;
        this.realm = realm;
        this.provider = providerId;

        this.name = name;

        // by default position is undefined
        this.position = null;
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
    public String getRealm() {
        return realm;
    }

    //    @Override
    //    public final String getType() {
    //        return SystemKeys.RESOURCE_LOGIN;
    //    }

    public String getTemplate() {
        if (template == null) {
            return "button";
        }

        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle(String lang) {
        if (titleMap != null && lang != null) {
            return titleMap.get(lang);
        }

        return name;
    }

    public String getDescription(String lang) {
        if (descriptionMap != null && lang != null) {
            return descriptionMap.get(lang);
        }

        return null;
    }

    public Map<String, String> getTitleMap() {
        return titleMap;
    }

    public void setTitleMap(Map<String, String> titleMap) {
        this.titleMap = titleMap;
    }

    public Map<String, String> getDescriptionMap() {
        return descriptionMap;
    }

    public void setDescriptionMap(Map<String, String> descriptionMap) {
        this.descriptionMap = descriptionMap;
    }

    public String getIcon() {
        if (icon == null) {
            return DEFAULT_ICON;
        }

        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconUrl() {
        if (iconUrl == null) {
            return ICON_PATH + getIcon();
        }

        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getCssClass() {
        if (cssClass == null) {
            return "provider-" + getAuthority() + " provider-" + getKey();
        }

        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getFragment() {
        return TEMPLATE_PATH + getTemplate();
    }

    public ConfigurableProperties getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConfigurableProperties configuration) {
        this.configuration = configuration;
    }

    @Override
    public int compareTo(LoginProvider o) {
        int c = getPosition().compareTo(o.getPosition());

        if (c == 0) {
            // use name
            c = getName().compareTo(o.getName());
        }

        return c;
    }

    public String getKey() {
        if (name == null) {
            return getProvider();
        }

        return name.trim().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    public Integer getPosition() {
        if (position != null) {
            return position;
        }

        return MAX_POSITION;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
