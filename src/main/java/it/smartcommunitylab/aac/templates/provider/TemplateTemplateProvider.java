package it.smartcommunitylab.aac.templates.provider;

import java.util.HashMap;
import java.util.UUID;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.templates.model.EndSessionTemplate;
import it.smartcommunitylab.aac.templates.model.FooterTemplate;
import it.smartcommunitylab.aac.templates.model.LoginTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.model.UserApprovalTemplate;
import it.smartcommunitylab.aac.templates.service.TemplateService;

public class TemplateTemplateProvider
        extends
        AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    private final InternalUserAccount account;
    private final OAuth2ClientDetails clientDetails;

    public TemplateTemplateProvider(String providerId,
            TemplateService templateService, ApiResource apiResource,
            RealmTemplateProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, providerId, templateService, providerConfig, realm);

        // TODO mock user from props
        // TODO add mocking user props via config to build templates
        account = new InternalUserAccount();
        account.setRealm(realm);
        account.setName("Mock");
        account.setSurname("User");
        account.setEmail("mockuser@test.local.me");
        account.setUsername(account.getEmail());
        account.setUuid(UUID.randomUUID().toString());

        clientDetails = new OAuth2ClientDetails();
        clientDetails.setClientId(UUID.randomUUID().toString());
        clientDetails.setRealm(realm);
        clientDetails.setName("ClientApp");

        factories = new HashMap<>();
        factories.put(LoginTemplate.TEMPLATE, () -> new LoginTemplate(realm));
        factories.put(EndSessionTemplate.TEMPLATE, () -> {
            TemplateModel m = new EndSessionTemplate(realm);
            m.setModelAttribute("account", account);
            return m;
        });
        factories.put(UserApprovalTemplate.TEMPLATE, () -> {
            TemplateModel m = new UserApprovalTemplate(realm);
            m.setModelAttribute("account", account);
            m.setModelAttribute("client", clientDetails);
            if (apiResource != null) {
                m.setModelAttribute("resources", apiResource.getScopes());
            }
            return m;
        });
        factories.put(FooterTemplate.TEMPLATE, () -> new FooterTemplate(realm));

    }

}
