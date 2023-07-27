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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSecurityTag {
    /**
     * This name must correspond to a declared SecurityRequirement.
     *
     * @return String name
     */
    String name() default "";

    /**
     * If the security scheme is of type "oauth2" or "openIdConnect", then the value
     * is a list of scope names required for the execution. For other security
     * scheme types, the array must be empty.
     *
     * @return String array of scopes
     */
    @AliasFor("value")
    String[] scopes() default {};

    @AliasFor("scopes")
    String[] value() default {};
}
