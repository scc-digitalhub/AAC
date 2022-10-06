package it.smartcommunitylab.aac.templates.provider;

import java.util.HashMap;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.profiles.scope.OpenIdResource;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.templates.model.EndSessionTemplate;
import it.smartcommunitylab.aac.templates.model.FooterTemplate;
import it.smartcommunitylab.aac.templates.model.LoginTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.model.UserApprovalTemplate;
import it.smartcommunitylab.aac.templates.service.TemplateService;

public class TemplateTemplateProvider
        extends
        AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    private final Resource resource;

    public TemplateTemplateProvider(String providerId,
            TemplateService templateService, Resource oauthResource,
            RealmTemplateProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, providerId, templateService, providerConfig, realm);
        // TODO mock user from props
        this.resource = oauthResource != null ? oauthResource : new OpenIdResource();

        factories = new HashMap<>();
        factories.put(LoginTemplate.TEMPLATE, () -> new LoginTemplate(realm));
        factories.put(EndSessionTemplate.TEMPLATE, () -> new EndSessionTemplate(realm));
        factories.put(UserApprovalTemplate.TEMPLATE, () -> {
            TemplateModel m = new UserApprovalTemplate(realm);
            m.setModelAttribute("resources", resource.getScopes());
            return m;
        });
        factories.put(FooterTemplate.TEMPLATE, () -> new FooterTemplate(realm));

    }

}
