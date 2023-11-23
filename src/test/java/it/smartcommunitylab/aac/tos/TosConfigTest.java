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

import static it.smartcommunitylab.aac.tos.controller.TosController.TOS_TERMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.service.RealmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringJUnitWebConfig
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TosConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapConfig config;

    @Autowired
    private RealmService realmService;

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
    }

    @Test
    public void tosConfigIsEnabled() throws Exception {
        Realm realm = realmService.findRealm(slug);
        assertThat(realm).isNotNull();

        TosConfigurationMap tosConfig = realm.getTosConfiguration();
        assertThat(tosConfig).isNotNull();

        //update config to enable tos
        tosConfig.setEnableTOS(true);
        Realm realmUpdated = realmService.updateRealm(
            slug,
            realm.getName(),
            realm.isEditable(),
            realm.isPublic(),
            realm.getOAuthConfiguration().getConfiguration(),
            tosConfig.getConfiguration(),
            null
        );

        //updated model shows 'enabled'
        TosConfigurationMap tosConfigUpdated = realmUpdated.getTosConfiguration();
        assertThat(tosConfigUpdated).isNotNull();
        assertThat(tosConfigUpdated.isEnableTOS()).isTrue();

        // find from db contains 'enabled'
        Realm realmFind = realmService.findRealm(slug);
        assertThat(realmFind).isNotNull();
        TosConfigurationMap tosConfigFind = realmFind.getTosConfiguration();
        assertThat(tosConfigFind).isNotNull();
        assertThat(tosConfigFind.isEnableTOS()).isTrue();

        //update config to disabled tos
        tosConfig.setEnableTOS(false);
        Realm realmUpdated2 = realmService.updateRealm(
            slug,
            realm.getName(),
            realm.isEditable(),
            realm.isPublic(),
            realm.getOAuthConfiguration().getConfiguration(),
            tosConfig.getConfiguration(),
            null
        );

        //updated model shows 'disabled'
        TosConfigurationMap tosConfigUpdated2 = realmUpdated2.getTosConfiguration();
        assertThat(tosConfigUpdated2).isNotNull();
        assertThat(tosConfigUpdated2.isEnableTOS()).isFalse();

        // find from db contains 'disabled'
        Realm realmFind2 = realmService.findRealm(slug);
        assertThat(realmFind2).isNotNull();
        TosConfigurationMap tosConfigFind2 = realmFind2.getTosConfiguration();
        assertThat(tosConfigFind2).isNotNull();
        assertThat(tosConfigFind2.isEnableTOS()).isFalse();

        //check that objects are detached
        assertThat(tosConfigUpdated.isEnableTOS()).isTrue();
        assertThat(tosConfigFind.isEnableTOS()).isTrue();
        assertThat(tosConfigUpdated2.isEnableTOS()).isFalse();
        assertThat(tosConfigFind2.isEnableTOS()).isFalse();
    }

    @Test
    public void tosConfigIsApproval() throws Exception {
        Realm realm = realmService.findRealm(slug);
        assertThat(realm).isNotNull();

        TosConfigurationMap tosConfig = realm.getTosConfiguration();
        assertThat(tosConfig).isNotNull();

        //update config to enable tos
        tosConfig.setApproveTOS(true);
        Realm realmUpdated = realmService.updateRealm(
            slug,
            realm.getName(),
            realm.isEditable(),
            realm.isPublic(),
            realm.getOAuthConfiguration().getConfiguration(),
            tosConfig.getConfiguration(),
            null
        );

        //updated model shows 'enabled'
        TosConfigurationMap tosConfigUpdated = realmUpdated.getTosConfiguration();
        assertThat(tosConfigUpdated).isNotNull();
        assertThat(tosConfigUpdated.isApprovedTOS()).isTrue();

        // find from db contains 'enabled'
        Realm realmFind = realmService.findRealm(slug);
        assertThat(realmFind).isNotNull();
        TosConfigurationMap tosConfigFind = realmFind.getTosConfiguration();
        assertThat(tosConfigFind).isNotNull();
        assertThat(tosConfigFind.isApprovedTOS()).isTrue();

        //update config to disabled tos
        tosConfig.setApproveTOS(false);
        Realm realmUpdated2 = realmService.updateRealm(
            slug,
            realm.getName(),
            realm.isEditable(),
            realm.isPublic(),
            realm.getOAuthConfiguration().getConfiguration(),
            tosConfig.getConfiguration(),
            null
        );

        //updated model shows 'disabled'
        TosConfigurationMap tosConfigUpdated2 = realmUpdated2.getTosConfiguration();
        assertThat(tosConfigUpdated2).isNotNull();
        assertThat(tosConfigUpdated2.isApprovedTOS()).isFalse();

        // find from db contains 'disabled'
        Realm realmFind2 = realmService.findRealm(slug);
        assertThat(realmFind2).isNotNull();
        TosConfigurationMap tosConfigFind2 = realmFind2.getTosConfiguration();
        assertThat(tosConfigFind2).isNotNull();
        assertThat(tosConfigFind2.isApprovedTOS()).isFalse();

        //check that objects are detached
        assertThat(tosConfigUpdated.isApprovedTOS()).isTrue();
        assertThat(tosConfigFind.isApprovedTOS()).isTrue();
        assertThat(tosConfigUpdated2.isApprovedTOS()).isFalse();
        assertThat(tosConfigFind2.isApprovedTOS()).isFalse();
    }

    @Test
    public void termsPageIsPublicWhenEnabled() throws Exception {
        //enable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(true);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get("/-/" + slug + TOS_TERMS);

        // expect terms in response
        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();
        // page should contain tos
        //TODO check on an identifier
        // assertThat(res.getResponse().getContentAsString()).containsIgnoringCase(TOS_TERMS_ACCEPT);
    }

    @Test
    public void termsPageIsUnavailableWhenDisabled() throws Exception {
        //disable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(false);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get("/-/" + slug + TOS_TERMS);

        // expect error in response
        MvcResult res = this.mockMvc.perform(req).andDo(print()).andExpect(status().is3xxRedirection()).andReturn();

        // expect a redirect in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        //follow redirect and expect tos page
        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).startsWith(ERROR_PAGE);
    }

    private void updateTos(String slug, TosConfigurationMap configMap) throws NoSuchRealmException {
        Realm realm = realmService.findRealm(slug);
        if (realm == null) {
            throw new NoSuchRealmException();
        }

        realmService.updateRealm(
            slug,
            realm.getName(),
            realm.isEditable(),
            realm.isPublic(),
            realm.getOAuthConfiguration().getConfiguration(),
            configMap.getConfiguration(),
            null
        );
    }

    private static final String ERROR_PAGE = "/error";
}
