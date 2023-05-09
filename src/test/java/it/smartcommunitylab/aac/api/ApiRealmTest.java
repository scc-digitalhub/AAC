package it.smartcommunitylab.aac.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.api.scopes.AdminRealmsScope;
import it.smartcommunitylab.aac.auth.WithMockBearerTokenAuthentication;
import it.smartcommunitylab.aac.bootstrap.AACBootstrap;
import it.smartcommunitylab.aac.model.Realm;

@SpringJUnitWebConfig
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApiRealmTest {

    private static final String ENDPOINT = "/api/realms";

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

    @Test
    public void listRealmsIsProtected() throws Exception {
        MvcResult res = this.mockMvc
                .perform(
                        get(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with no response from spring security
        assertThat(res.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE })
    public void listRealmsAsUserFails() throws Exception {
        MvcResult res = this.mockMvc
                .perform(
                        get(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with a response from authorization
        assertThat(res.getResponse().getContentAsString()).isNotBlank();
    }

    @Test
    @WithMockBearerTokenAuthentication(authorities = { Config.R_ADMIN })
    public void listRealmsAsAdminWithNoScopeFails() throws Exception {
        MvcResult res = this.mockMvc
                .perform(
                        get(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        // expect a 403 with a response from authorization
        assertThat(res.getResponse().getContentAsString()).isNotBlank();
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void listRealmsAsAdminWithScope() throws Exception {
        MvcResult res = this.mockMvc
                .perform(
                        get(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = res.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        assertDoesNotThrow(() -> {
            List<Realm> realms = mapper.readValue(response, typeRef);

            realms.forEach(r -> {
                assertThat(r.getSlug()).isNotBlank();
            });
        });
    }

    @Test
    @WithMockBearerTokenAuthentication(scopes = { AdminRealmsScope.SCOPE }, authorities = { Config.R_ADMIN })
    public void checkSystemRealmExists() throws Exception {
        MvcResult res = this.mockMvc
                .perform(
                        get(ENDPOINT + "/system")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
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

        // realm slug is as expected
        assertEquals("system", realm.getSlug());
    }

    private final TypeReference<List<Realm>> typeRef = new TypeReference<List<Realm>>() {
    };
}
