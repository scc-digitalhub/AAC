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

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Arrays;
import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.jackson.JsonMixin;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public class SubtypeModule extends SimpleModule implements InitializingBean {

    private final ApplicationContext context;

    private final Collection<String> basePackages;

    public SubtypeModule(ApplicationContext context, String... basePackages) {
        Assert.notNull(context, "Context must not be null");
        this.context = context;
        this.basePackages = Arrays.asList(basePackages);
    }

    public SubtypeModule(ApplicationContext context, Collection<String> basePackages) {
        Assert.notNull(context, "Context must not be null");
        this.context = context;
        this.basePackages = basePackages;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (ObjectUtils.isEmpty(this.basePackages)) {
            return;
        }
        scan(this.basePackages);
    }

    public void scan(Collection<String> basePackages) throws Exception {
        JsonSubTypeComponentScanner scanner = new JsonSubTypeComponentScanner();
        scanner.setEnvironment(this.context.getEnvironment());
        scanner.setResourceLoader(this.context);
        for (String basePackage : basePackages) {
            if (StringUtils.hasText(basePackage)) {
                for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                    registerSubtype(ClassUtils.forName(candidate.getBeanClassName(), this.context.getClassLoader()));
                }
            }
        }
    }

    private void registerSubtype(Class<?> clazz) {
        JsonTypeName annotation = clazz.getAnnotation(JsonTypeName.class);
        if (annotation != null && annotation.value() != "") {
            registerSubtype(annotation.value(), clazz);
        }
    }

    public void registerSubtype(String name, Class<?> type) {
        Assert.notNull(type, "type is required");
        Assert.notNull(name, "name can not be null");
        System.out.println("register subtype for " + type.getName() + " name " + name);
        this.registerSubtypes(new NamedType(type, name));
    }

    static class JsonSubTypeComponentScanner extends ClassPathScanningCandidateComponentProvider {

        JsonSubTypeComponentScanner() {
            addIncludeFilter(new AnnotationTypeFilter(JsonTypeName.class));
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return true;
        }
    }
}
