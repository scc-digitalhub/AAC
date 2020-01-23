package it.smartcommunitylab.aac.test.openid;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;

public class OpenidUtils {

    public static ClientDetailsEntity createClient(String clientId, long developerId, String grantTypes,
            String[] scopes,
            String redirectUri)
            throws Exception {
        // manually add client to repo
        ClientDetailsEntity entity = new ClientDetailsEntity();

        entity.setName(clientId);
        entity.setClientId(clientId);
        entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.name());
        entity.setAuthorizedGrantTypes(grantTypes);
        entity.setDeveloperId(developerId);
        entity.setClientSecret(UUID.randomUUID().toString());
        entity.setClientSecretMobile(UUID.randomUUID().toString());
        entity.setRedirectUri(redirectUri);
        entity.setMobileAppSchema(clientId);

        entity.setScope(String.join(",", scopes));

        ClientAppInfo info = new ClientAppInfo();
        info.setIdentityProviders(Collections.singletonMap(Config.IDP_INTERNAL, ClientAppInfo.APPROVED));
        info.setName(clientId);
        info.setDisplayName(clientId);
        info.setResourceApprovals(Collections.<String, Boolean>emptyMap());

        entity.setAdditionalInformation(info.toJson());
        return entity;
    }

    public static JWKSet fetchJWKS(TestRestTemplate restTemplate, String endpoint) throws ParseException {
        ResponseEntity<String> response = restTemplate.getForEntity(endpoint,
                String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return JWKSet.parse(response.getBody());
        } else {
            return null;
        }

    }

    public static JSONObject getTokenViaAuthCode(
            TestRestTemplate restTemplate, String endpoint,
            ClientDetails client, String sessionId,
            String[] scopes)
            throws RestClientException, UnsupportedEncodingException, ParseException, JOSEException,
            NoSuchAlgorithmException, InvalidKeySpecException {

        // request token
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, String.format("JSESSIONID=%s", sessionId));

        String clientId = client.getClientId();
        String nonce = RandomStringUtils.random(5, true, true);
        String state = RandomStringUtils.random(5, true, true);
        // redirect does not need urlEncoding because there are no parameters
        String redirectURL = client.getRegisteredRedirectUri().iterator().next();

        // manually build URI to avoid sketchy (wrong) resttemplate urlEncoding of
        // queryString
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint + "/eauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectURL)
                .queryParam("scope", String.join(" ", scopes))
                .queryParam("response_type", "code")
                .queryParam("response_mode", "query")
                .queryParam("state", state)
                .queryParam("nonce", nonce);

        URI uri = builder.build().toUri();
        System.out.println("call " + uri.toString());

        // note: TESTrestTemplate does not follow redirects
        ResponseEntity<String> response = restTemplate.exchange(uri,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        String locationURL = response.getHeaders().getFirst(HttpHeaders.LOCATION);

        // call pre-auth to fetch code
        ResponseEntity<String> response2 = restTemplate.exchange(
                locationURL,
                HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);

        // extract query parameters
        locationURL = response2.getHeaders().getFirst(HttpHeaders.LOCATION);
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(locationURL)
                .build()
                .getQueryParams();

        String code = parameters.getFirst("code");

        // exchange code for tokens
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // build basic auth for client
        String auth = clientId + ":" + client.getClientSecret();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("UTF-8")));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // post as form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectURL);
        map.add("code", code);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response3 = restTemplate.postForEntity(
                endpoint + "/oauth/token",
                entity,
                String.class);

        // parse
        return new JSONObject(response3.getBody());

    }
}
