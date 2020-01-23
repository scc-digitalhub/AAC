package it.smartcommunitylab.aac.test.openid;

import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "oauth2.jwt=true" })
@ActiveProfiles("test")
@EnableConfigurationProperties
public class ImplicitFlowTestWithJWT extends ImplicitFlowTest {

}
