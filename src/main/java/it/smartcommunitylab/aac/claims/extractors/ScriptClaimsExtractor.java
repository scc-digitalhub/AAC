package it.smartcommunitylab.aac.claims.extractors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.model.ClaimsExtractor;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.dto.UserProfile;
import it.smartcommunitylab.aac.model.User;

public class ScriptClaimsExtractor implements ClaimsExtractor<AbstractClaim> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String CLAIM_MAPPING_FUNCTION = "claimMapping";

    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private ScriptExecutionService executionService;
    private String claimMappingFunction = CLAIM_MAPPING_FUNCTION;
    private String userClaimsFunction;
    private String clientClaimsFunction;

    private Converter<MultiValueMap<String, Serializable>, List<AbstractClaim>> claimsParser;

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    public void setClaimsParser(Converter<MultiValueMap<String, Serializable>, List<AbstractClaim>> claimsParser) {
        Assert.notNull(claimsParser, "claim parser can not be null");
        this.claimsParser = claimsParser;
    }

    public void setUserClaimsFunction(String userClaimsFunction) {
        this.userClaimsFunction = userClaimsFunction;
    }

    public void setClientClaimsFunction(String clientClaimsFunction) {
        this.clientClaimsFunction = clientClaimsFunction;
    }

    public void setClaimMappingFunction(String claimMappingFunction) {
        Assert.hasText(claimMappingFunction, "claim mapping function can not be null or empty");
        this.claimMappingFunction = claimMappingFunction;
    }

    @Override
    public Collection<AbstractClaim> extractUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        if (executionService == null || !StringUtils.hasText(userClaimsFunction)) {
            logger.trace("execution service or code is null");
            return null;
        }

        Map<String, Serializable> exts = Collections.emptyMap();
        if (extensions != null) {
            exts = extensions;
        }
        try {
            // translate user, client and scopes to a map
            // the context is the input for the claim mapping function
            Map<String, Serializable> context = buildUserContext(user, client, scopes, exts);

            // execute script
            // TODO update to use multiValueMap
            Map<String, Serializable> customClaims = executionService.executeFunction(
                    claimMappingFunction, userClaimsFunction, context);

            // convert result to claimSet
            if (claimsParser == null) {
                throw new SystemException("claims parser undefined");
            }

            return claimsParser.convert(toMultiMap(customClaims));
        } catch (SystemException | InvalidDefinitionException e) {
            logger.error(e.getMessage());

            return null;
        }

    }

    @Override
    public Collection<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        if (executionService == null || !StringUtils.hasText(clientClaimsFunction)) {
            logger.trace("execution service or code is null");
            return null;
        }

        Map<String, Serializable> exts = Collections.emptyMap();
        if (extensions != null) {
            exts = extensions;
        }
        try {
            // translate client and scopes to a map
            // the context is the input for the claim mapping function
            Map<String, Serializable> context = buildClientContext(client, scopes, exts);

            // execute script
            Map<String, Serializable> customClaims = executionService.executeFunction(
                    claimMappingFunction, clientClaimsFunction, context);

            // convert result to claimSet
            if (claimsParser == null) {
                throw new SystemException("claims parser undefined");
            }

            return claimsParser.convert(toMultiMap(customClaims));
        } catch (SystemException | InvalidDefinitionException e) {
            logger.error(e.getMessage());

            return null;
        }
    }

    private Map<String, Serializable> buildUserContext(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        UserProfile profile = new UserProfile(user);

        // translate user, client and scopes to a map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("user", mapper.convertValue(profile, serMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));
        if (extensions != null) {
            map.put("extensions", mapper.convertValue(extensions, serMapTypeRef));
        }

        return map;
    }

    private Map<String, Serializable> buildClientContext(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // translate client and scopes to a map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("client", mapper.convertValue(client, serMapTypeRef));
        if (extensions != null) {
            map.put("extensions", mapper.convertValue(extensions, serMapTypeRef));
        }

        return map;
    }

    private MultiValueMap<String, Serializable> toMultiMap(@Nullable Map<String, Serializable> source) {
        LinkedMultiValueMap<String, Serializable> map = new LinkedMultiValueMap<>();
        if (source != null) {
            source.forEach((k, v) -> {
                map.put(k, Collections.singletonList(v));
            });
        }

        return map;
    }

}
