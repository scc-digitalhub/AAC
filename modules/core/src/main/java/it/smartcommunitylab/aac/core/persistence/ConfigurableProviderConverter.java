/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.core.persistence;

import com.fasterxml.jackson.annotation.JsonTypeName;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/*
 * Convert between a types config and the generic persistence entity
 */
public class ConfigurableProviderConverter
    implements Converter<ConfigurableProvider<? extends ConfigMap>, ProviderEntity> {

    @Override
    public ProviderEntity convert(ConfigurableProvider<? extends ConfigMap> reg) {
        ProviderEntity pe = new ProviderEntity();

        pe.setType(reg.getClass().getName());
        //try read name from annotation
        try {
            JsonTypeName ann = reg.getClass().getAnnotation(JsonTypeName.class);
            if (ann != null && ann.value() != null) {
                pe.setType(ann.value());
            }
        } catch (Exception e) {
            //ignore, named type is not required
        }

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
            titleMap = reg
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
            descriptionMap = reg
                .getDescriptionMap()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }
        pe.setDescriptionMap(descriptionMap);

        //configuration
        pe.setSettingsMap(reg.getSettings());
        pe.setConfigurationMap(reg.getConfiguration());
        pe.setVersion(reg.getVersion());
        pe.setEnabled(reg.isEnabled());

        return pe;
    }
}
