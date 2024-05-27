/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.jackson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderSettingsMap;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.base.model.ConfigurableProviderImpl;
import it.smartcommunitylab.aac.base.provider.config.AbstractConfigurableProviderConverter;
import it.smartcommunitylab.aac.base.provider.config.DefaultConfigMapConverter;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.model.ProviderProperties;
import it.smartcommunitylab.aac.core.provider.ResolvableGenericsTypeProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableConverterFactory;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderConfig;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;

@Slf4j
public class JacksonTests {

    private static final String BOOTSTRAP_FILE = "classpath:/bootstrap-test.yaml";

    private ResourceLoader resourceLoader = new DefaultResourceLoader();
    private ObjectMapper yamlMapper = yamlObjectMapper();

    private ObjectMapper jsonMapper = new ObjectMapper();

    private AnnotationConfigApplicationContext context;

    @Test
    public void schema() throws Exception {
        IdentityProviderSettingsMap a = new IdentityProviderSettingsMap();
        JsonNode schema = a.getSchema();
        System.out.println("schema: " + schema.toString());
    }

    @Test
    public void types() {
        // IdentityProviderSettingsMap a = new IdentityProviderSettingsMap();
        // ConfigurableIdentityProvider a = new ConfigurableIdentityProvider();
        TestConfig a = new TestConfig();
        ResolvableType at = a.getResolvableType();
        ResolvableType ac = a.getConfigType();
        System.out.println("res: " + String.valueOf(at));
        System.out.println("c: " + ac.resolve().getName());
    }

    @Test
    public void generator() throws Exception {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        TestConfig tc = new TestConfig();
        IdentityProviderSettingsMap im = new IdentityProviderSettingsMap();
        Class<?> iType = im.getResolvableType().resolve();
        System.out.println(iType.getName());

        // JavaType providerType = mapper.getTypeFactory().constructSimpleType((Class<?>) TestConfig.class, null);
        // JavaType configurableType = mapper
        //     .getTypeFactory()
        //     .constructSimpleType((Class<?>) ConfigurableIdentityProvider.class, null);
        // JavaType mapType = mapper
        //     .getTypeFactory()
        //     .constructSimpleType((Class<?>) IdentityProviderSettingsMap.class, null);

        // Class<?> configType = tc.getResolvableType().resolve();
        Class<?> configurableType = cp.getResolvableType().resolve();
        Class<?> mapType = cp.getResolvableType(0).resolve();

        // AbstractConfigurableProviderConverter<
        //     TestConfig,
        //     ConfigurableIdentityProvider,
        //     IdentityProviderSettingsMap,
        //     IdentityProviderSettingsMap
        // > c = new AbstractConfigurableProviderConverter<
        //     TestConfig,
        //     ConfigurableIdentityProvider,
        //     IdentityProviderSettingsMap,
        //     IdentityProviderSettingsMap
        // >() {};

        Class<
            AbstractConfigurableProviderConverter<
                TestConfig,
                ConfigurableIdentityProvider,
                IdentityProviderSettingsMap,
                IdentityProviderSettingsMap
            >
        > subclass = (Class<
                AbstractConfigurableProviderConverter<
                    TestConfig,
                    ConfigurableIdentityProvider,
                    IdentityProviderSettingsMap,
                    IdentityProviderSettingsMap
                >
            >) new ByteBuddy()
            .subclass(
                TypeDescription.Generic.Builder.parameterizedType(
                    AbstractConfigurableProviderConverter.class,
                    TestConfig.class,
                    configurableType,
                    mapType,
                    mapType
                ).build()
            )
            .name(getClass().getPackageName() + ".TestConfigConverter")
            .make()
            .load(getClass().getClassLoader())
            .getLoaded();

        System.out.println("class " + subclass.getName());

        AbstractConfigurableProviderConverter<
            TestConfig,
            ConfigurableIdentityProvider,
            IdentityProviderSettingsMap,
            IdentityProviderSettingsMap
        > c = subclass.getDeclaredConstructor().newInstance();
        ResolvableType t0 = c.getResolvableType(0);
        ResolvableType t1 = c.getResolvableType(1);
        ResolvableType t2 = c.getResolvableType(2);
        ResolvableType t3 = c.getResolvableType(3);

        System.out.println("class " + subclass.getName());

        // TestConfig o = c.convert(cp);
        Object o = c.convert(cp);
        System.out.println("res: " + String.valueOf(o));
    }

    @Test
    public void convert1() throws Exception {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setAuthority("au1");
        cp.setRealm("realm1");
        cp.setProvider("asd");

        cp.setConfiguration(Map.of("notes", "configNotes"));
        cp.setSettings(Map.of("notes", "settingNOtes"));

        TestConfig tc = new TestConfig();
        IdentityProviderSettingsMap im = new IdentityProviderSettingsMap();
        Class<?> iType = im.getResolvableType().resolve();
        System.out.println(iType.getName());

        // JavaType providerType = mapper.getTypeFactory().constructSimpleType((Class<?>) TestConfig.class, null);
        // JavaType configurableType = mapper
        //     .getTypeFactory()
        //     .constructSimpleType((Class<?>) ConfigurableIdentityProvider.class, null);
        // JavaType mapType = mapper
        //     .getTypeFactory()
        //     .constructSimpleType((Class<?>) IdentityProviderSettingsMap.class, null);

        Class<?> configType = tc.getResolvableType().resolve();
        Class<?> configurableType = cp.getResolvableType().resolve();
        Class<?> mapType = cp.getResolvableType(0).resolve();

        // AbstractConfigurableProviderConverter<
        //     TestConfig,
        //     ConfigurableIdentityProvider,
        //     IdentityProviderSettingsMap,
        //     IdentityProviderSettingsMap
        // > c = new AbstractConfigurableProviderConverter<
        //     TestConfig,
        //     ConfigurableIdentityProvider,
        //     IdentityProviderSettingsMap,
        //     IdentityProviderSettingsMap
        // >() {};

        AbstractConfigurableProviderConverter<
            ProviderConfig<IdentityProviderSettingsMap, ConfigMap>,
            ConfigurableIdentityProvider,
            IdentityProviderSettingsMap,
            ConfigMap
        > c = ConfigurableConverterFactory.instance()
            .buildConfigurableProviderConverter(configType, configurableType, mapType, mapType);

        // Converter<ConfigurableIdentityProvider, ? extends ProviderConfig<?, ?>> c = (Converter<
        //         ConfigurableIdentityProvider,
        //         ? extends ProviderConfig<?, ?>
        //     >) new ByteBuddy()
        //     .subclass(
        //         TypeDescription.Generic.Builder.parameterizedType(
        //             AbstractConfigurableProviderConverter.class,
        //             configType,
        //             configurableType,
        //             mapType,
        //             mapType
        //         ).build()
        //     )
        //     .name(getClass().getPackageName() + ".TestConfigConverter")
        //     .make()
        //     .load(getClass().getClassLoader())
        //     .getLoaded()
        //     .getDeclaredConstructor()
        //     .newInstance();

        ResolvableType t0 = ((ResolvableGenericsTypeProvider) c).getResolvableType(0);
        ResolvableType t1 = ((ResolvableGenericsTypeProvider) c).getResolvableType(1);
        ResolvableType t2 = ((ResolvableGenericsTypeProvider) c).getResolvableType(2);
        ResolvableType t3 = ((ResolvableGenericsTypeProvider) c).getResolvableType(3);

        System.out.println("class " + c.getClass().getName());

        // TestConfig o = c.convert(cp);
        ProviderConfig<?, ?> o = c.convert(cp);
        System.out.println("res: " + String.valueOf(o));
    }

    @Test
    public void convertMap() throws Exception {
        IdentityProviderSettingsMap im = new IdentityProviderSettingsMap();
        Map<String, java.io.Serializable> map = new HashMap<>();
        map.put("notes", "NASDLOASDLA");

        Class<?> iType = im.getResolvableType().resolve();
        System.out.println(iType.getName());

        DefaultConfigMapConverter<IdentityProviderSettingsMap> c = ConfigurableConverterFactory.instance()
            .buildConfigMapConverter(iType);

        ResolvableType t0 = ((ResolvableGenericsTypeProvider) c).getResolvableType(0);

        System.out.println("class " + c.getClass().getName());
        System.out.println("t " + t0.resolve().getName());

        // TestConfig o = c.convert(cp);
        IdentityProviderSettingsMap o = c.convert(map);
        System.out.println("res: " + String.valueOf(o));

        DefaultConfigMapConverter<? extends ConfigMap> c1 = ConfigurableConverterFactory.instance()
            .buildConfigMapConverter(iType);

        ResolvableType t1 = ((ResolvableGenericsTypeProvider) c1).getResolvableType(0);

        System.out.println("class " + c1.getClass().getName());
        System.out.println("t " + t1.resolve().getName());

        // TestConfig o = c.convert(cp);
        ConfigMap o1 = c1.convert(map);
        System.out.println("res: " + String.valueOf(o1));
    }

    // @Test
    // public void infer() {
    //     ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();

    //     JavaType providerType = mapper.getTypeFactory().constructSimpleType((Class<?>) TestConfig.class, null);
    //     JavaType configurableType = mapper
    //         .getTypeFactory()
    //         .constructSimpleType((Class<?>) ConfigurableIdentityProvider.class, null);
    //     JavaType mapType = mapper
    //         .getTypeFactory()
    //         .constructSimpleType((Class<?>) IdentityProviderSettingsMap.class, null);

    //     // AbstractConfigurableProviderConverter<
    //     //     TestConfig,
    //     //     ConfigurableIdentityProvider,
    //     //     IdentityProviderSettingsMap,
    //     //     IdentityProviderSettingsMap
    //     // > c = new AbstractConfigurableProviderConverter<
    //     //     TestConfig,
    //     //     ConfigurableIdentityProvider,
    //     //     IdentityProviderSettingsMap,
    //     //     IdentityProviderSettingsMap
    //     // >() {};

    //     DefaultConfigurableProviderConverter<
    //         ProviderConfig<IdentityProviderSettingsMap, AbstractConfigMap>,
    //         ConfigurableProvider<IdentityProviderSettingsMap>,
    //         IdentityProviderSettingsMap,
    //         AbstractConfigMap
    //     > c = new DefaultConfigurableProviderConverter<>();
    //     c.setConfigurableType(configurableType);
    //     c.setProviderType(providerType);
    //     c.setSettingsType(mapType);
    //     c.setConfigType(mapType);

    //     ResolvableType t0 = c.getResolvableType(0);

    //     ResolvableType resolvableType = ResolvableType.forClass(TestConfig.class);
    //     @SuppressWarnings("unchecked")
    //     Class<C> clazz = (Class<C>) resolvableType.getSuperType().getGeneric(1).resolve();

    //     // TestConfig o = c.convert(cp);
    //     Object o = c.convert((ConfigurableProvider<IdentityProviderSettingsMap>) cp);
    //     System.out.println("res: " + String.valueOf(o));
    // }

    @Test
    public void serialize() throws Exception {
        IdentityProviderSettingsMap a = new IdentityProviderSettingsMap();
        a.setNotes("adasda");
        String value = jsonMapper.writeValueAsString(a);
        System.out.println("json: \n" + value);

        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider();
        cp.setSettings(Map.of("notes", "test"));
        value = jsonMapper.writeValueAsString(cp);
        System.out.println("json: \n" + value);
    }

    @Test
    public void serializeNoType() throws Exception {
        AccountProviderSettingsMap a = new AccountProviderSettingsMap();
        a.setRepositoryId("adasda");
        String value = jsonMapper.writeValueAsString(a);
        System.out.println("json: \n" + value);
    }

    @Test
    public void deserializeMap() throws Exception {
        String value = "{\"type\":\"identity\",\"notes\":\"adasda\",\"hookFunctions\":{}}";
        // String value =
        // "{\"type\":\"it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap\",\"notes\":\"adasda\",\"hookFunctions\":{}}";

        jsonMapper.registerSubtypes(new NamedType(IdentityProviderSettingsMap.class, "identity"));

        ConfigMap a = jsonMapper.readValue(value, AbstractSettingsMap.class);

        System.out.println("obj: \n" + a);

        String value2 = "{\"type\":\"AccountProviderSettingsMap\",\"repositoryId\":\"adasda\"}";
        a = jsonMapper.readValue(value2, AbstractSettingsMap.class);

        System.out.println("obj: \n" + a);
    }

    @Test
    public void deserializeMapScanned() throws Exception {
        String value = "{\"type\":\"identity\",\"notes\":\"adasda\",\"hookFunctions\":{}}";
        // String value =
        // "{\"type\":\"it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap\",\"notes\":\"adasda\",\"hookFunctions\":{}}";
        SubtypeModule module = new SubtypeModule(
            new AnnotationConfigApplicationContext(),
            "it.smartcommunitylab.aac.identity.provider",
            "it.smartcommunitylab.aac.identity.model"
        );
        //important: do before adding to jackson
        module.afterPropertiesSet();

        jsonMapper.registerModule(module);

        ConfigMap a = jsonMapper.readValue(value, AbstractSettingsMap.class);

        System.out.println("obj: \n" + a);

        String value2 =
            "{\"type\":\"identity\",\"enabled\":false,\"titleMap\":{},\"descriptionMap\":{},\"configuration\":{},\"settings\":{\"notes\":\"test\"}}";

        ConfigurableProvider<ConfigMap> cp = jsonMapper.readValue(value2, ConfigurableProvider.class);
        System.out.println("cp: \n" + cp);

        //resolve
        ResolvableType gt = ((ResolvableGenericsTypeProvider) cp).getResolvableType();
        System.out.println("gt: \n" + gt);
        ResolvableType mt = ((ResolvableGenericsTypeProvider) cp).getResolvableType(0);
        System.out.println("mt: \n" + mt);
    }

    @Test
    public void deserialize() throws IOException {
        Resource res = resourceLoader.getResource(BOOTSTRAP_FILE);
        log.info("dump file");
        String content = Streams.asString(res.getInputStream());
        System.out.println(content);

        ProviderProperties properties = yamlMapper.readValue(content, ProviderProperties.class);
        log.debug(String.valueOf(properties));
    }

    private void load(Class<?>... basePackageClasses) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        List<String> basePackages = Arrays.stream(basePackageClasses)
            .map(ClassUtils::getPackageName)
            .collect(Collectors.toList());
        context.registerBean(JsonMixinModule.class, () -> new JsonMixinModule(context, basePackages));
        context.refresh();
        this.context = context;
    }

    public void scan() {}

    // JsonMixinModule jsonMixinModule() {
    // 			JsonMixinModule jsonMixinModule = new JsonMixinModule();
    // 			jsonMixinModule.registerEntries(entries, context.getClassLoader());
    // 			return jsonMixinModule;
    // 		}

    public ObjectMapper yamlObjectMapper() {
        //        YAMLFactory factory = new YAMLFactory()
        //                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
        //                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLFactory factory = yamlFactory();
        ObjectMapper yamlObjectMapper = new ObjectMapper(factory);
        yamlObjectMapper.registerModule(new JavaTimeModule());
        // yamlObjectMapper.registerModule(jsonMixinModule());
        yamlObjectMapper.setSerializationInclusion(Include.NON_EMPTY);
        yamlObjectMapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false);
        return yamlObjectMapper;
    }

    //    @Bean
    public YAMLFactory yamlFactory() {
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

    class TestConfigConverter
        extends AbstractConfigurableProviderConverter<
            TestConfig,
            ConfigurableIdentityProvider,
            IdentityProviderSettingsMap,
            IdentityProviderSettingsMap
        > {}
}
