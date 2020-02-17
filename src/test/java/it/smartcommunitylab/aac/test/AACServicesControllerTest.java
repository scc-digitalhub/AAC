package it.smartcommunitylab.aac.test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

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

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.Config.CLAIM_TYPE;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableConfigurationProperties
@SuppressWarnings("unchecked")
public class AACServicesControllerTest extends OAuth2AwareControllerTest {

    private final static String CLIENT_ID = "servicesclient";
    private final static String USERNAME = "testservices";
    private final static String PASSWORD = "password";
    
	private static final String TESTNAMESPACE = "testnamespace";
	private static final String TEST_SERVICE_ID = "testServiceId";
	private static final String TESTSPACE = "testspace";

	@Override
	protected String getScopes() {
		return super.getScopes() + ",servicemanagement,servicemanagement.me,claimmanagement,claimmanagement.me";
	}

	@Before
	public void setUp() throws Exception {
        super.setUp(CLIENT_ID, USERNAME, PASSWORD);
		addRoleToTestUser("services", TESTSPACE, Config.R_PROVIDER);
	}
	
	@Test
	public void testGetServices() throws Exception {
		RequestBuilder request = MockMvcRequestBuilders.get("/api/services")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		String result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		Map<String, Object> resultPage = mapper.readValue(result, Map.class);
		int core = (int) resultPage.get("totalElements");
		
		request = MockMvcRequestBuilders.get("/api/services")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		request = MockMvcRequestBuilders.get("/api/services")
				.contentType(MediaType.APPLICATION_JSON);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isUnauthorized());
		
		ServiceDTO dto = serviceDef();
		String content = mapper.writeValueAsString(dto);

		// create a single service
		request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

		// check updated list
		request = MockMvcRequestBuilders.get("/api/services")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		resultPage = mapper.readValue(result, Map.class);
		assertEquals(core + 1, resultPage.get("totalElements"));
		
		// check filter
		request = MockMvcRequestBuilders.get("/api/services?name={name}", "name")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		resultPage = mapper.readValue(result, Map.class);
		assertEquals(1, resultPage.get("totalElements"));

		// check filter nonmatching
		request = MockMvcRequestBuilders.get("/api/services?name={name}", "abc")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		resultPage = mapper.readValue(result, Map.class);
		assertEquals(0, resultPage.get("totalElements"));

	}
	
	@Test
	public void testCreateService() throws Exception {
		ServiceDTO dto = serviceDef();
		String content = mapper.writeValueAsString(dto);

		// nominal case
		RequestBuilder request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		String result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		
		ServiceDTO resDto = mapper.readValue(result, ServiceDTO.class);
		assertEquals(dto.getServiceId().toLowerCase(), resDto.getServiceId()); 

		// read single service
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		resDto = mapper.readValue(result, ServiceDTO.class);
		assertEquals(dto.getServiceId().toLowerCase(), resDto.getServiceId()); 

		// incorrect context
		dto = new ServiceDTO();
		dto.setContext(null);
		dto.setServiceId(TEST_SERVICE_ID);
		dto.setNamespace(TESTNAMESPACE);
		dto.setName("name");
		dto.setDescription("description");
		content = mapper.writeValueAsString(dto);
		request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isUnauthorized());

		// duplicate namespace
		dto = new ServiceDTO();
		dto.setContext(TESTSPACE);
		dto.setServiceId(TEST_SERVICE_ID+"1");
		dto.setNamespace(TESTNAMESPACE);
		dto.setName("name");
		dto.setDescription("description");
		content = mapper.writeValueAsString(dto);
		request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isBadRequest());

		// empty namespace
		dto = new ServiceDTO();
		dto.setContext(TESTSPACE);
		dto.setServiceId(TEST_SERVICE_ID+"1");
		dto.setNamespace(null);
		dto.setName("name");
		dto.setDescription("description");
		content = mapper.writeValueAsString(dto);
		request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isBadRequest());

		// empty name
		dto = new ServiceDTO();
		dto.setContext(TESTSPACE);
		dto.setServiceId(TEST_SERVICE_ID+"1");
		dto.setNamespace(TESTNAMESPACE +"1");
		dto.setName(null);
		dto.setDescription("description");
		content = mapper.writeValueAsString(dto);
		request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isBadRequest());

		// empty description
		dto = new ServiceDTO();
		dto.setContext(TESTSPACE);
		dto.setServiceId(TEST_SERVICE_ID+"1");
		dto.setNamespace(TESTNAMESPACE +"1");
		dto.setName("name");
		dto.setDescription(null);
		content = mapper.writeValueAsString(dto);
		request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", token);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isBadRequest());

	}

	@Test
	public void testDeleteService() throws Exception {
		ServiceDTO dto = serviceDef();
		String content = mapper.writeValueAsString(dto);

		// nominal case
		RequestBuilder request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

		request = MockMvcRequestBuilders.delete("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

		// read single service
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		
		request = MockMvcRequestBuilders.delete("/api/services/{serviceId}", "smartcommunity.orgmanagement")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isUnauthorized());
		
	}	
	

	@Test
	public void testScope() throws Exception {
		ServiceDTO dto = serviceDef();
		String content = mapper.writeValueAsString(dto);

		// create service
		RequestBuilder request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		ServiceScopeDTO scope = new ServiceScopeDTO();
		scope.setApprovalRequired(true);
		scope.setAuthority(AUTHORITY.ROLE_USER);
		scope.setClaims(Collections.singletonList("testclaim"));
		scope.setDescription("scope descr");
		scope.setName("scope name");
		scope.setScope("scope");
		scope.setServiceId(TEST_SERVICE_ID);
		content = mapper.writeValueAsString(scope);
		
		request = MockMvcRequestBuilders.put("/api/services/{serviceId}/scope", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		// read single service
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		String result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		
		ServiceDTO update = mapper.readValue(result, ServiceDTO.class);
		assertEquals("scope", update.getScopes().get(0).getScope());

		// update
		scope = new ServiceScopeDTO();
		scope.setApprovalRequired(true);
		scope.setAuthority(AUTHORITY.ROLE_USER);
		scope.setClaims(Collections.singletonList("testclaim"));
		scope.setDescription("scope descr");
		scope.setName("scope name");
		scope.setScope("scope1");
		scope.setServiceId(TEST_SERVICE_ID);
		content = mapper.writeValueAsString(scope);
		request = MockMvcRequestBuilders.put("/api/services/{serviceId}/scope", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

		// read single service
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		
		update = mapper.readValue(result, ServiceDTO.class);
		assertEquals(2, update.getScopes().size());

		// delete
		request = MockMvcRequestBuilders.delete("/api/services/{serviceId}/scope/{scope}", TEST_SERVICE_ID, "scope")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();

		
		update = mapper.readValue(result, ServiceDTO.class);
		assertEquals(1, update.getScopes().size());


	}	
	
	
	@Test
	public void testClaim() throws Exception {
		ServiceDTO dto = serviceDef();
		String content = mapper.writeValueAsString(dto);

		// create service
		RequestBuilder request = MockMvcRequestBuilders.post("/api/services")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		ServiceClaimDTO claim = new ServiceClaimDTO();
		claim.setName("claim name");
		claim.setClaim("claim");
		claim.setMultiple(true);
		claim.setType(CLAIM_TYPE.type_object.getLitType());
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
		
		ServiceDTO update = mapper.readValue(result, ServiceDTO.class);
		assertEquals("claim", update.getClaims().get(0).getClaim());
		Long id = update.getClaims().get(0).getClaimId();

		// update
		claim = new ServiceClaimDTO();
		claim.setName("claim name");
		claim.setClaim("claim1");
		claim.setClaimId(id);
		claim.setMultiple(true);
		claim.setType(CLAIM_TYPE.type_object.getLitType());
		claim.setServiceId(TEST_SERVICE_ID);
		content = mapper.writeValueAsString(claim);
		
		
		request = MockMvcRequestBuilders.put("/api/services/{serviceId}/claim", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

		// read single service
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		
		update = mapper.readValue(result, ServiceDTO.class);
		assertEquals(1, update.getClaims().size());

		// delete (empty)
		request = MockMvcRequestBuilders.delete("/api/services/{serviceId}/claim/{claim}", TEST_SERVICE_ID, "claim")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();

		
		update = mapper.readValue(result, ServiceDTO.class);
		assertEquals(1, update.getClaims().size());

		// delete
		request = MockMvcRequestBuilders.delete("/api/services/{serviceId}/claim/{claim}", TEST_SERVICE_ID, "claim1")
				.contentType(MediaType.APPLICATION_JSON).content(content).header("Authorization", userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		request = MockMvcRequestBuilders.get("/api/services/{serviceId}", TEST_SERVICE_ID)
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", userToken);
		result = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();

		
		update = mapper.readValue(result, ServiceDTO.class);
		assertEquals(0, update.getClaims().size());

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
