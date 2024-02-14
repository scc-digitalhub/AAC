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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import delight.graaljssandbox.GraalSandbox;
import delight.graaljssandbox.GraalSandboxes;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import delight.nashornsandbox.internal.RemoveComments;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.script.ScriptException;

public class LocalGraalExecutionService implements ScriptExecutionService {

    public static final int DEFAULT_MAX_CPU_TIME = 100;
    public static final int DEFAULT_MAX_MEMORY = 10485760;

    private int maxCpuTime;
    private int maxMemory;

    // custom jackson configuration with typeReference
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setSerializationInclusion(Include.NON_NULL);
    private final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

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
            URL res =
                com.oracle.js.parser.ScriptEnvironment.class.getClassLoader().getResource("/META-INF/truffle/language");
            // initialize the file system for the language file
            FileSystems.newFileSystem(res.toURI(), new HashMap<>());
        } catch (Throwable ignored) {
            // in case of starting without fat jar
        }

        GraalSandbox sandbox = createSandbox();
        try {
            StringWriter writer = new StringWriter();
            writer.append(CONSOLE_OVERRIDE);
            writer.append("data = ");
            mapper.writeValue(writer, input);
            writer.append(";");
            writer.append("\n");
            writer.append(function);
            writer.append(";");
            writer.append("\n");
            writer.append("result = JSON.stringify(" + name + "(data));");
            writer.append("logs = JSON.stringify(_logs);");

            String code = RemoveComments.perform(writer.toString());
            sandbox.eval(code);

            String output = (String) sandbox.get("result");
            Map<String, Serializable> result = mapper.readValue(output, typeRef);
            String logs = (String) sandbox.get("logs");

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

    @Override
    public <T> T executeFunction(String name, String function, Class<T> clazz, Serializable... inputs)
        throws InvalidDefinitionException, SystemException {
        // TODO evaluate function syntax etc

        // workaround for graal 19.2.1 and fat jars
        // https://github.com/oracle/graal/issues/1348
        try {
            URL res =
                com.oracle.js.parser.ScriptEnvironment.class.getClassLoader().getResource("/META-INF/truffle/language");
            // initialize the file system for the language file
            FileSystems.newFileSystem(res.toURI(), new HashMap<>());
        } catch (Throwable ignored) {
            // in case of starting without fat jar
        }

        GraalSandbox sandbox = createSandbox();
        try {
            StringWriter writer = new StringWriter();
            writer.append(CONSOLE_OVERRIDE);

            List<String> vars = new LinkedList<>();
            for (int i = 0; i < inputs.length; i++) {
                String v = "a" + i;
                writer.append(v).append("=");
                mapper.writeValue(writer, inputs[i]);
                writer.append(";\n");
                vars.add(v);
            }

            writer.append(function).append(";").append("\n");
            writer.append("result = JSON.stringify(" + name + "(" + String.join(",", vars) + "));");
            writer.append("logs = JSON.stringify(_logs);");

            String code = RemoveComments.perform(writer.toString());
            sandbox.eval(code);

            String output = (String) sandbox.get("result");
            T result = mapper.readValue(output, clazz);
            String logs = (String) sandbox.get("logs");

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
        sandbox.allowNoBraces(false);
        sandbox.disallowAllClasses();
        sandbox.allowPrintFunctions(false);
        return sandbox;
    }

    private static final String CONSOLE_OVERRIDE =
        "var _logs = [];\n" + //
        "if (console) {\n" + //
        "  for (let c of [\"log\", \"warn\", \"debug\", \"error\"]) {\n" + //
        "    console[c] = function () {\n" + //
        "      var line = { ...arguments };\n" + //
        "      _logs.push(line);\n" + //
        "    };\n" + //
        "  }\n" + //
        "}\n" + //
        "";
}
