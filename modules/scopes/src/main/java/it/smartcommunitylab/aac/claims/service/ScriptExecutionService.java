package it.smartcommunitylab.aac.claims.service;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import java.io.Serializable;
import java.util.Map;

public interface ScriptExecutionService {
    public Map<String, Serializable> executeFunction(String name, String function, Map<String, Serializable> input)
        throws InvalidDefinitionException, SystemException;
}
