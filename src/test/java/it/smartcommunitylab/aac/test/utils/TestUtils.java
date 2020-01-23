package it.smartcommunitylab.aac.test.utils;

import java.util.List;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class TestUtils {
    /*
     * Perform 2-step login to validate session on AAC+internal auth
     */

    public static String login(TestRestTemplate restTemplate, String endpoint, String username, String password) {

        String jsid = null;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // post as form data for login
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("username", username);
        map.add("password", password);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                endpoint + "/aac/login",
                entity,
                String.class);

        // expect redirect from login to eauth
        if (response.getStatusCode().is3xxRedirection()) {
            // fetch set-cookie for session from headers
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            for (String cookie : cookies) {
                if (cookie.contains("JSESSIONID")) {
                    jsid = cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
                    break;
                }
            }

            if (jsid == null) {
                return null;
            }

            // call eauth to validate session on aac
            headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, String.format("JSESSIONID=%s", jsid));

            // use "/" as redirect
            ResponseEntity<String> response2 = restTemplate.exchange(
                    endpoint + "/aac/eauth/internal?target=%2F",
                    HttpMethod.GET, new HttpEntity<Object>(headers),
                    String.class);

            if (!response2.getStatusCode().is2xxSuccessful() && !response.getStatusCode().is3xxRedirection()) {
                // error, clear
                jsid = "";
            }

        } else {
            System.out.println("response is " + response.getStatusCodeValue() + ": " + response.getBody());

        }

        return jsid;

    }
}
