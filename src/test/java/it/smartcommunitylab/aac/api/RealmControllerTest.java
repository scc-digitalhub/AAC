package it.smartcommunitylab.aac.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.api.scopes.AdminRealmsScope;
import it.smartcommunitylab.aac.api.scopes.ApiRealmScope;
import it.smartcommunitylab.aac.auth.WithMockBearerTokenAuthentication;
import it.smartcommunitylab.aac.bootstrap.AACBootstrap;
import it.smartcommunitylab.aac.core.persistence.RealmEntity;
import it.smartcommunitylab.aac.core.persistence.RealmEntityRepository;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
//@Rollback(false)
@ActiveProfiles("test")
class RealmControllerTest {

    private static final String ENDPOINT = "/api/realm";

    @Autowired
    private MockMvc mockMvc;

    /*
     * Mock bootstrap to avoid execution
     */
    @MockBean
    private AACBootstrap bootstrap;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SmartValidator validator;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RealmEntityRepository realmEntityRepository;

    private final TypeReference<List<Realm>> typeRef = new TypeReference<List<Realm>>() {};

    // getRealms
    @Test
    public void getRealmsIsProtected() throws Exception {
        MvcResult res = mockMvc.perform(get(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        // @SpringBootTest and @AutoConfigureMockMvc errors not mocked
//                .andExpect(jsonPath("$.status", is(403)))
//                .andExpect(jsonPath("$.error", is("Forbidden")))
//                .andExpect(jsonPath("$.message", is("Access Denied")))
//                .andExpect(jsonPath("$.path", is(ENDPOINT)));

        // expect a 403 with a blank response from authorization
        assertThat(res.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE })
    public void getRealmsAsUserFails() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));
    }

    @Test
    @WithMockBearerTokenAuthentication(authorities = { Config.R_ADMIN })
    public void getRealmsAsAdminWithNoScopeFails() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void getRealmsAsAdminWithScope() throws Exception {

        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test_1");
        realmEntity.setName("name_test_1");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test_2");
        realmEntity.setName("name_test_2");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        MvcResult res = mockMvc.perform(get(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a list of realm entities
        assertDoesNotThrow(() -> {
            mapper.readValue(response, typeRef);
        });

        // list contains 3 realms
        List<Realm> realms = mapper.readValue(response, typeRef);
        assertEquals(3, realms.size());

        // each realm slug is not blank
        realms.forEach(r -> {
            assertThat(r.getSlug()).isNotBlank();
        });
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void getRealmsAsAdminWithScopeWithQuery() throws Exception {

        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test_1");
        realmEntity.setName("name_test_1");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test_2");
        realmEntity.setName("name_test_2");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        MvcResult res = mockMvc.perform(get(ENDPOINT)
                        .param("q", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a list of realm entities
        assertDoesNotThrow(() -> {
            mapper.readValue(response, typeRef);
        });

        // list contains 2 realms
        List<Realm> realms = mapper.readValue(response, typeRef);
        assertEquals(2, realms.size());

        // each realm slug is not blank
        realms.forEach(r -> {
            assertThat(r.getSlug()).isNotBlank();
        });
    }


    // getRealm
    @Test
    public void getRealmIsProtected() throws Exception {
        MvcResult res = mockMvc.perform(get(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with a blank response from authorization
        assertThat(res.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE })
    public void getRealmAsUserWithAdminScopeFails() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/system")));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE })
    public void getRealmAsUserWithApiScopeFails() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/system")));
    }

    @Test
    @WithMockBearerTokenAuthentication(authorities = { Config.R_ADMIN })
    public void getRealmAsAdminWithNoScopeFails() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/system")));
    }

    // TODO: #realm+':ROLE_ADMIN'
    // admin of realm1 tries to get realm2 -> should fail
    // admin of realm1 tries to get realm1 -> should not fail (?)

//    @Test
//    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE }, authorities = { Config.R_ADMIN }, realm = "slug_test")
//    public void getRealmSystemAsRealmAdminWithApiScopeFails() throws Exception {
//        mockMvc.perform(get(ENDPOINT + "/system")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk());
//    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void getRealmSystemAsAdminWithApiScope() throws Exception {
        MvcResult res = mockMvc.perform(get(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void getRealmCustomAsAdminWithApiScope() throws Exception {
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        MvcResult res = mockMvc.perform(get(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void getRealmSystemAsAdminWithAdminScopeAndCheckSystemRealmExists() throws Exception {
        MvcResult res = mockMvc.perform(get(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });

        // realm entity is valid
        Realm realm = mapper.readValue(response, Realm.class);
        DataBinder binder = new DataBinder(realm);
        validator.validate(realm, binder.getBindingResult());
        assertFalse(binder.getBindingResult().hasErrors());

        // realm is as expected
        assertEquals("system", realm.getSlug());
        assertEquals("system", realm.getName());
        assertTrue(realm.getCustomization().isEmpty());
        assertFalse(realm.getOAuthConfiguration().getEnableClientRegistration());
        assertFalse(realm.getOAuthConfiguration().getOpenClientRegistration());
        assertFalse(realm.isPublic());
        assertFalse(realm.isEditable());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void getRealmCustomAsAdminWithAdminScopeAndCheckCustomRealmExists() throws Exception {
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        MvcResult res = mockMvc.perform(get(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });

        // realm entity is valid
        Realm realm = mapper.readValue(response, Realm.class);
        DataBinder binder = new DataBinder(realm);
        validator.validate(realm, binder.getBindingResult());
        assertFalse(binder.getBindingResult().hasErrors());

        // realm is as expected
        assertEquals("slug_test", realm.getSlug());
        assertEquals("name_test", realm.getName());
        assertTrue(realm.getCustomization().isEmpty());
        assertFalse(realm.getOAuthConfiguration().getEnableClientRegistration());
        assertFalse(realm.getOAuthConfiguration().getOpenClientRegistration());
        assertTrue(realm.isPublic());
        assertTrue(realm.isEditable());
    }

    // addRealm
    @Test
    void addRealmIsProtected() throws Exception {
        Realm realm = new Realm("slug_test", "name_test");

        MvcResult res = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with no response from spring security
        assertThat(res.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE })
    public void addRealmAsUserFails() throws Exception {
        Realm realm = new Realm("slug_test", "name_test");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));
    }

    @Test
    @WithMockBearerTokenAuthentication(authorities = { Config.R_ADMIN })
    public void addRealmAsAdminWithNoScopeFails() throws Exception {
        Realm realm = new Realm("slug_test", "name_test");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void addRealmAsAdminWithScope() throws Exception {
        Realm realm = new Realm("slug_test", "name_test");

        MvcResult res = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });

        // realm entity is valid
        Realm realmRes = mapper.readValue(response, Realm.class);
        DataBinder binder = new DataBinder(realmRes);
        validator.validate(realmRes, binder.getBindingResult());
        assertFalse(binder.getBindingResult().hasErrors());

        // realm is as expected
        assertEquals(realm.getSlug(), realmRes.getSlug());
        assertEquals(realm.getName(), realmRes.getName());
        assertEquals(realm.getCustomization(), realmRes.getCustomization());
        assertEquals(realm.getOAuthConfiguration().getEnableClientRegistration(), realmRes.getOAuthConfiguration().getEnableClientRegistration());
        assertEquals(realm.getOAuthConfiguration().getOpenClientRegistration(), realmRes.getOAuthConfiguration().getOpenClientRegistration());
        assertEquals(realm.isPublic(), realmRes.isPublic());
        assertEquals(realm.isEditable(), realmRes.isEditable());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void addRealmAndCheckAddedRealmExists() throws Exception {
        Realm realm = new Realm("slug_test", "name_test");

        // realm does not already exist
        assertNull(realmEntityRepository.findBySlug("slug_test"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // realm is found
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void addRealmWithAlreadyExistingSlugFails() throws Exception {
        Realm realm = new Realm("slug_test", "name_test");

        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm already exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("error.already_registered")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void addRealmWithEmptySlugFails() throws Exception {
        Realm realm = new Realm("", "name_test");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));

        // realm is not added
        assertNull(realmEntityRepository.findBySlug(""));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void addRealmWithEmptyNameFails() throws Exception {
        Realm realm = new Realm("slug_test", "");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));

        // realm is not added
        assertNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void addRealmWithShortSlugFails() throws Exception {
        Realm realm = new Realm("s", "");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is(ENDPOINT)));

        // realm is not added
        assertNull(realmEntityRepository.findBySlug("s"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void addRealmWithCapitalSlugConverts() throws Exception {
        Realm realm = new Realm("slug_Test", "name_test");

        MvcResult res = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });

        // realm entity is valid
        Realm realmRes = mapper.readValue(response, Realm.class);
        DataBinder binder = new DataBinder(realmRes);
        validator.validate(realmRes, binder.getBindingResult());
        assertFalse(binder.getBindingResult().hasErrors());

        // realm is as expected
        assertEquals("slug_test", realmRes.getSlug());

        // realm is found
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));
    }

    // updateRealm
    @Test
    void updateRealmIsProtected() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("name_test_update");
        realm.setEditable(false);
        realm.setPublic(false);

        MvcResult res = mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with no response from spring security
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // realm is not updated
        assertEquals(realmEntity.getName(), realmEntityRepository.findBySlug("slug_test").getName());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE })
    public void updateRealmAsUserWithAdminScopeFails() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("name_test_update");
        realm.setEditable(false);
        realm.setPublic(false);

        mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not updated
        assertEquals(realmEntity.getName(), realmEntityRepository.findBySlug("slug_test").getName());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE })
    public void updateRealmAsUserWithApiScopeFails() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("name_test_update");
        realm.setEditable(false);
        realm.setPublic(false);

        mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not updated
        assertEquals(realmEntity.getName(), realmEntityRepository.findBySlug("slug_test").getName());
    }

    @Test
    @WithMockBearerTokenAuthentication(authorities = { Config.R_ADMIN })
    public void updateRealmAsAdminWithNoScopeFails() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("name_test_update");
        realm.setEditable(false);
        realm.setPublic(false);

        mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not updated
        assertEquals(realmEntity.getName(), realmEntityRepository.findBySlug("slug_test").getName());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void updateRealmAsAdminWithAdminScope() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("name_test_update");
        realm.setEditable(false);
        realm.setPublic(false);

        MvcResult res = mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });

        // realm entity is valid
        Realm realmRes = mapper.readValue(response, Realm.class);
        DataBinder binder = new DataBinder(realmRes);
        validator.validate(realmRes, binder.getBindingResult());
        assertFalse(binder.getBindingResult().hasErrors());

        // realm is as expected
        assertEquals(realm.getSlug(), realmRes.getSlug());
        assertEquals(realm.getName(), realmRes.getName());
        assertEquals(realm.getCustomization(), realmRes.getCustomization());
        assertEquals(realm.getOAuthConfiguration().getEnableClientRegistration(), realmRes.getOAuthConfiguration().getEnableClientRegistration());
        assertEquals(realm.getOAuthConfiguration().getOpenClientRegistration(), realmRes.getOAuthConfiguration().getOpenClientRegistration());
        assertEquals(realm.isPublic(), realmRes.isPublic());
        assertEquals(realm.isEditable(), realmRes.isEditable());

        // realm is updated
        assertEquals(realm.getName(), realmEntityRepository.findBySlug("slug_test").getName());
        assertEquals(realm.isEditable(), realmEntityRepository.findBySlug("slug_test").isEditable());
        assertEquals(realm.isPublic(), realmEntityRepository.findBySlug("slug_test").isPublic());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void updateRealmAsAdminWithApiScope() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("name_test_update");
        realm.setEditable(false);
        realm.setPublic(false);

        MvcResult res = mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // response is not blank
        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        // response contains a single realm entity
        assertDoesNotThrow(() -> {
            mapper.readValue(response, Realm.class);
        });

        // realm entity is valid
        Realm realmRes = mapper.readValue(response, Realm.class);
        DataBinder binder = new DataBinder(realmRes);
        validator.validate(realmRes, binder.getBindingResult());
        assertFalse(binder.getBindingResult().hasErrors());

        // realm is as expected
        assertEquals(realm.getSlug(), realmRes.getSlug());
        assertEquals(realm.getName(), realmRes.getName());
        assertEquals(realm.getCustomization(), realmRes.getCustomization());
        assertEquals(realm.getOAuthConfiguration().getEnableClientRegistration(), realmRes.getOAuthConfiguration().getEnableClientRegistration());
        assertEquals(realm.getOAuthConfiguration().getOpenClientRegistration(), realmRes.getOAuthConfiguration().getOpenClientRegistration());
        assertEquals(realm.isPublic(), realmRes.isPublic());
        assertEquals(realm.isEditable(), realmRes.isEditable());

        // realm is updated
        assertEquals(realm.getName(), realmEntityRepository.findBySlug("slug_test").getName());
        assertEquals(realm.isEditable(), realmEntityRepository.findBySlug("slug_test").isEditable());
        assertEquals(realm.isPublic(), realmEntityRepository.findBySlug("slug_test").isPublic());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void updateRealmDoesNotExistFails() throws Exception {
        // realm does not exist
        assertNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("name_test_update");
        realm.setEditable(false);
        realm.setPublic(false);

        mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not added
        assertNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void updateRealmEmptyNameFails() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        // updated realm
        Realm realm = new Realm();
        realm.setSlug("slug_test");
        realm.setName("");
        realm.setEditable(false);
        realm.setPublic(false);

        mockMvc.perform(put(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not updated
        assertEquals(realmEntity.getName(), realmEntityRepository.findBySlug("slug_test").getName());
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void updateRealmSystemFails() throws Exception {
        // updated realm
        Realm realm = new Realm();
        realm.setSlug("system");
        realm.setName("system_test");
        realm.setEditable(true);
        realm.setPublic(true);

        mockMvc.perform(put(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(realm)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/system")));

        // realm is not updated
        assertEquals("system", realmEntityRepository.findBySlug("system").getName());
        assertFalse(realmEntityRepository.findBySlug("system").isEditable());
        assertFalse(realmEntityRepository.findBySlug("system").isPublic());
    }

    // TODO: #realm+':ROLE_ADMIN'

    // deleteRealm
    @Test
    public void deleteRealmIsProtected() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        MvcResult res = mockMvc.perform(delete(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with a blank response from authorization
        assertThat(res.getResponse().getContentAsString()).isBlank();

        // realm is not deleted
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE })
    public void deleteRealmAsUserWithAdminScopeFails() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        mockMvc.perform(delete(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not deleted
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE })
    public void deleteRealmAsUserWithApiScopeFails() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        mockMvc.perform(delete(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not deleted
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(authorities = { Config.R_ADMIN })
    public void deleteRealmAsAdminWithNoScopeFails() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        mockMvc.perform(delete(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied")))
                .andExpect(jsonPath("$.description", is("Access is denied")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/slug_test")));

        // realm is not deleted
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));
    }

    // TODO: #realm+':ROLE_ADMIN'

    @Test
    @WithMockBearerTokenAuthentication(scopes = { ApiRealmScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void deleteRealmAsAdminWithApiScope() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        mockMvc.perform(delete(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        // realm is deleted
        assertNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void deleteRealmAsAdminWithAdminScope() throws Exception {
        // add realm to database
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setSlug("slug_test");
        realmEntity.setName("name_test");
        realmEntity.setEditable(true);
        realmEntity.setPublic(true);

        entityManager.persist(realmEntity);
        entityManager.flush();

        // realm exists
        assertNotNull(realmEntityRepository.findBySlug("slug_test"));

        mockMvc.perform(delete(ENDPOINT + "/slug_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        // realm is deleted
        assertNull(realmEntityRepository.findBySlug("slug_test"));
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void deleteRealmSystemFails() throws Exception {
        mockMvc.perform(delete(ENDPOINT + "/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is(ENDPOINT + "/system")));

        // realm is not deleted
        assertNotNull(realmEntityRepository.findBySlug("system"));
    }

    // TODO: cleanup true
}