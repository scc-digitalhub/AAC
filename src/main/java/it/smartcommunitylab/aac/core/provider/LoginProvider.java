package it.smartcommunitylab.aac.core.provider;

public interface LoginProvider extends ResourceProvider {
    /*
     * Config
     */
    public String getName();

    public String getDescription();

    public Integer getPosition();

    /*
     * Login
     */
    public String getLoginUrl();

    /*
     * Template render
     */
    public String getTemplate();

    public String getIconUrl();

    public String getCssClass();
}
