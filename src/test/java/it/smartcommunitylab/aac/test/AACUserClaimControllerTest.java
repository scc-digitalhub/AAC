package it.smartcommunitylab.aac.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.CLAIM_TYPE;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.dto.UserClaimProfileDTO;
import it.smartcommunitylab.aac.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableConfigurationProperties
@SuppressWarnings("unchecked")
public class AACUserClaimControllerTest extends OAuth2AwareControllerTest {

	/**
	 * 
	 */
	private static final String TEST_USER_2 = "abc@def";
	private static final String TESTNAMESPACE = "testnamespace";
	private static final String TEST_SERVICE_ID = "testServiceId";
	private static final String TESTSPACE = "testspace";

	@Override
	protected String getScopes() {
		return super.getScopes() + ",servicemanagement,servicemanagement.me,claimmanagement,claimmanagement.me";
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		addRoleToTestUser("services", TESTSPACE, Config.R_PROVIDER);
		prepareService(); 
	}
	
	@After
	public void tearDown() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken))
		.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}
	
	@Test
	public void testClaimsExistingUser() throws Exception {
		String content;
		RequestBuilder request;
		String result;

		// read claims
		request = MockMvcRequestBuilders.get("/api/claims/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		Map<String, Object> resultPage = mapper.readValue(result, Map.class);
		int total = (int) resultPage.get("totalElements");
		assertEquals(0, total);

		// add claim
		UserClaimProfileDTO 
		profile = new UserClaimProfileDTO();
		profile.setClaims(Collections.singletonMap(TESTNAMESPACE+"/claim_number", 1));
		content = mapper.writeValueAsString(profile);
		request = MockMvcRequestBuilders.post("/api/claims/{serviceId}/{userId}", TEST_SERVICE_ID, user.getId())
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		
		UserClaimProfileDTO 
		res = mapper.readValue(result, UserClaimProfileDTO.class);
		assertNotNull(res);
		assertEquals(1, res.getClaims().get(TESTNAMESPACE+"/claim_number"));

		// read claims
		request = MockMvcRequestBuilders.get("/api/claims/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		resultPage = mapper.readValue(result, Map.class);
		total = (int) resultPage.get("totalElements");
		assertEquals(1, total);
		
	}

	
	@Test
	public void testClaimsNonExistingUser() throws Exception {
		String content;
		RequestBuilder request;
		String result;
		
		// read claims
		request = MockMvcRequestBuilders.get("/api/claims/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		Map<String, Object> resultPage = mapper.readValue(result, Map.class);
		int total = (int) resultPage.get("totalElements");
		assertEquals(0, total);

		// add claim
		UserClaimProfileDTO 
		profile = new UserClaimProfileDTO();
		profile.setClaims(Collections.singletonMap(TESTNAMESPACE+"/claim_number", 1));
		content = mapper.writeValueAsString(profile);
		request = MockMvcRequestBuilders.post("/api/claims/{serviceId}/username?username={username}", TEST_SERVICE_ID, TEST_USER_2)
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		
		UserClaimProfileDTO 
		res = mapper.readValue(result, UserClaimProfileDTO.class);
		assertNotNull(res);
		assertEquals(1, res.getClaims().get(TESTNAMESPACE+"/claim_number"));

		// read claims
		request = MockMvcRequestBuilders.get("/api/claims/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		resultPage = mapper.readValue(result, Map.class);
		total = (int) resultPage.get("totalElements");
		assertEquals(1, total);

		// read by username
		request = MockMvcRequestBuilders.get("/api/claims/{serviceId}/username?username={username}", TEST_SERVICE_ID, TEST_USER_2)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		res = mapper.readValue(result, UserClaimProfileDTO.class);
		assertNotNull(res);
		assertEquals(1, res.getClaims().get(TESTNAMESPACE+"/claim_number"));

		// TODO complete: register user and read by id
		// register user
		User user2 = registrationManager.registerOffline("NAME", "SURNAME", TEST_USER_2, "password", null, false, null);
		request = MockMvcRequestBuilders.get("/api/claims/{serviceId}/{userId}", TEST_SERVICE_ID, user2.getId())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		res = mapper.readValue(result, UserClaimProfileDTO.class);
		assertNotNull(res);
		assertEquals(1, res.getClaims().get(TESTNAMESPACE+"/claim_number"));

		
	}

	protected void prepareService() throws JsonProcessingException, Exception, UnsupportedEncodingException,
			IOException, JsonParseException, JsonMappingException {
		ServiceDTO dto = serviceDef();
		String content = mapper.writeValueAsString(dto);

		// create service
		RequestBuilder request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());


		// create claim1: object array
		ServiceClaimDTO claim = new ServiceClaimDTO();
		claim.setName("claim name");
		claim.setClaim("claim_obj_array");
		claim.setMultiple(true);
		claim.setType(CLAIM_TYPE.type_object.getLitType());
		claim.setServiceId(TEST_SERVICE_ID);
		content = mapper.writeValueAsString(claim);
		request = MockMvcRequestBuilders.put("/api/services/{serviceId}/claim", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());


		// create claim2: object
		claim = new ServiceClaimDTO();
		claim.setName("claim name");
		claim.setClaim("claim_obj");
		claim.setMultiple(false);
		claim.setType(CLAIM_TYPE.type_object.getLitType());
		claim.setServiceId(TEST_SERVICE_ID);
		content = mapper.writeValueAsString(claim);
		request = MockMvcRequestBuilders.put("/api/services/{serviceId}/claim", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

		
		// create claim3: number
		claim = new ServiceClaimDTO();
		claim.setName("claim name");
		claim.setClaim("claim_number");
		claim.setMultiple(false);
		claim.setType(CLAIM_TYPE.type_number.getLitType());
		claim.setServiceId(TEST_SERVICE_ID);
		content = mapper.writeValueAsString(claim);
		request = MockMvcRequestBuilders.put("/api/services/{serviceId}/claim", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());


		// read single service
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		String result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		ServiceDTO resDto = mapper.readValue(result, ServiceDTO.class);
		assertEquals(3, resDto.getClaims().size());
	}	
	
	
	
	protected ServiceDTO serviceDef() {
		ServiceDTO dto = new ServiceDTO();
		dto.setContext(TESTSPACE);
		dto.setServiceId(TEST_SERVICE_ID);
		dto.setNamespace(TESTNAMESPACE);
		dto.setName("name");
		dto.setDescription("description");
		return dto;
	}	

}
