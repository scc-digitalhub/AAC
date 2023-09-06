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

package it.smartcommunitylab.aac.tos;

import static org.assertj.core.api.Assertions.assertThat;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import it.smartcommunitylab.aac.tos.provider.TosTemplateProvider;
import it.smartcommunitylab.aac.tos.templates.TosTemplate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

@SpringJUnitWebConfig
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TosTemplateTest {

    @Autowired
    private BootstrapConfig config;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TosTemplateAuthority tosTemplateAuthority;

    //TODO remove
    @Autowired
    private ProviderConfigRepository<RealmTemplateProviderConfig> registrationRepository;

    private String slug;

    @BeforeEach
    public void setUp() {
        if (slug == null) {
            assertThat(config.getRealms().iterator().hasNext()).isTrue();
            RealmConfig rc = config.getRealms().iterator().next();
            assertThat(rc.getRealm()).isNotNull();

            slug = rc.getRealm().getSlug();
            assertThat(slug).isNotBlank();
        }

        //create provider
        //NOTE: currently providers are auto-created only via manager
        //TODO remove, this should be auto-created
        RealmTemplateProviderConfig conf = registrationRepository.findByProviderId(slug);
        if (conf == null) {
            conf =
                new RealmTemplateProviderConfig(
                    new ConfigurableTemplateProvider(SystemKeys.AUTHORITY_TEMPLATE, slug, slug),
                    new TemplateProviderConfigMap()
                );

            registrationRepository.addRegistration(conf);
        }

        //clear templates
        Collection<TemplateModel> templates = templateService.listTemplates(
            SystemKeys.AUTHORITY_TOS,
            slug,
            TosTemplate.TEMPLATE
        );
        if (!templates.isEmpty()) {
            templates.forEach(t -> {
                try {
                    templateService.deleteTemplate(t.getId());
                } catch (NoSuchTemplateException e) {}
            });
        }
    }

    @Test
    public void tosTemplateProviderIsAvailable() throws Exception {
        TosTemplateProvider provider = tosTemplateAuthority.getProviderByRealm(slug);
        assertThat(provider).isNotNull();
    }

    @Test
    public void tosTemplateIsAvailableViaProvider() throws Exception {
        String id = "tosTemplateIsAvailableViaProvider";
        TemplateModel templateCreate = createTemplate(slug, id);
        assertThat(templateCreate).isNotNull();

        TosTemplateProvider provider = tosTemplateAuthority.getProviderByRealm(slug);
        assertThat(provider).isNotNull();

        Template template = provider.getTemplate(TosTemplate.TEMPLATE, Locale.ENGLISH);
        assertThat(template).isNotNull();
        assertThat(template.getId()).isEqualTo(id);
        assertThat(template.getAuthority()).isEqualTo(SystemKeys.AUTHORITY_TOS);
        assertThat(template.getRealm()).isEqualTo(slug);
        assertThat(template.getTemplate()).isEqualTo(TosTemplate.TEMPLATE);

        assertThat(template.getLanguage()).isEqualTo("en");
        assertThat(template.getContent()).isEqualTo(TEMPLATE_CONTENT);
    }

    //TODO move CRUD tests to TemplateServiceTests
    @Test
    public void tosTemplateCreateTest() throws Exception {
        TosTemplate reg = new TosTemplate(slug);
        reg.setLanguage("en");
        reg.setContent(TEMPLATE_CONTENT);
        TemplateModel template = templateService.addTemplate(
            "tosTemplateCreateTest",
            SystemKeys.AUTHORITY_TOS,
            slug,
            reg
        );
        assertThat(template).isNotNull();
        assertThat(template.getId()).isEqualTo("tosTemplateCreateTest");
        assertThat(template.getAuthority()).isEqualTo(SystemKeys.AUTHORITY_TOS);
        assertThat(template.getRealm()).isEqualTo(slug);
        assertThat(template.getTemplate()).isEqualTo(TosTemplate.TEMPLATE);

        assertThat(template.getLanguage()).isEqualTo("en");
        assertThat(template.getContent()).isEqualTo(TEMPLATE_CONTENT);
    }

    @Test
    public void tosTemplateUpdateTest() throws Exception {
        String id = "tosTemplateUpdateTest";

        TemplateModel template = createTemplate(slug, id);
        assertThat(template.getContent()).isEqualTo(TEMPLATE_CONTENT);

        Map<String, String> content = Arrays
            .stream(TosTemplate.KEYS)
            .collect(Collectors.toMap(k -> k, k -> "TEST_TEMPLATE_UPDATED"));

        template.setContent(content);
        TemplateModel templateUpdated = templateService.updateTemplate(template.getId(), template);
        assertThat(templateUpdated.getContent()).isEqualTo(content);

        TemplateModel templateFind = templateService.findTemplate(id);
        assertThat(templateFind).isNotNull();
        assertThat(templateFind.getContent()).isEqualTo(content);
    }

    @Test
    public void tosTemplateDeleteTest() throws Exception {
        String id = "tosTemplateDeleteTest";
        TemplateModel template = createTemplate(slug, id);
        assertThat(template).isNotNull();

        TemplateModel templateFind = templateService.findTemplate(id);
        assertThat(templateFind).isNotNull();

        templateService.deleteTemplate(id);
        TemplateModel templateDeleted = templateService.findTemplate(id);
        assertThat(templateDeleted).isNull();
    }

    private TemplateModel createTemplate(String slug, String id) throws RegistrationException {
        TosTemplate reg = new TosTemplate(slug);
        reg.setLanguage("en");
        reg.setContent(TEMPLATE_CONTENT);
        return templateService.addTemplate(id, SystemKeys.AUTHORITY_TOS, slug, reg);
    }

    public static final Map<String, String> TEMPLATE_CONTENT = Arrays
        .stream(TosTemplate.KEYS)
        .collect(Collectors.toMap(k -> k, k -> "TEST_TEMPLATE"));
}
