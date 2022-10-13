package it.smartcommunitylab.aac.oauth.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * OAuth 2.0 Client Credentials
 * as per RFC6749
 * 
 * https://www.rfc-editor.org/rfc/rfc6749#section-4.4
 */

@SpringBootTest
@AutoConfigureMockMvc
public class ClientCredentialsGrantTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

}
