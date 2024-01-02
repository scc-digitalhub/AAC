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

package it.smartcommunitylab.aac.openidfed.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractLoginProvider;
import it.smartcommunitylab.aac.openidfed.model.OpenIdFedLogin;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class OpenIdFedLoginProvider extends AbstractLoginProvider {

    private List<OpenIdFedLogin> entries = Collections.emptyList();

    public OpenIdFedLoginProvider(String provider, String realm, String name) {
        this(SystemKeys.AUTHORITY_OPENIDFED, provider, realm, name);
    }

    public OpenIdFedLoginProvider(String authority, String provider, String realm, String name) {
        super(authority, provider, realm, name);
        // default config
        setTemplate("button-expand");

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
    }

    public List<OpenIdFedLogin> getEntries() {
        return entries;
    }

    public void setEntries(List<OpenIdFedLogin> entries) {
        this.entries = entries;
    }

    public static final String[] ICONS = { "cie", "spid" };
}
