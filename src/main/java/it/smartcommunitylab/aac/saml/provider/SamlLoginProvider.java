package it.smartcommunitylab.aac.saml.provider;

import org.apache.commons.lang3.ArrayUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractLoginProvider;

public class SamlLoginProvider extends AbstractLoginProvider {

    public SamlLoginProvider(String provider, String realm, String name) {
        this(SystemKeys.AUTHORITY_SAML, provider, realm, name);
    }

    public SamlLoginProvider(String authority, String provider, String realm, String name) {
        super(authority, provider, realm, name);

        // default config
        setTemplate("button");

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

    public static final String[] ICONS = {
            "google", "facebook", "github", "microsoft", "apple", "instagram"
    };

}
