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

package it.smartcommunitylab.aac.tos.provider;

import java.util.HashMap;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractTemplateProvider;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import it.smartcommunitylab.aac.tos.templates.TosApproveTemplate;
import it.smartcommunitylab.aac.tos.templates.TosTemplate;

public class TosTemplateProvider
		extends AbstractTemplateProvider<TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

	public TosTemplateProvider(String providerId, TemplateService templateService,
			RealmTemplateProviderConfig providerConfig, String realm) {
		super(SystemKeys.AUTHORITY_TOS, providerId, templateService, providerConfig, realm);
		factories = new HashMap<>();
		factories.put(TosTemplate.TEMPLATE, () -> {
			TemplateModel m = new TosTemplate(realm);
			return m;
		});
		factories.put(TosApproveTemplate.TEMPLATE, () -> {
			TemplateModel m = new TosApproveTemplate(realm);
			return m;
		});
	}
}
