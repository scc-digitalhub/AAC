package it.smartcommunitylab.aac.api.controller;

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
