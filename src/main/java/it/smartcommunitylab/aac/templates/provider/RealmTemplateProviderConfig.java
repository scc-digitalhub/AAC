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

    private Set<String> languages;

    public RealmTemplateProviderConfig(String authority, String provider, String realm,
            TemplateProviderConfigMap configMap) {
        super(authority, provider, realm, configMap);
        this.languages = Collections.emptySet();
    }

    public RealmTemplateProviderConfig(ConfigurableTemplateProvider cp) {
        super(cp);
        this.languages = cp.getLanguages();
    }

    public Set<String> getLanguages() {
        return languages != null ? languages : new TreeSet<>(Arrays.asList(LanguageService.LANGUAGES));
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    @Override
    public ConfigurableTemplateProvider getConfigurable() {
        ConfigurableTemplateProvider cp = new ConfigurableTemplateProvider(getAuthority(), getProvider(), getRealm());
        cp.setName(getName());
        cp.setDescription(getDescription());

        cp.setLanguages(getLanguages());

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());

        return cp;
    }

}
