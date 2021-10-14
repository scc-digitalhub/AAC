package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;

public class ScriptClaimExtractor {
    public static final String CLAIM_MAPPING_FUNCTION = "claimMapping";

    private final ScriptExecutionService executionService;
    private String functionName;
    private String functionCode;

    public ScriptClaimExtractor(ScriptExecutionService executionService) {
        this(executionService, CLAIM_MAPPING_FUNCTION);
    }

    public ScriptClaimExtractor(ScriptExecutionService executionService, String claimMappingFunctionName) {
        Assert.notNull(executionService, "a script execution service is required");
        Assert.hasText(claimMappingFunctionName, "function name can not be null or empty");
        this.executionService = executionService;
        functionName = claimMappingFunctionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public Map<String, Serializable> execute(Map<String, Serializable> input)
            throws SystemException, InvalidDefinitionException {
        if (!StringUtils.hasText(functionCode)) {
            return Collections.emptyMap();
        }

        // execute script
        Map<String, Serializable> claimsMap = executionService.executeFunction(functionName,
                functionCode,
                input);

        return claimsMap;

    }

}
