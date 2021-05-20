package it.smartcommunitylab.aac.claims;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.script.ScriptException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import delight.graaljssandbox.GraalSandbox;
import delight.graaljssandbox.GraalSandboxes;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;

public class LocalGraalExecutionService implements ScriptExecutionService {

    public static final int DEFAULT_MAX_CPU_TIME = 100;
    public static final int DEFAULT_MAX_MEMORY = 10485760;

    private int maxCpuTime;
    private int maxMemory;

    // custom jackson configuration with typeReference
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    public LocalGraalExecutionService() {
        this.maxCpuTime = DEFAULT_MAX_CPU_TIME;
        this.maxMemory = DEFAULT_MAX_MEMORY;
    }

    public int getMaxCpuTime() {
        return maxCpuTime;
    }

    public void setMaxCpuTime(int maxCpuTime) {
        this.maxCpuTime = maxCpuTime;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    // TODO implement a threadPool for sandboxes to avoid bootstrap
    @Override
    public Map<String, Serializable> executeFunction(String name, String function, Map<String, Serializable> input)
            throws InvalidDefinitionException, SystemException {

        // TODO evaluate function syntax etc

        // workaround for graal 19.2.1 and fat jars
        // https://github.com/oracle/graal/issues/1348
        try {
            URL res = com.oracle.js.parser.ScriptEnvironment.class.getClassLoader()
                    .getResource("/META-INF/truffle/language");
            // initialize the file system for the language file
            FileSystems.newFileSystem(res.toURI(), new HashMap<>());
        } catch (Throwable ignored) {
            // in case of starting without fat jar
        }

        GraalSandbox sandbox = createSandbox();
        try {
            StringWriter writer = new StringWriter();
            writer.append("data = ");
            mapper.writeValue(writer, input);
            writer.append(";");
            writer.append("\n");
            writer.append(function);
            writer.append(";");
            writer.append("\n");
            writer.append("result = JSON.stringify("
                    + name
                    + "(data))");

            
            String code = writer.toString();
            sandbox.eval(code);
            String output = (String) sandbox.get("result");

            Map<String, Serializable> result = mapper.readValue(output, typeRef);
            return result;
        } catch (JsonGenerationException | JsonMappingException e) {
            throw new InvalidDefinitionException(e.getMessage());
        } catch (IOException e) {
            throw new SystemException(e.getMessage());
        } catch (ScriptCPUAbuseException | ScriptException e) {
            throw new InvalidDefinitionException(e.getMessage());
        } finally {
            sandbox.getExecutor().shutdown();
        }

    }

    private GraalSandbox createSandbox() {
        GraalSandbox sandbox;
        sandbox = GraalSandboxes.create();
        sandbox.setMaxCPUTime(maxCpuTime);
        sandbox.setMaxMemory(maxMemory);
        sandbox.setMaxPreparedStatements(30); // because preparing scripts for execution is expensive
        sandbox.setExecutor(Executors.newSingleThreadExecutor());
        return sandbox;
    }
}
