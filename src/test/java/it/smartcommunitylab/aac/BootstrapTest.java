/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import java.io.IOException;
import java.io.Writer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;

@SpringJUnitWebConfig
// @SpringBootTest
// @AutoConfigureMockMvc
public class BootstrapTest {

    private final Logger logger = LoggerFactory.getLogger(BootstrapTest.class);

    @Autowired
    private ResourceLoader resourceLoader;

    private String bootstrapFile = "classpath:/bootstrap-test.yaml";

    private BootstrapConfig bootstrapConfig(String file) throws IOException {
        ObjectMapper yamlObjectMapper = yamlObjectMapper();

        // read configuration
        Resource res = resourceLoader.getResource(file);
        if (!res.exists()) {
            throw new IllegalArgumentException("error loading bootstrap from " + file);
        }

        // manually load form yaml because spring properties
        // can't bind abstract classes via jsonTypeInfo
        // also a custom factory won't work because properties are exposed as strings.
        BootstrapConfig config = yamlObjectMapper.readValue(res.getInputStream(), BootstrapConfig.class);

        return config;
    }

    @Test
    public void bootstrapTest() throws Exception {
        System.out.println("loading bootstrap from file " + String.valueOf(bootstrapFile));
        BootstrapConfig config = bootstrapConfig(bootstrapFile);

        assertNotNull(config, "config not valid");
    }

    /*
     * Object mapper
     */

    private ObjectMapper yamlObjectMapper() {
        //        YAMLFactory factory = new YAMLFactory()
        //                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
        //                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLFactory factory = yamlFactory();
        ObjectMapper yamlObjectMapper = new ObjectMapper(factory);
        yamlObjectMapper.registerModule(new JavaTimeModule());
        yamlObjectMapper.setSerializationInclusion(Include.NON_EMPTY);
        yamlObjectMapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false);
        return yamlObjectMapper;
    }

    private YAMLFactory yamlFactory() {
        class CustomYAMLFactory extends YAMLFactory {

            private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

            @Override
            protected YAMLGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
                int feats = _yamlGeneratorFeatures;
                return yamlGenerator(ctxt, _generatorFeatures, feats, _objectCodec, out, _version);
            }
        }

        return new CustomYAMLFactory()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, false)
            .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
            .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false);
    }

    private YAMLGenerator yamlGenerator(
        IOContext ctxt,
        int jsonFeatures,
        int yamlFeatures,
        ObjectCodec codec,
        Writer out,
        org.yaml.snakeyaml.DumperOptions.Version version
    ) throws IOException {
        class MyYAMLGenerator extends YAMLGenerator {

            public MyYAMLGenerator(
                IOContext ctxt,
                int jsonFeatures,
                int yamlFeatures,
                ObjectCodec codec,
                Writer out,
                org.yaml.snakeyaml.DumperOptions.Version version
            ) throws IOException {
                super(ctxt, jsonFeatures, yamlFeatures, null, codec, out, version);
            }

            @Override
            protected DumperOptions buildDumperOptions(
                int jsonFeatures,
                int yamlFeatures,
                org.yaml.snakeyaml.DumperOptions.Version version
            ) {
                DumperOptions opt = super.buildDumperOptions(jsonFeatures, yamlFeatures, version);
                // override opts
                opt.setDefaultScalarStyle(ScalarStyle.LITERAL);
                opt.setDefaultFlowStyle(FlowStyle.BLOCK);
                opt.setIndicatorIndent(2);
                opt.setIndent(4);
                opt.setPrettyFlow(true);
                opt.setCanonical(false);
                return opt;
            }
        }

        return new MyYAMLGenerator(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
    }
}
