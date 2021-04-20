package it.smartcommunitylab.aac.oauth.endpoint;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.aac.SystemKeys;

@Controller
public class TokenEndpoint {

    @Autowired
    private org.springframework.security.oauth2.provider.endpoint.TokenEndpoint tokenEndpoint;

    @RequestMapping(value = {
            "/oauth/token",
            "/-/{realm}/oauth/token" }, method = RequestMethod.GET)
    public ResponseEntity<OAuth2AccessToken> getAccessToken(
            @RequestParam Map<String, String> parameters,
            @PathVariable("realm") Optional<String> realmKey,
            Principal principal)
            throws HttpRequestMethodNotSupportedException {

        String realm = SystemKeys.REALM_COMMON;
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }

        // build a new map with realm parameter
        Map<String, String> params = new HashMap<>();
        params.putAll(parameters);
        params.put("realm", realm);

        return tokenEndpoint.getAccessToken(principal, params);
    }

    @RequestMapping(value = {
            "/oauth/token",
            "/-/{realm}/oauth/token" }, method = RequestMethod.POST)
    public ResponseEntity<OAuth2AccessToken> postAccessToken(
            @RequestParam Map<String, String> parameters,
            @PathVariable("realm") Optional<String> realmKey,
            Principal principal)
            throws HttpRequestMethodNotSupportedException {
        String realm = SystemKeys.REALM_COMMON;
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }

        // build a new map with realm parameter
        Map<String, String> params = new HashMap<>();
        params.putAll(parameters);
        params.put("realm", realm);

        ResponseEntity<OAuth2AccessToken> response = tokenEndpoint.postAccessToken(principal, params);

        // invalidate session now
        // this should be meaningless since we expect this endpoint to live under a
        // sessionless context
        SecurityContextHolder.getContext().setAuthentication(null);

        return response;
    }

}