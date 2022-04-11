package it.smartcommunitylab.aac.dto;

import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;

public class LoginAuthorityBean implements Comparable<LoginAuthorityBean> {
    private String provider;
    private String authority;
    private String realm;

    private String loginUrl;
    private String registrationUrl;
    private String resetUrl;

    private String name;
    private String description;
    private String icon;
    private String iconUrl;

    private String displayMode;
    private String cssClass;

    private ConfigurableProperties configuration;

    public LoginAuthorityBean(String provider) {
        Assert.hasText(provider, "provider can not be null or empty");
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getRegistrationUrl() {
        return registrationUrl;
    }

    public void setRegistrationUrl(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }

    public String getResetUrl() {
        return resetUrl;
    }

    public void setResetUrl(String resetUrl) {
        this.resetUrl = resetUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getFragment() {
        return "login/" + getDisplayMode();
    }

    public ConfigurableProperties getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConfigurableProperties configuration) {
        this.configuration = configuration;
    }

    @Override
    public int compareTo(LoginAuthorityBean o) {
        return name.compareTo(((LoginAuthorityBean) o).name);
    }

    public static LoginAuthorityBean from(IdentityProvider idp) {
        LoginAuthorityBean a = new LoginAuthorityBean(idp.getProvider());
        a.authority = idp.getAuthority();
        a.provider = idp.getProvider();
        a.realm = idp.getRealm();

        a.loginUrl = idp.getAuthenticationUrl();
        Map<String, String> actionUrls = idp.getActionUrls();
        if (actionUrls != null) {
            a.registrationUrl = actionUrls.get(SystemKeys.ACTION_REGISTER);
            a.resetUrl = actionUrls.get(SystemKeys.ACTION_RESET);
        }

        a.name = idp.getName();
        a.description = idp.getDescription();
        String key = a.name.trim()
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        a.cssClass = "provider-" + key;
        a.icon = "it-key";
        if (ArrayUtils.contains(ICONS, key)) {
            a.icon = "logo-" + key;
        }
        a.iconUrl = a.icon.startsWith("logo-") ? "svg/sprite.svg#" + a.icon : "italia/svg/sprite.svg#" + a.icon;
        a.displayMode = idp.getDisplayMode() != null ? idp.getDisplayMode() : SystemKeys.DISPLAY_MODE_BUTTON;

        return a;
    }

    public final static String[] ICONS = {
            "google", "facebook", "github", "microsoft", "apple", "instagram"
    };
}
