package it.smartcommunitylab.aac.templates.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.templates.service.LanguageService;

public class RealmTemplateProviderConfig
        extends AbstractProviderConfig<TemplateProviderConfigMap, ConfigurableTemplateProvider>
        implements TemplateProviderConfig<TemplateProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + TemplateProviderConfigMap.RESOURCE_TYPE;

    private Set<String> languages;
    private String customStyle;

    public RealmTemplateProviderConfig(String authority, String provider, String realm,
            TemplateProviderConfigMap configMap) {
        super(authority, provider, realm, configMap);
        this.languages = Collections.emptySet();
    }

    public RealmTemplateProviderConfig(ConfigurableTemplateProvider cp, TemplateProviderConfigMap configMap) {
        super(cp, configMap);
        this.languages = cp.getLanguages();
        this.customStyle = cp.getCustomStyle();
    }

    public Set<String> getLanguages() {
        return (languages != null && !languages.isEmpty()) ? languages
                : new TreeSet<>(Arrays.asList(LanguageService.LANGUAGES));
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public String getCustomStyle() {
        return customStyle;
    }

    public void setCustomStyle(String customStyle) {
        this.customStyle = customStyle;
    }

}
