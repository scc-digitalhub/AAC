package it.smartcommunitylab.aac.audit.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.auth.OAuth2ClientAuthenticationToken;
import it.smartcommunitylab.aac.oauth.event.TokenGrantEvent;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.realms.service.RealmService;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuditHelperMethods {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    // PREPARE CLIENT DATA
    public static Map<String, Object> clientData(OAuth2ClientAuthenticationToken authClient, String realm){
        Map<String, Object> clientData = new LinkedHashMap<>();
        OAuth2ClientDetails clientDetails = authClient.getOAuth2ClientDetails();

        clientData.put("clientId", authClient.getClientId());
        clientData.put("realm", clientDetails != null ? clientDetails.getRealm() : realm);
        clientData.put("clientName", clientDetails != null ? clientDetails.getName() : authClient.getName());

        return clientData;
    }

    // PREPARE USER DATA
    public static Map<String, Object> userData(Object rawAuthenticationDetails, String levelRealmEvent) {
        switch (levelRealmEvent) {
            case SystemKeys.EVENTS_LEVEL_MINIMAL: {
                return extractMinimalUserData(rawAuthenticationDetails);
            }
            case SystemKeys.EVENTS_LEVEL_DETAILS: {
                Map<String, Object> userData = extractMinimalUserData(rawAuthenticationDetails);
                Map<String, Object> safeUserDetails = AuditHelperMethods.extractUserDetails(rawAuthenticationDetails);
                userData.put("details", safeUserDetails);
                return userData;
            }
            case SystemKeys.EVENTS_LEVEL_FULL: {
                return MAPPER.convertValue(rawAuthenticationDetails, new TypeReference<>() {});
            }
        }
        return Map.of();
    }

    // Extracts the minimal fields (subjectId, realm, username)
    private static Map<String, Object> extractMinimalUserData(Object rawAuthenticationDetails) {
        Map<String, Object> userData = new LinkedHashMap<>();
        Map<String, Object> authUserMap = MAPPER.convertValue(rawAuthenticationDetails, new TypeReference<>() {});

        userData.put("subjectId", authUserMap.get("subjectId"));
        userData.put("realm", authUserMap.get("realm"));
        userData.put("username", authUserMap.get("username"));

        return userData;
    }

    // PREPARE TOKEN SANITIZE
    public static String sanitizeToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isEmpty()) {
            return tokenValue;
        }

        int firstDot = tokenValue.indexOf('.');

        if (firstDot != -1) {
            int secondDot = tokenValue.indexOf('.', firstDot + 1);

            // Ensure exactly two dots for a valid JWT format
            if (secondDot != -1 && tokenValue.indexOf('.', secondDot + 1) == -1) {
                return tokenValue.substring(0, secondDot + 1) + "__SIGNATURE__";
            }
        }

        // Fallback for non-JWT formats
        return "***MASKED_TOKEN***";
    }

    // PREPARE ISSUED TOKEN
    public static List<String> issuedTokens(AACOAuth2AccessToken token, TokenGrantEvent event) {
        List<String> issuedTokens = new ArrayList<>();

        if (token == null) {
            return issuedTokens;
        }

        // Defensively check if the access token value is actually present
        if (token.getValue() != null && !token.getValue().isEmpty()) {
            issuedTokens.add("access_token");
        }

        // Check if a refresh token was issued
        if (token.getRefreshToken() != null && token.getRefreshToken().getValue() != null) {
            issuedTokens.add("refresh_token");
        }

        // NOTE: At this point token.getIdToken() is usually null.
        // The OIDCTokenEnhancer that would normally populate it is disabled in OAuth2Config.
        // The real id_token is actually built later in TokenEndpoint (via idTokenServices.createIdToken),
        // AFTER this audit event is already published.
        //
        // To keep the "issued_tokens" array accurate, we mirror the exact issuing rules of TokenEndpoint:
        // an id_token is generated only for user-centric grants (authorization_code, refresh_token,
        // implicit, and password) provided the "openid" scope is requested. Machine-to-machine grants
        // (like client_credentials) never issue an id_token and are excluded.
        //
        // We keep the defensive token.getIdToken() != null check as a fallback in case
        // the OIDCTokenEnhancer is ever re-enabled in the system configuration.
        boolean isEligibleGrantType =
            AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(event.getGrantType()) ||
            AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(event.getGrantType()) ||
            AuthorizationGrantType.IMPLICIT.getValue().equals(event.getGrantType()) ||
            AuthorizationGrantType.PASSWORD.getValue().equals(event.getGrantType());

        boolean idTokenIssued = isEligibleGrantType &&
            token.getScope() != null &&
            token.getScope().contains(Config.SCOPE_OPENID);

        if (token.getIdToken() != null || idTokenIssued) {
            issuedTokens.add("id_token");
        }

        return issuedTokens;
    }

    // PREPARE TOKEN TYPE
    public static String tokenType(AACOAuth2AccessToken token, Authentication rawAuthentication){
        // determine token type from the token itself: on refresh grants the
        // OAuth2Authentication carried by the event has no userAuthentication,
        // so we must rely on the token subject vs authorizedParty (client_id)
        String subject = token.getSubject();
        boolean isUserToken =
            rawAuthentication != null ||
                (StringUtils.hasText(subject) && !subject.equals(token.getAuthorizedParty()));
        return isUserToken ? "user" : "client";
    }

    // RESOLVE AUDIT-OAUTH2TOKEN-LEVEL CONFIGURED IN REALM
    public static String resolveOauth2EventsLevel(RealmService realmService, String realm) {
        if (realmService != null && StringUtils.hasText(realm)) {
            Realm r = realmService.findRealm(realm);
            if (
                r != null &&
                    r.getAuditConfiguration() != null &&
                    StringUtils.hasText(r.getAuditConfiguration().getOauth2EventsLevel())
            ) {
                return r.getAuditConfiguration().getOauth2EventsLevel();
            }
        }

        // DEFAULT VALUE
        return SystemKeys.EVENTS_LEVEL_NONE;
    }

    public static Map<String, Object> extractUserDetails(Object rawDetails) {
        try {
            if (rawDetails == null) {
                return null;
            }

            Map<String, Object> safeDetails = new LinkedHashMap<>();

            // Jackson converts to LinkedHashMap, preserving original JSON order
            Map<String, Object> mappedObject = MAPPER.convertValue(rawDetails, new TypeReference<>() {});

            Object identitiesObj = mappedObject.get("identities");

            if (identitiesObj instanceof List) {
                List<Map<String, Object>> rawIdentities = MAPPER.convertValue(identitiesObj, new TypeReference<>() {});
                List<Map<String, Object>> safeIdentities = new ArrayList<>();

                // Iterate over ALL identities
                for (Map<String, Object> rawIdentity : rawIdentities) {
                    if (!rawIdentity.containsKey("principal")) {
                        continue;
                    }
                    Map<String, Object> safeIdentity = new LinkedHashMap<>();

                    // Extract exclusively the "account" block if present
                    if (rawIdentity.containsKey("account")) {
                        Object accountObj = rawIdentity.get("account");
                        Map<String, Object> safeAccount = MAPPER.convertValue(accountObj, new TypeReference<>() {});
                        safeIdentity.put("account", safeAccount);
                        safeIdentities.add(safeIdentity);
                    }
                }
                safeDetails.put("identities", safeIdentities);
            }
            return safeDetails;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting details object properties: " + e.getMessage());
        }
    }
}
