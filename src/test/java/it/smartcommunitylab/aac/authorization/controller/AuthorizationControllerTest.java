package it.smartcommunitylab.aac.authorization.controller;

import java.util.Arrays;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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

import it.smartcommunitylab.aac.authorization.AuthorizationHelper;
import it.smartcommunitylab.aac.authorization.AuthorizationSchemaHelper;
import it.smartcommunitylab.aac.authorization.NotValidResourceException;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeValueDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationResourceDTO;
import it.smartcommunitylab.aac.authorization.model.Authorization;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNode;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeAlreadyExist;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeValue;
import it.smartcommunitylab.aac.authorization.model.AuthorizationUser;
import it.smartcommunitylab.aac.authorization.model.FQname;
import it.smartcommunitylab.aac.authorization.model.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AuthorizationControllerTestConfig.class }, loader = AnnotationConfigWebContextLoader.class)
public class AuthorizationControllerTest {

	@Autowired
	private WebApplicationContext ctx;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
	}

	@Test
	public void addRootChild() throws Exception {
		ObjectMapper jsonMapper = new ObjectMapper();
		AuthorizationNode node = new AuthorizationNode(new FQname("domain", "A"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/schema")
				.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(node));
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void addChildAuthorizationNode() throws Exception {
		ObjectMapper jsonMapper = new ObjectMapper();
		AuthorizationNode node = new AuthorizationNode(new FQname("domain", "A"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/schema/parent-qname")
				.content(jsonMapper.writeValueAsString(node)).contentType(MediaType.APPLICATION_JSON);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void getSchema() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/authorization/domain/schema/qname-node");
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void removeAuthorization() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.delete("/authorization/domain/my-auth");
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void insertAuthorization() throws Exception {
		ObjectMapper jsonMapper = new ObjectMapper();
		Resource resource = new Resource(new FQname("domain", "A"),
				Arrays.asList(new AuthorizationNodeValue("A", "a", "a_value")));
		Authorization auth = new Authorization(new AuthorizationUser("subject", "type"), "action", resource,
				new AuthorizationUser("entity", "type"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain")
				.content(jsonMapper.writeValueAsString(auth)).contentType(MediaType.APPLICATION_JSON);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void validateAuthorization() throws Exception {
		ObjectMapper jsonMapper = new ObjectMapper();
		Resource resource = new Resource(new FQname("domain", "A"),
				Arrays.asList(new AuthorizationNodeValue("A", "a", "a_value")));
		Authorization auth = new Authorization(new AuthorizationUser("subject", "type"), "action", resource,
				new AuthorizationUser("entity", "type"));
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/validate")
				.content(jsonMapper.writeValueAsString(auth)).contentType(MediaType.APPLICATION_JSON);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andExpect(MockMvcResultMatchers.content().string("false"));
	}

	@Test
	public void validateResource() throws Exception {
		AuthorizationNodeValueDTO nodeValue = new AuthorizationNodeValueDTO();
		nodeValue.setName("a");
		nodeValue.setQname("A");
		nodeValue.setValue("a_value");
		AuthorizationResourceDTO resource = new AuthorizationResourceDTO();
		resource.setQnameRef("A");
		resource.setValues(Arrays.asList(nodeValue));
		ObjectMapper jsonMapper = new ObjectMapper();
		RequestBuilder request = MockMvcRequestBuilders.post("/authorization/domain/schema/validate")
				.content(jsonMapper.writeValueAsString(resource)).contentType(MediaType.APPLICATION_JSON);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}
}

@TestConfiguration
@EnableWebMvc // without it mockMvc post thrown an HTTP 415 ERROR
class AuthorizationControllerTestConfig {

	@Bean
	public AuthorizationController authorizationController() {
		return new AuthorizationController();
	}

	@Bean
	public AuthorizationHelper authorizationHelper() {
		return new AuthorizationHelper() {

			@Override
			public boolean validate(Authorization auth) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void remove(Authorization auth) {
				// TODO Auto-generated method stub

			}

			@Override
			public Authorization insert(Authorization auth) throws NotValidResourceException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void remove(String authorizationId) {
				// TODO Auto-generated method stub

			}
		};
	}

	@Bean
	public AuthorizationSchemaHelper authorizationSchemaHelper() {
		return new AuthorizationSchemaHelper() {

			@Override
			public boolean isValid(Resource res) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public AuthorizationNode getNode(FQname qname) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getChildren(AuthorizationNode node) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getAllChildren(AuthorizationNode node) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public AuthorizationSchemaHelper addRootChild(AuthorizationNode child)
					throws AuthorizationNodeAlreadyExist {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public AuthorizationSchemaHelper addChild(AuthorizationNode parent, AuthorizationNode child)
					throws AuthorizationNodeAlreadyExist {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public AuthorizationSchemaHelper addChild(FQname parentQname, AuthorizationNode child)
					throws AuthorizationNodeAlreadyExist {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getChildren(FQname qName) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<AuthorizationNode> getAllChildren(FQname qname) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

}
