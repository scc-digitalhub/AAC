package it.smartcommunitylab.aac.test;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.controller.RolesController;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AACRolesControllerTestConfig.class }, loader = AnnotationConfigWebContextLoader.class, initializers = ConfigFileApplicationContextInitializer.class)
public class AACRolesControllerTest {

	private static final String TEST_ROLE = "[\"CONTROLLER_TEST_ROLE\"]";

	@Autowired
	private WebApplicationContext ctx;

	private MockMvc mockMvc;
	
	private final static String TOKEN = "fcd61482-0944-4bc3-bf3f-1aac8acf7d3d";
	private final static String BEARER = "Bearer " + TOKEN;
	
	private final static String USER_ID = "8";

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
	}
	
	@Test
	public void testGetRoles() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/me")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", BEARER);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}
	
	@Test
	public void testGetAllRoles() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/all/user/" + USER_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", BEARER);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}	
	
	@Test
	public void testGetTenantRoles() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/tenant/user/" + USER_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", BEARER);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}	
	
	@Test
	public void testGetClientRoles() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/client")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", BEARER);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}		
	
	@Test
	public void testAddDeleteRoles() throws Exception {
		int size = getRoleSize();
		
		RequestBuilder request = MockMvcRequestBuilders.put("/userroles/user/" + USER_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", BEARER).content(TEST_ROLE);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		int addSize = getRoleSize();
		assertEquals(size + 1, addSize);
		
		request = MockMvcRequestBuilders.delete("/userroles/user/" + USER_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", BEARER).content(TEST_ROLE);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());		

		int deleteSize = getRoleSize();
		assertEquals(size, deleteSize);		
	}	
	
	private int getRoleSize() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		
		RequestBuilder request = MockMvcRequestBuilders.get("/userroles/all/user/" + USER_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", BEARER);
		String result = mockMvc.perform(request).andReturn().getResponse().getContentAsString();
		List resultList = mapper.readValue(result, List.class);
		return resultList.size();
	}
	
}

@TestConfiguration
@EnableWebMvc
@ComponentScan(basePackages = { "it.smartcommunitylab.aac"})
class AACRolesControllerTestConfig {

	@Bean
	public RolesController rolesController() {
		return new RolesController();
	}

	
}
