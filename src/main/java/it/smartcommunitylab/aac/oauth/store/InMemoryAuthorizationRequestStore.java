package it.smartcommunitylab.aac.oauth.store;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

public class InMemoryAuthorizationRequestStore implements AuthorizationRequestStore, Serializable {

    private final Map<String, AuthorizationRequest> requests;

    public InMemoryAuthorizationRequestStore() {
        this.requests = new ConcurrentHashMap<>();
    }

    @Override
    public AuthorizationRequest find(String key) {
        Assert.hasText(key, "key can not be null or empty");
        return requests.get(key);
    }

    @Override
    public Collection<AuthorizationRequest> findAll() {
        return Collections.unmodifiableCollection(requests.values());
    }

    @Override
    public String store(AuthorizationRequest request) {
        String key = extractKey(request);
        requests.put(key, request);

        return key;
    }

    @Override
    public void store(AuthorizationRequest request, String key) {
        requests.put(key, request);
    }

    @Override
    public void remove(String key) {
        requests.remove(key);
    }

    private String extractKey(AuthorizationRequest request) {
        // use a sorted map as source to ensure consistency
        Map<String, String> values = new TreeMap<String, String>();

        values.put("client_id", request.getClientId());
        values.put(
            "response_type",
            StringUtils.collectionToCommaDelimitedString(new TreeSet<String>(request.getResponseTypes()))
        );
        values.put("scope", StringUtils.collectionToCommaDelimitedString(new TreeSet<String>(request.getScope())));
        values.put("redirect_uri", request.getRedirectUri());
        values.put("state", request.getState());
        values.put(
            "resource",
            StringUtils.collectionToCommaDelimitedString(new TreeSet<String>(request.getResourceIds()))
        );

        // build key and reduce to md5hash
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
        }

        try {
            return DigestUtils.md5DigestAsHex(sb.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("error building key");
        }
    }
}
