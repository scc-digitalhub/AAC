package it.smartcommunitylab.aac.internal.provider;

import java.util.HashMap;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.internal.templates.InternalChangeTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalConfirmTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalRegisterTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;

public class InternalTemplateProvider
        extends
        AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    public InternalTemplateProvider(String providerId,
            TemplateService templateService,
            RealmTemplateProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, templateService, providerConfig, realm);

        factories = new HashMap<>();
        factories.put(InternalConfirmTemplate.TEMPLATE, () -> new InternalConfirmTemplate(realm));
        factories.put(InternalRegisterTemplate.TEMPLATE, () -> new InternalRegisterTemplate(realm));
        factories.put(InternalChangeTemplate.TEMPLATE, () -> new InternalChangeTemplate(realm));
    }

}
