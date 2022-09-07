package it.smartcommunitylab.aac.core.base;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.provider.LoginProvider;

public abstract class AbstractLoginProvider extends AbstractProvider
        implements LoginProvider, Comparable<LoginProvider> {

    public static final String DEFAULT_ICON = "it-key";
    protected static final String ICON_PATH = "italia/svg/sprite.svg#";
    protected static final String TEMPLATE_PATH = "login/";
    protected static final int MAX_POSITION = 10000000;

    private String template;
    private String loginUrl;
    private Integer position;

    private String name;
    private String description;
    private String icon;
    private String iconUrl;

    private String cssClass;

    private ConfigurableProperties configuration;

    public AbstractLoginProvider(String authority, String providerId, String realm, String name) {
        super(authority, providerId, realm);
        Assert.hasText(name, "name can not be null or empty");

        this.name = name;

        // by default position is undefined
        this.position = null;
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_LOGIN;
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

        return name.trim()
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
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
