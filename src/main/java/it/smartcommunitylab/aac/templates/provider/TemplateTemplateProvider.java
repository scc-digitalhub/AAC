/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.templates.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.profiles.scope.OpenIdResource;
import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.templates.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.templates.model.EndSessionTemplate;
import it.smartcommunitylab.aac.templates.model.FooterTemplate;
import it.smartcommunitylab.aac.templates.model.LoginTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.model.UserApprovalTemplate;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import java.util.HashMap;
import java.util.UUID;

public class TemplateTemplateProvider
    extends AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    private final Resource resource;
    private final InternalUserAccount account;
    private final OAuth2ClientDetails clientDetails;

    public TemplateTemplateProvider(
        String providerId,
        TemplateService templateService,
        Resource oauthResource,
        RealmTemplateProviderConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_TEMPLATE, providerId, templateService, providerConfig, realm);
        // TODO mock user from props
        this.resource = oauthResource != null ? oauthResource : new OpenIdResource();

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
        factories.put(
            EndSessionTemplate.TEMPLATE,
            () -> {
                TemplateModel m = new EndSessionTemplate(realm);
                m.setModelAttribute("account", account);
                return m;
            }
        );
        factories.put(
            UserApprovalTemplate.TEMPLATE,
            () -> {
                TemplateModel m = new UserApprovalTemplate(realm);
                m.setModelAttribute("account", account);
                m.setModelAttribute("client", clientDetails);
                m.setModelAttribute("resources", resource.getScopes());
                return m;
            }
        );
        factories.put(FooterTemplate.TEMPLATE, () -> new FooterTemplate(realm));
    }
}
