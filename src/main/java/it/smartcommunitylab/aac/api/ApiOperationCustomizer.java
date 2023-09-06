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

package it.smartcommunitylab.aac.api;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

public class ApiOperationCustomizer implements OperationCustomizer {

    private final String name;

    public ApiOperationCustomizer(String name) {
        Assert.hasText(name, "security requirement name can not be null or blank");
        this.name = name;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiSecurityTag securityTag = handlerMethod.getBeanType().getAnnotation(ApiSecurityTag.class);
        if (securityTag != null) {
            String sname = StringUtils.hasText(securityTag.name()) ? securityTag.name() : name;

            // read from scopes AND from value as fallback since AliasFor is available only
            // via AssertionUtils (from Spring)
            String[] scopes = (securityTag.scopes() != null && securityTag.scopes().length > 0)
                ? securityTag.scopes()
                : securityTag.value();

            // translate to requirement
            operation.addSecurityItem(new SecurityRequirement().addList(sname, Arrays.asList(scopes)));
        }

        return operation;
    }
}
