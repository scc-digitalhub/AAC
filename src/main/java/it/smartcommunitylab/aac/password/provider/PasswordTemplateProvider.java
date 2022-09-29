package it.smartcommunitylab.aac.password.provider;

import java.util.HashMap;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.password.templates.PasswordChangeTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordLoginTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordPolicyTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordResetTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;

public class PasswordTemplateProvider
        extends
        AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    public PasswordTemplateProvider(String providerId,
            TemplateService templateService,
            RealmTemplateProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, templateService, providerConfig, realm);

        factories = new HashMap<>();
        factories.put(PasswordLoginTemplate.TEMPLATE, () -> new PasswordLoginTemplate(realm));
        factories.put(PasswordChangeTemplate.TEMPLATE, () -> new PasswordChangeTemplate(realm));
        factories.put(PasswordPolicyTemplate.TEMPLATE, () -> new PasswordPolicyTemplate(realm));
        factories.put(PasswordResetTemplate.TEMPLATE, () -> new PasswordResetTemplate(realm));
    }

}
