package it.smartcommunitylab.aac.attributes.provider;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.mapper.ExactAttributesMapper;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.oauth.flow.FlowExecutionException;

public class WebhookAttributeProvider extends AbstractProvider<UserAttributes>
        implements AttributeProvider<WebhookAttributeProviderConfigMap, WebhookAttributeProviderConfig> {
    private static final Logger logger = LoggerFactory.getLogger(WebhookAttributeProvider.class);

    public static final String ATTRIBUTE_MAPPING_FUNCTION = "attributeMapping";
    private static final int DEFAULT_TIMEOUT = 5000;

    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    // services
    private final AttributeService attributeService;
    private final AttributeStore attributeStore;

    private final WebhookAttributeProviderConfig providerConfig;

    private RestTemplate restTemplate;

    public WebhookAttributeProvider(
            String providerId,
            AttributeService attributeService, AttributeStore attributeStore,
            WebhookAttributeProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBHOOK, providerId, realm);
        Assert.notNull(config, "provider config is mandatory");
        Assert.notNull(attributeService, "attribute service is mandatory");
        Assert.notNull(attributeStore, "attribute store is mandatory");

        this.attributeService = attributeService;
        this.attributeStore = attributeStore;

        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.providerConfig = config;

        // validate url
        String url = providerConfig.getConfigMap().getUrl();
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("empty url");
        }

        try {
            URL u = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid or malformed url");
        }

        // validate attribute sets, if empty nothing to do
        if (providerConfig.getAttributeSets().isEmpty()) {
            throw new IllegalArgumentException("no attribute sets enabled");
        }

        // build client
        int timeout = config.getConfigMap().getTimeout() != null ? config.getConfigMap().getTimeout() : DEFAULT_TIMEOUT;
        RequestConfig rconfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        CloseableHttpClient client = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(rconfig)
                .build();
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
                client);
        restTemplate = new RestTemplate(clientHttpRequestFactory);

    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public String getName() {
        return providerConfig.getName();
    }

    @Override
    public String getDescription() {
        return providerConfig.getDescription();
    }

    @Override
    public WebhookAttributeProviderConfig getConfig() {
        return providerConfig;
    }

    @Override
    public ConfigurableAttributeProvider getConfigurable() {
        return providerConfig.getConfigurable();
    }

    @Override
    public Collection<UserAttributes> convertPrincipalAttributes(UserAuthenticatedPrincipal principal,
            String subjectId) {

        if (providerConfig.getAttributeSets().isEmpty()) {
            return Collections.emptyList();
        }

        WebhookAttributeProviderConfigMap configMap = providerConfig.getConfigMap();

        List<UserAttributes> result = new ArrayList<>();
        Map<String, Serializable> principalAttributes = new HashMap<>();
        // get all attributes from principal
        Map<String, String> attributes = principal.getAttributes().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

        // TODO handle all attributes not only strings.
        principalAttributes.putAll(attributes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

        // we use also name from principal
        String name = principal.getName();
        principalAttributes.put("name", name);

        // add auth info
        principalAttributes.put("authority", principal.getAuthority());
        principalAttributes.put("provider", principal.getProvider());
        principalAttributes.put("realm", principal.getRealm());

        Map<String, ExactAttributesMapper> mappers = new HashMap<>();

        // fetch attribute sets
        for (String setId : providerConfig.getAttributeSets()) {
            try {
                AttributeSet as = attributeService.getAttributeSet(setId);

                // build exact mapper
                mappers.put(as.getIdentifier(), new ExactAttributesMapper(as));

            } catch (NoSuchAttributeSetException | RuntimeException e) {

            }
        }

        // execute call to webhook and parse result
        try {
            String webhook = configMap.getUrl();

            URL url = new URL(webhook);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            // TODO add signature header
            if (StringUtils.hasText(configMap.getAuthentication())) {
                // pass authorization header
                // TODO support multiple authentication methods
                headers.set("Authorization", configMap.getAuthentication());
            }

            HttpEntity<Map<String, Serializable>> entity = new HttpEntity<>(principalAttributes, headers);

            ResponseEntity<String> response = restTemplate.exchange(url.toString(), HttpMethod.POST, entity,
                    String.class);

            logger.debug("Hook response code: " + response.getStatusCodeValue());
            logger.trace("Hook result: " + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new FlowExecutionException("invalid response from webhook");
            }

            Map<String, Serializable> customAttributes = mapper.readValue(response.getBody(), serMapTypeRef);

            if (customAttributes != null) {
                // we'll let each mapper parse the result set if present
                for (String id : customAttributes.keySet()) {
                    if (mappers.containsKey(id)) {
                        ExactAttributesMapper mapper = mappers.get(id);
                        AttributeSet set = mapper.mapAttributes((Map<String, Serializable>) customAttributes.get(id));
                        if (set.getAttributes() != null && !set.getAttributes().isEmpty()) {
                            // build result
                            result.add(new DefaultUserAttributesImpl(
                                    getAuthority(), getProvider(), getRealm(), subjectId,
                                    set));
                        }
                    }
                }

            }
        } catch (MalformedURLException e) {
            throw new FlowExecutionException("Invalid hook URL " + String.valueOf(configMap.getUrl()));
        } catch (Exception e) {
            throw new FlowExecutionException("Hook invocation failure: " + e.getMessage());
        }

        // store attributes as flat map from all sets
        Set<Entry<String, Serializable>> storeAttributes = new HashSet<>();
        for (UserAttributes ua : result) {
            for (Attribute a : ua.getAttributes()) {
                // TODO handle repeatable attributes by enum
                String key = ua.getIdentifier() + "|" + a.getKey();
                Entry<String, Serializable> es = new AbstractMap.SimpleEntry<>(key, a.getValue());
                storeAttributes.add(es);
            }
        }

        attributeStore.setAttributes(subjectId, storeAttributes);

        return result;
    }

    @Override
    public Collection<UserAttributes> getUserAttributes(String subjectId) {
        // fetch from store
        Map<String, Serializable> attributes = attributeStore.findAttributes(subjectId);
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserAttributes> result = new ArrayList<>();

        // build sets from stored values
        for (String setId : providerConfig.getAttributeSets()) {
            try {
                AttributeSet as = attributeService.getAttributeSet(setId);
                String prefix = setId + "|";
                // TODO handle repeatable attributes by enum
                Map<String, Serializable> principalAttributes = attributes.entrySet().stream()
                        .filter(e -> e.getKey().startsWith(prefix))
                        .collect(Collectors.toMap(e -> e.getKey().substring(prefix.length()), e -> e.getValue()));

                // use exact mapper
                ExactAttributesMapper mapper = new ExactAttributesMapper(as);
                AttributeSet set = mapper.mapAttributes(principalAttributes);
                if (set.getAttributes() != null && !set.getAttributes().isEmpty()) {
                    // build result
                    result.add(new DefaultUserAttributesImpl(
                            getAuthority(), getProvider(), getRealm(), subjectId,
                            set));
                }
            } catch (NoSuchAttributeSetException | RuntimeException e) {
            }
        }

        return result;
    }

    @Override
    public void deleteUserAttributes(String subjectId) {
        // cleanup from store
        attributeStore.deleteAttributes(subjectId);
    }

}
