package it.smartcommunitylab.aac.test.jwt;

import java.text.ParseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import it.smartcommunitylab.aac.jose.JWKSetKeyStore;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableConfigurationProperties
public class JWTTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JWKSetKeyStore jwtKeyStore;

    @Autowired
    private JWTSigningAndValidationService jwtService;

    @Before
    public void init() {
    }

    @After
    public void cleanup() {
    }

    @Test
    public void testDefaultSigningKey() {
        logger.debug("testDefaultSigningKey");
        String defaultSignKeyId = jwtService.getDefaultSignerKeyId();
        String defaultSignAlgo = jwtService.getDefaultSigningAlgorithm().getName();

        Assert.assertNotNull(defaultSignKeyId);
        Assert.assertNotNull(defaultSignAlgo);

        JWK key = jwtKeyStore.getJwkSet().getKeyByKeyId(defaultSignKeyId);
        Assert.assertNotNull(key);

        // key should include private
        Assert.assertTrue(key.isPrivate());

    }

    @Test
    public void testJWKSurl() throws ParseException {
        logger.debug("testJWKSurl");
        // load jwks from url
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/aac/jwk",
                String.class);
        Assert.assertTrue((response.getStatusCode().is2xxSuccessful()));

        // parse keySet and assert not empty
        JWKSet keySet = JWKSet.parse(response.getBody());
        Assert.assertFalse(keySet.getKeys().isEmpty());

        // assert default key in keySet
        String defaultSignKeyId = jwtService.getDefaultSignerKeyId();
        JWK key = keySet.getKeyByKeyId(defaultSignKeyId);
        Assert.assertNotNull(key);

        // key should NOT include private
        Assert.assertFalse(key.isPrivate());
    }

}
