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
import static it.smartcommunitylab.aac.tos.controller.TosController.TOS_TERMS_ACCEPT;
import static it.smartcommunitylab.aac.tos.controller.TosController.TOS_TERMS_REJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.smartcommunitylab.aac.auth.WithMockUserAuthentication;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.model.TosConfigurationMap;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class TosUserTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapConfig config;

    @Autowired
    private RealmService realmService;

    @Autowired
    private UserService userService;

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
        //make sure user has NOT approved
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void whenEnabledUserIsRedirectedToTos() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Subject subject = (Subject) authentication.getPrincipal();
        String userId = subject.getSubjectId();

        //reset tos on user
        updateUserTos(userId, null);

        //enable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(true);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(TEST_URL);
        MvcResult res = this.mockMvc.perform(req).andExpect(status().is3xxRedirection()).andReturn();

        // expect a redirect in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        //follow redirect and expect tos page
        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo(TOS_TERMS);

        // keep the same session for the whole request flow
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession();
        assertThat(session).isNotNull();

        // follow redirect to fetch response
        req = MockMvcRequestBuilders.get(redirectedUrl).session(session);
        res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        // page should contain tos action
        assertThat(res.getResponse().getContentAsString()).containsIgnoringCase(TOS_TERMS_ACCEPT);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void whenDisabledUserIsNotRedirectedToTos() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Subject subject = (Subject) authentication.getPrincipal();
        String userId = subject.getSubjectId();

        //reset tos on user
        updateUserTos(userId, null);

        //disable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(false);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(TEST_URL);

        // expect not a redirect in response
        this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void whenApprovedUserIsNotRedirectedToTos() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Subject subject = (Subject) authentication.getPrincipal();
        String userId = subject.getSubjectId();

        //accept tos on user
        updateUserTos(userId, true);

        //enable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(true);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(TEST_URL);

        // expect not a redirect in response
        this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void whenRejectedUserIsRedirectedToTos() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Subject subject = (Subject) authentication.getPrincipal();
        String userId = subject.getSubjectId();

        //reset tos on user
        updateUserTos(userId, false);

        //enable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(true);
        configMap.setApproveTOS(true);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(TEST_URL);
        MvcResult res = this.mockMvc.perform(req).andExpect(status().is3xxRedirection()).andReturn();

        // expect a redirect in response
        assertThat(res.getResponse().getContentAsString()).isBlank();

        //follow redirect and expect tos page
        String redirectedUrl = res.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo(TOS_TERMS_REJECT);
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void whenApprovalShowTosWithApproval() throws Exception {
        //enable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(true);
        configMap.setApproveTOS(true);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(TOS_TERMS);
        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        // expect valid response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        // page should contain tos action and form
        assertThat(res.getResponse().getContentAsString()).containsIgnoringCase(TOS_TERMS_ACCEPT);
        assertThat(res.getResponse().getContentAsString()).containsIgnoringCase("reject");
    }

    @Test
    @WithMockUserAuthentication(username = "test", realm = "test")
    public void whenNoApprovalShowTosWithOk() throws Exception {
        //enable tos
        TosConfigurationMap configMap = new TosConfigurationMap();
        configMap.setEnableTOS(true);
        configMap.setApproveTOS(false);
        updateTos(slug, configMap);

        //mock request
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(TOS_TERMS);
        MvcResult res = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();

        // expect valid response
        assertThat(res.getResponse().getContentAsString()).isNotBlank();

        // page should contain tos action and form
        assertThat(res.getResponse().getContentAsString()).containsIgnoringCase(TOS_TERMS_ACCEPT);
        assertThat(res.getResponse().getContentAsString()).doesNotContain("reject");
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
            configMap.getConfiguration()
        );
    }

    private void updateUserTos(String userId, Boolean tos) throws NoSuchUserException {
        if (tos == null) {
            userService.resetTos(userId);
        } else if (tos.booleanValue() == true) {
            userService.acceptTos(userId);
        } else if (tos.booleanValue() == false) {
            userService.rejectTos(userId);
        }
    }

    //TODO replace with a proper call
    private static final String TEST_URL = "/whoami";
}
