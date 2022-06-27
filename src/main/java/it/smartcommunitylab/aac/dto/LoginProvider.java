package it.smartcommunitylab.aac.dto;

import org.springframework.util.Assert;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;

public class LoginProvider implements Comparable<LoginProvider> {
    private String provider;
    private String authority;
    private String realm;

    private String template;
    private String loginUrl;

    private String name;
    private String description;
    private String icon;
    private String iconUrl;

    private String cssClass;

    private ConfigurableProperties configuration;

    public LoginProvider(String authority, String provider, String realm) {
        Assert.hasText(provider, "provider can not be null or empty");
        this.provider = provider;
        this.authority = authority;
        this.realm = realm;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        if (icon == null) {
            return "it-key";
        }

        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconUrl() {
        if (iconUrl == null) {
            return "italia/svg/sprite.svg#" + getIcon();
        }

        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getCssClass() {
        if (cssClass == null) {
            return "provider-" + authority + " provider-" + getKey();
        }

        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getFragment() {
        return "login/" + getTemplate();
    }

    public ConfigurableProperties getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConfigurableProperties configuration) {
        this.configuration = configuration;
    }

    @Override
    public int compareTo(LoginProvider o) {
        return name.compareTo(((LoginProvider) o).name);
    }

    public String getKey() {
        if (name == null) {
            return provider;
        }

        return name.trim()
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

//    public static LoginProvider from(
//            IdentityProvider<? extends UserIdentity> idp) {
//        LoginProvider a = new LoginProvider(idp.getProvider());
//        a.authority = idp.getAuthority();
//        a.provider = idp.getProvider();
//        a.realm = idp.getRealm();
//
//        a.loginUrl = idp.getAuthenticationUrl();
//
//        a.name = idp.getName();
//        a.description = StringUtils.hasText(idp.getDescription()) ? idp.getDescription().trim() : null;
//
//        String authority = idp.getAuthority();
//        String key = a.name.trim()
//                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
//        a.cssClass = "provider-" + authority + " provider-" + key;
//        a.icon = "it-key";
//        a.iconUrl = "italia/svg/sprite.svg#" + a.icon;
//
//        a.configuration = idp.getConfig();
//        return a;
//    }

}
