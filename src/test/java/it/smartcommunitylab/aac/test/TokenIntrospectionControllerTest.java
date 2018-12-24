package it.smartcommunitylab.aac.test;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import it.smartcommunitylab.aac.dto.AACTokenIntrospection;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AACTokenIntrospectionControllerTestConfig.class }, loader = AnnotationConfigWebContextLoader.class, initializers = ConfigFileApplicationContextInitializer.class)
public class TokenIntrospectionControllerTest extends OAuth2AwareControllerTest {

	@Before
	public void setUp() throws Exception {
		super.setUp();
	}
	
	@Test
	public void testAddDeleteRoles() throws Exception {
		// authentication required, should fail 401
		RequestBuilder request = MockMvcRequestBuilders.post("/token_introspection?token={token}", userToken.substring(userToken.indexOf(' ')+1));
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isUnauthorized());

		// authentication required, use basic with client id/secret. should succeed
		String auth = client.getClientId()+":"+client.getClientSecret();
        byte[] encodedAuth = org.springframework.security.crypto.codec.Base64.encode(auth.getBytes(Charset.forName("utf-8")) );
		request = MockMvcRequestBuilders.post("/token_introspection?token={token}", userToken.substring(userToken.indexOf(' ')+1))
				.header("Authorization","Basic "+ new String(encodedAuth));
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		
		// authentication required, use basic with client id and wrong secret. should fail 401
		auth = client.getClientId()+":WRONG PASSWORD";
        encodedAuth = org.springframework.security.crypto.codec.Base64.encode(auth.getBytes(Charset.forName("utf-8")) );
		request = MockMvcRequestBuilders.post("/token_introspection?token={token}", userToken.substring(userToken.indexOf(' ')+1))
				.header("Authorization","Basic "+ new String(encodedAuth));
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isUnauthorized());

		// authentication required, use user token. should fail 403
		request = MockMvcRequestBuilders.post("/token_introspection?token={token}", userToken.substring(userToken.indexOf(' ')+1))
				.header("Authorization",userToken);
		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden());

		// authentication required, use client credentials token. should succeed
		request = MockMvcRequestBuilders.post("/token_introspection?token={token}", userToken.substring(userToken.indexOf(' ')+1))
				.header("Authorization",token);
		ResultActions result = mockMvc.perform(request);
		result
		.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
		String body = result.andReturn().getResponse().getContentAsString();
		AACTokenIntrospection tokenValue = jsonMapper.readValue(body, AACTokenIntrospection.class);
		assertEquals(TEST, tokenValue.getClient_id());
		assertEquals(TEST, tokenValue.getAud());
		assertEquals(user.getUsername(), tokenValue.getUsername());
		assertEquals(user.getId().toString(), tokenValue.getAac_user_id());
	}	
}

@TestConfiguration
@EnableWebMvc
@ComponentScan(basePackages = { "it.smartcommunitylab.aac"})
class AACTokenIntrospectionControllerTestConfig {
}