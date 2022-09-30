package it.smartcommunitylab.aac.templates.provider;

import java.util.HashMap;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.templates.model.EndSessionTemplate;
import it.smartcommunitylab.aac.templates.model.FooterTemplate;
import it.smartcommunitylab.aac.templates.model.LoginTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.model.UserApprovalTemplate;
import it.smartcommunitylab.aac.templates.service.TemplateService;

public class TemplateTemplateProvider
        extends
        AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    public TemplateTemplateProvider(String providerId,
            TemplateService templateService,
            RealmTemplateProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, providerId, templateService, providerConfig, realm);

        factories = new HashMap<>();
        factories.put(LoginTemplate.TEMPLATE, () -> new LoginTemplate(realm));
        factories.put(UserApprovalTemplate.TEMPLATE, () -> new UserApprovalTemplate(realm));
        factories.put(EndSessionTemplate.TEMPLATE, () -> new EndSessionTemplate(realm));
        factories.put(FooterTemplate.TEMPLATE, () -> new FooterTemplate(realm));

    }

}
