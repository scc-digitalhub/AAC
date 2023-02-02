package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;

public abstract class AbstractProviderConfig<M extends AbstractConfigMap, T extends ConfigurableProvider>
        implements ProviderConfig<M>, Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String realm;
    private final String provider;

    protected String name;
    protected Map<String, String> titleMap;
    protected Map<String, String> descriptionMap;

    protected M configMap;
    protected int version;

    protected AbstractProviderConfig(String authority, String provider, String realm, M configMap) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
        this.configMap = configMap;
        this.version = 0;
    }

    protected AbstractProviderConfig(T cp, M configMap) {
        this(cp.getAuthority(), cp.getProvider(), cp.getRealm(), configMap);
        this.name = cp.getName();
        this.titleMap = cp.getTitleMap();
        this.descriptionMap = cp.getDescriptionMap();

        this.version = cp.getVersion() != null ? cp.getVersion() : 0;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractProviderConfig() {
        this((String) null, (String) null, (String) null, null);
    }

    public String getAuthority() {
        return authority;
    }

    public String getRealm() {
        return realm;
    }

    public String getProvider() {
        return provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle(Locale locale) {
        String lang = locale.getLanguage();
        if (titleMap != null) {
            return titleMap.get(lang);
        }

        return null;
    }

    public Map<String, String> getTitleMap() {
        return titleMap;
    }

    public void setTitleMap(Map<String, String> titleMap) {
        this.titleMap = titleMap;
    }

    public String getDescription(Locale locale) {
        String lang = locale.getLanguage();
        if (descriptionMap != null) {
            return descriptionMap.get(lang);
        }

        return null;
    }

    public Map<String, String> getDescriptionMap() {
        return descriptionMap;
    }

    public void setDescriptionMap(Map<String, String> descriptionMap) {
        this.descriptionMap = descriptionMap;
    }

    public M getConfigMap() {
        return configMap;
    }

    public void setConfigMap(M configMap) {
        this.configMap = configMap;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

}
