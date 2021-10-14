package it.smartcommunitylab.aac.oauth.request;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;

public class TokenRequest extends org.springframework.security.oauth2.provider.TokenRequest {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    /*
     * Audience indicates consumers of this token, i.e. resource servers
     */
    private Set<String> audience = new HashSet<>();

    /*
     * ResourceIds indicates which resources are involved at resource server: it is
     * used to restrict access to a set of resources *from the resource server point
     * of view*.
     */

    private Set<String> resourceIds = new HashSet<>();

    /*
     * Extensions map for additional params
     */
    protected Map<String, Serializable> extensions = new HashMap<>();

    public TokenRequest(
            Map<String, String> requestParameters,
            String clientId, String grantType,
            Collection<String> scope,
            Collection<String> resourceIds, Collection<String> audience) {
        super(requestParameters, clientId, scope, grantType);

        if (resourceIds != null) {
            this.resourceIds = new HashSet<>(resourceIds);
        }

        if (audience != null) {
            this.audience = new HashSet<>(audience);
        }

        extensions = new HashMap<>();
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Set<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(Set<String> audience) {
        this.audience = audience;
    }

    public Map<String, Serializable> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Serializable> extensions) {
        this.extensions = extensions;
    }

    @Override
    public OAuth2Request createOAuth2Request(ClientDetails client) {
        // get request parameters modifiable
        HashMap<String, String> map = new HashMap<String, String>(getRequestParameters());
        // Remove confidential fields if present to prevent leaks,
        // these are not meant to be stored but only used during processing
        map.remove("password");
        map.remove("client_secret");
        map.remove("code");

        // add grant type
        map.put(OAuth2Utils.GRANT_TYPE, getGrantType());

        // add audience details
        if (getAudience() != null) {
            map.put("audience", StringUtils.collectionToCommaDelimitedString(getAudience()));
        }
        if (getResourceIds() != null) {
            map.put("resource_ids", StringUtils.collectionToCommaDelimitedString(getResourceIds()));
        }

        return new OAuth2Request(map, client.getClientId(),
                client.getAuthorities(), true,
                getScope(), getResourceIds(), null, null, null);

    }

}
