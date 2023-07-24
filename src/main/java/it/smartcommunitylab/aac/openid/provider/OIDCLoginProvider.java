package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractLoginProvider;
import org.apache.commons.lang3.ArrayUtils;

public class OIDCLoginProvider extends AbstractLoginProvider {

    public OIDCLoginProvider(String provider, String realm, String name) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm, name);
    }

    public OIDCLoginProvider(String authority, String provider, String realm, String name) {
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

    public static final String[] ICONS = { "google", "facebook", "github", "microsoft", "apple", "instagram" };
}
