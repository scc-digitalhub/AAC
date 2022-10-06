package it.smartcommunitylab.aac.internal.provider;

import java.util.HashMap;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.internal.dto.UserRegistrationBean;
import it.smartcommunitylab.aac.internal.templates.InternalChangeAccountSuccessTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalChangeAccountTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalConfirmAccountTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalRegisterAccountSuccessTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalRegisterAccountTemplate;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
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

        // TODO add mocking user props via config to build templates

        factories = new HashMap<>();
        factories.put(InternalConfirmAccountTemplate.TEMPLATE, () -> new InternalConfirmAccountTemplate(realm));
        factories.put(InternalRegisterAccountTemplate.TEMPLATE, () -> {
            TemplateModel t = new InternalRegisterAccountTemplate(realm);
            t.setModelAttribute("reg", new UserRegistrationBean());
            t.setModelAttribute("policy", new PasswordPolicy());
            return t;
        });
        factories.put(InternalRegisterAccountSuccessTemplate.TEMPLATE,
                () -> new InternalRegisterAccountSuccessTemplate(realm));
        factories.put(InternalChangeAccountTemplate.TEMPLATE, () -> {
            TemplateModel t = new InternalChangeAccountTemplate(realm);
            t.setModelAttribute("reg", new UserRegistrationBean());
            t.setModelAttribute("policy", new PasswordPolicy());
            return t;
        });
        factories.put(InternalChangeAccountSuccessTemplate.TEMPLATE,
                () -> new InternalChangeAccountSuccessTemplate(realm));

    }

}
