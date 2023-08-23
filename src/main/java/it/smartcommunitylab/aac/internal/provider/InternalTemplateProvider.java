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

package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.dto.UserRegistrationBean;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.templates.InternalChangeAccountSuccessTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalChangeAccountTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalRegisterAccountConfirmTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalRegisterAccountSuccessTemplate;
import it.smartcommunitylab.aac.internal.templates.InternalRegisterAccountTemplate;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.templates.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import java.util.HashMap;

public class InternalTemplateProvider
    extends AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    private final InternalUserAccount account;
    private final UserRegistrationBean registration;

    public InternalTemplateProvider(
        String providerId,
        TemplateService templateService,
        RealmTemplateProviderConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, templateService, providerConfig, realm);
        // TODO add mocking user props via config to build templates
        account = new InternalUserAccount(SystemKeys.AUTHORITY_INTERNAL, realm, "");
        account.setProvider(providerId);
        account.setName("Mock");
        account.setSurname("User");
        account.setEmail("mockuser@test.local.me");
        account.setUsername(account.getEmail());

        registration = new UserRegistrationBean();
        registration.setEmail(account.getEmail());
        registration.setName(account.getName());
        registration.setSurname(account.getSurname());

        factories = new HashMap<>();
        factories.put(
            InternalRegisterAccountConfirmTemplate.TEMPLATE,
            () -> {
                TemplateModel t = new InternalRegisterAccountConfirmTemplate(realm);
                t.setModelAttribute("reg", registration);
                t.setModelAttribute("account", account);
                return t;
            }
        );
        factories.put(
            InternalRegisterAccountTemplate.TEMPLATE,
            () -> {
                TemplateModel t = new InternalRegisterAccountTemplate(realm);
                t.setModelAttribute("reg", registration);
                t.setModelAttribute("policy", new PasswordPolicy());
                return t;
            }
        );
        factories.put(
            InternalRegisterAccountSuccessTemplate.TEMPLATE,
            () -> {
                TemplateModel t = new InternalRegisterAccountSuccessTemplate(realm);
                t.setModelAttribute("reg", registration);
                t.setModelAttribute("account", account);
                return t;
            }
        );
        factories.put(
            InternalChangeAccountTemplate.TEMPLATE,
            () -> {
                TemplateModel t = new InternalChangeAccountTemplate(realm);
                t.setModelAttribute("reg", registration);
                t.setModelAttribute("policy", new PasswordPolicy());
                t.setModelAttribute("account", account);
                return t;
            }
        );
        factories.put(
            InternalChangeAccountSuccessTemplate.TEMPLATE,
            () -> new InternalChangeAccountSuccessTemplate(realm)
        );
    }
}
