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

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.core.authorities.TemplateProviderAuthority;
import it.smartcommunitylab.aac.core.persistence.TemplateProviderEntity;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableTemplateProvider;

import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TemplateProviderService
    extends ConfigurableProviderService<TemplateProviderAuthority<?, ?, ?, ?>, ConfigurableTemplateProvider, TemplateProviderEntity> {

    public TemplateProviderService(
        TemplateProviderAuthorityService authorityService,
        ConfigurableProviderEntityService<TemplateProviderEntity> providerService
    ) {
        super(authorityService, providerService);
        // set converters
        setConfigConverter(new TemplateProviderConfigConverter());
        setEntityConverter(new TemplateProviderEntityConverter());
    }

    class TemplateProviderConfigConverter implements Converter<ConfigurableTemplateProvider, TemplateProviderEntity> {

        @Override
        public TemplateProviderEntity convert(ConfigurableTemplateProvider reg) {
            TemplateProviderEntity pe = new TemplateProviderEntity();

            pe.setAuthority(reg.getAuthority());
            pe.setProvider(reg.getProvider());
            pe.setRealm(reg.getRealm());

            String name = reg.getName();
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            pe.setName(name);

            Map<String, String> titleMap = null;
            if (reg.getTitleMap() != null) {
                // cleanup every field via safelist
                titleMap =
                    reg
                        .getTitleMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setTitleMap(titleMap);

            Map<String, String> descriptionMap = null;
            if (reg.getDescriptionMap() != null) {
                // cleanup every field via safelist
                descriptionMap =
                    reg
                        .getDescriptionMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setDescriptionMap(descriptionMap);

            pe.setLanguages(StringUtils.collectionToCommaDelimitedString(reg.getLanguages()));
            pe.setCustomStyle(reg.getCustomStyle());

            pe.setEnabled(reg.isEnabled());
            pe.setConfigurationMap(reg.getConfiguration());
            pe.setVersion(reg.getVersion());

            return pe;
        }
    }

    class TemplateProviderEntityConverter implements Converter<TemplateProviderEntity, ConfigurableTemplateProvider> {

        @Override
        public ConfigurableTemplateProvider convert(TemplateProviderEntity pe) {
            ConfigurableTemplateProvider cp = new ConfigurableTemplateProvider(
                pe.getAuthority(),
                pe.getProvider(),
                pe.getRealm()
            );

            cp.setName(pe.getName());
            cp.setTitleMap(pe.getTitleMap());
            cp.setDescriptionMap(pe.getDescriptionMap());

            cp.setLanguages(StringUtils.commaDelimitedListToSet(pe.getLanguages()));
            cp.setCustomStyle(pe.getCustomStyle());

            cp.setEnabled(pe.isEnabled());

            cp.setConfiguration(pe.getConfigurationMap());
            cp.setVersion(pe.getVersion());

            return cp;
        }
    }
}
