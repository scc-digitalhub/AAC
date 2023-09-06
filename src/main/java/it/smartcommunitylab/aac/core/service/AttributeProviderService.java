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

import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AttributeProviderService
    extends ConfigurableProviderService<AttributeProviderAuthority<?, ?, ?>, ConfigurableAttributeProvider, AttributeProviderEntity> {

    public AttributeProviderService(
        AttributeProviderAuthorityService authorityService,
        ConfigurableProviderEntityService<AttributeProviderEntity> providerService
    ) {
        super(authorityService, providerService);
        // set converters
        setEntityConverter(new AttributeProviderEntityConverter());
        setConfigConverter(new AttributeProviderConfigConverter());
    }

    class AttributeProviderEntityConverter
        implements Converter<AttributeProviderEntity, ConfigurableAttributeProvider> {

        @Override
        public ConfigurableAttributeProvider convert(AttributeProviderEntity pe) {
            ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(
                pe.getAuthority(),
                pe.getProvider(),
                pe.getRealm()
            );
            cp.setConfiguration(pe.getConfigurationMap());
            cp.setVersion(pe.getVersion());

            cp.setEnabled(pe.isEnabled());
            cp.setPersistence(pe.getPersistence());
            cp.setEvents(pe.getEvents());

            cp.setName(pe.getName());
            cp.setTitleMap(pe.getTitleMap());
            cp.setDescriptionMap(pe.getDescriptionMap());

            Set<String> attributeSets = pe.getAttributeSets() != null
                ? StringUtils.commaDelimitedListToSet(pe.getAttributeSets())
                : Collections.emptySet();
            cp.setAttributeSets(attributeSets);

            return cp;
        }
    }

    class AttributeProviderConfigConverter
        implements Converter<ConfigurableAttributeProvider, AttributeProviderEntity> {

        @Override
        public AttributeProviderEntity convert(ConfigurableAttributeProvider reg) {
            AttributeProviderEntity pe = new AttributeProviderEntity();

            pe.setAuthority(reg.getAuthority());
            pe.setProvider(reg.getProvider());
            pe.setRealm(reg.getRealm());
            pe.setEnabled(reg.isEnabled());

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

            // TODO add enum
            String persistence = reg.getPersistence();
            String events = reg.getEvents();

            pe.setPersistence(persistence);
            pe.setEvents(events);

            pe.setConfigurationMap(reg.getConfiguration());
            pe.setVersion(reg.getVersion());

            pe.setAttributeSets(StringUtils.collectionToCommaDelimitedString(reg.getAttributeSets()));

            return pe;
        }
    }
}
