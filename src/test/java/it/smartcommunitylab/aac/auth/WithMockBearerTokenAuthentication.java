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
 * Mock authentication in security context as bearer token
 * Use as annotation on methods/classes to inject a mocked authentication token
 *  
 * This class can be used with resource server/token introspector configs without 
 * mock components as long as the HTTP Authorization header is avoided in requests:
 *  - the bearer token resolver will skip processing due to missing header
 *  - the introspector won't execute, leaving no authentication in the security context
 *  - the factory will build a new security context for the current invocation
 *  - the authorization interceptor will read from the context the mocked auth
 *  
 *  By default the annotation will produce an authentication for 
 *  - sub "000"
 *  - ROLE_USER
 *  - realm:system
 *  - no scopes
 */

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = MockBearerTokenAuthenticationFactory.class)
public @interface WithMockBearerTokenAuthentication {

    @AliasFor("value")
    String subject() default "00000000-0000-0000-0000-000000000000";

    @AliasFor("subject")
    String value() default "00000000-0000-0000-0000-000000000000";

    String realm() default SystemKeys.REALM_SYSTEM;

    String[] authorities() default { Config.R_USER };

    String[] scopes() default {};

    @AliasFor(annotation = WithSecurityContext.class)
    TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;

    String token() default "mocktoken00000000-0000";

}
