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

package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.dto.UserEmail;
import it.smartcommunitylab.aac.password.model.InternalEditableUserPassword;
import it.smartcommunitylab.aac.password.model.PasswordPolicy;
import it.smartcommunitylab.aac.password.templates.PasswordChangeSuccessTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordChangeTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordPolicyTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordResetSuccessTemplate;
import it.smartcommunitylab.aac.password.templates.PasswordResetTemplate;
import it.smartcommunitylab.aac.templates.base.AbstractTemplateProvider;
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
                m.setModelAttribute("reg", new InternalEditableUserPassword(getRealm(), null));
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
