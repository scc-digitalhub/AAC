package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.Map;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;

public interface ScriptExecutionService {

    public Map<String, Serializable> executeFunction(String name, String function, Map<String, Serializable> input)
            throws InvalidDefinitionException, SystemException;

}
