package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.dto.UserEmail;
import it.smartcommunitylab.aac.password.model.InternalEditableUserPassword;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.templates.PasswordChangeSuccessTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordChangeTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordPolicyTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordResetSuccessTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordResetTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import java.util.HashMap;

public class PasswordTemplateProvider
    extends AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    public PasswordTemplateProvider(
        String providerId,
        TemplateService templateService,
        RealmTemplateProviderConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, templateService, providerConfig, realm);
        factories = new HashMap<>();
        factories.put(
            PasswordChangeTemplate.TEMPLATE,
            () -> {
                TemplateModel m = new PasswordChangeTemplate(realm);
                m.setModelAttribute("reg", new InternalEditableUserPassword());
                m.setModelAttribute("policy", new PasswordPolicy());
                return m;
            }
        );
        factories.put(PasswordChangeSuccessTemplate.TEMPLATE, () -> new PasswordChangeSuccessTemplate(realm));
        factories.put(
            PasswordPolicyTemplate.TEMPLATE,
            () -> {
                TemplateModel m = new PasswordPolicyTemplate(realm);
                m.setModelAttribute("policy", new PasswordPolicy());
                return m;
            }
        );
        factories.put(
            PasswordResetTemplate.TEMPLATE,
            () -> {
                TemplateModel m = new PasswordResetTemplate(realm);
                m.setModelAttribute("reg", new UserEmail());
                return m;
            }
        );
        factories.put(
            PasswordResetSuccessTemplate.TEMPLATE,
            () -> {
                TemplateModel m = new PasswordResetSuccessTemplate(realm);
                m.setModelAttribute("reg", new UserEmail());
                return m;
            }
        );
    }
}
