/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.oauth.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.users.model.User;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of the {@link OAuthFlowExtensions} with the Web hook
 * functionality. The call is accompanied with the JWT token of the requesting
 * client with respect to the authorized user scope claims.
 *
 * TODO add call signature with sha256payload+date+key(clientSecret)
 *
 * @author raman
 *
 */
public class WebhookOAuthFlowExtensions implements OAuthFlowExtensions {

    private static final Logger logger = LoggerFactory.getLogger(WebhookOAuthFlowExtensions.class);

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef =
        new TypeReference<HashMap<String, Serializable>>() {};
    private final TypeReference<HashMap<String, String>> stringMapTypeRef =
        new TypeReference<HashMap<String, String>>() {};

    @Value("${hook.timeout:10000}")
    private int timeout;

    private RestTemplate restTemplate;

    public WebhookOAuthFlowExtensions() {
        init();
    }

    public void init() {
        RequestConfig config = RequestConfig
            .custom()
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
            client
        );
        restTemplate = new RestTemplate(clientHttpRequestFactory);
    }

    @Override
    public Map<String, String> onBeforeUserApproval(
        Map<String, String> requestParameters,
        User user,
        OAuth2ClientDetails client
    ) throws FlowExecutionException {
        if (client.getHookWebUrls() == null) {
            return null;
        }

        String webhook = client.getHookWebUrls().get(OAuthFlowExtensions.BEFORE_USER_APPROVAL);

        if (!StringUtils.hasText(webhook)) {
            return null;
        }

        // convert to map
        Map<String, Serializable> map = new HashMap<>();
        map.put("request", mapper.convertValue(requestParameters, stringMapTypeRef));
        map.put("user", mapper.convertValue(user, serMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        try {
            URL url = new URL(webhook);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            // TODO add signature header
            headers.set("Authorization", buildBasicAuth(client.getClientId(), client.getClientSecret()));
            HttpEntity<Map<String, Serializable>> entity = new HttpEntity<>(map, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.POST,
                entity,
                String.class
            );

            logger.debug("Hook response code: " + response.getStatusCodeValue());
            logger.trace("Hook result: " + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new FlowExecutionException("invalid response from webhook");
            }

            Map<String, String> result = mapper.convertValue(response.getBody(), stringMapTypeRef);
            return result;
        } catch (MalformedURLException e) {
            throw new FlowExecutionException("Invalid hook URL: " + webhook);
        } catch (Exception e) {
            throw new FlowExecutionException("Hook invocation failure: " + e.getMessage());
        }
    }

    @Override
    public Optional<Boolean> onAfterUserApproval(Collection<String> scopes, User user, OAuth2ClientDetails client)
        throws FlowExecutionException {
        if (client.getHookWebUrls() == null) {
            return Optional.empty();
        }

        String webhook = client.getHookWebUrls().get(OAuthFlowExtensions.AFTER_USER_APPROVAL);

        if (!StringUtils.hasText(webhook)) {
            return Optional.empty();
        }

        // convert to map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("user", mapper.convertValue(user, serMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        try {
            URL url = new URL(webhook);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            // TODO add signature header
            headers.set("Authorization", buildBasicAuth(client.getClientId(), client.getClientSecret()));
            HttpEntity<Map<String, Serializable>> entity = new HttpEntity<>(map, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.POST,
                entity,
                String.class
            );

            logger.debug("Hook response code: " + response.getStatusCodeValue());
            logger.trace("Hook result: " + response.getBody());

            HttpStatus statusCode = response.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
                // check if response body, otherwise we consider 200 as "approved"
                String body = response.getBody();
                if (StringUtils.hasText(body)) {
                    ApprovalResult result = mapper.convertValue(body, ApprovalResult.class);
                    return Optional.ofNullable(result.approved);
                } else {
                    return Optional.of(true);
                }
            } else if (statusCode.is4xxClientError()) {
                // we consider this as "denied"
                return Optional.of(false);
            } else {
                throw new FlowExecutionException("invalid response from webhook: " + statusCode.toString());
            }
        } catch (MalformedURLException e) {
            throw new FlowExecutionException("Invalid hook URL: " + webhook);
        } catch (Exception e) {
            throw new FlowExecutionException("Hook invocation failure: " + e.getMessage());
        }
    }

    @Override
    public Map<String, String> onBeforeTokenGrant(Map<String, String> requestParameters, OAuth2ClientDetails client)
        throws FlowExecutionException {
        if (client.getHookWebUrls() == null) {
            return null;
        }

        String webhook = client.getHookWebUrls().get(OAuthFlowExtensions.BEFORE_TOKEN_GRANT);

        if (!StringUtils.hasText(webhook)) {
            return null;
        }

        // convert to map
        Map<String, Serializable> map = new HashMap<>();
        map.put("request", mapper.convertValue(requestParameters, stringMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));

        try {
            URL url = new URL(webhook);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            // TODO add signature header
            headers.set("Authorization", buildBasicAuth(client.getClientId(), client.getClientSecret()));
            HttpEntity<Map<String, Serializable>> entity = new HttpEntity<>(map, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.POST,
                entity,
                String.class
            );

            logger.debug("Hook response code: " + response.getStatusCodeValue());
            logger.trace("Hook result: " + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new FlowExecutionException("invalid response from webhook");
            }

            Map<String, String> result = mapper.convertValue(response.getBody(), stringMapTypeRef);
            return result;
        } catch (MalformedURLException e) {
            throw new FlowExecutionException("Invalid hook URL: " + webhook);
        } catch (Exception e) {
            throw new FlowExecutionException("Hook invocation failure: " + e.getMessage());
        }
    }

    @Override
    public void onAfterTokenGrant(OAuth2AccessToken accessToken, OAuth2ClientDetails client)
        throws FlowExecutionException {
        if (client.getHookWebUrls() != null) {
            String webhook = client.getHookWebUrls().get(OAuthFlowExtensions.AFTER_TOKEN_GRANT);

            if (StringUtils.hasText(webhook)) {
                try {
                    URL url = new URL(webhook);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                    // TODO add signature header
                    headers.set("Authorization", buildBasicAuth(client.getClientId(), client.getClientSecret()));
                    HttpEntity<OAuth2AccessToken> entity = new HttpEntity<>(accessToken, headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        url.toString(),
                        HttpMethod.POST,
                        entity,
                        String.class
                    );

                    logger.debug("Hook response code: " + response.getStatusCodeValue());
                    logger.trace("Hook result: " + response.getBody());

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new FlowExecutionException("invalid response from webhook");
                    }
                } catch (MalformedURLException e) {
                    throw new FlowExecutionException("Invalid hook URL: " + webhook);
                } catch (Exception e) {
                    throw new FlowExecutionException("Hook invocation failure: " + e.getMessage());
                }
            }
        }
    }

    public class ApprovalResult {

        public Boolean approved;
    }

    private String buildBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);

        return authHeader;
    }
}
