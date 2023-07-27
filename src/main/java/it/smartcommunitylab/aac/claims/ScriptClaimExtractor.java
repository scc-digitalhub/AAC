/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.claims;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
        Map<String, Serializable> claimsMap = executionService.executeFunction(functionName, functionCode, input);

        return claimsMap;
    }
}
