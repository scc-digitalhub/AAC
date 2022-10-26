package it.smartcommunitylab.aac.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;

/*
 * Mock authentication in security context as user auth
 * Use as annotation on methods/classes to inject a mocked authentication token
 *  
 * This class can be used with user auth protected configs without 
 * mock components:
 *  By default the annotation will produce an authentication for 
 *  - username: test
 *  - ROLE_USER
 *  - realm:system
 *  - no scopes
 */

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = MockUserAuthenticationFactory.class)
public @interface WithMockUserAuthentication {

    @AliasFor("value")
    String username() default "test";

    @AliasFor("username")
    String value() default "test";

    String realm() default SystemKeys.REALM_SYSTEM;

    String[] authorities() default { Config.R_USER };

    String password() default "";

    @AliasFor(annotation = WithSecurityContext.class)
    TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

}
