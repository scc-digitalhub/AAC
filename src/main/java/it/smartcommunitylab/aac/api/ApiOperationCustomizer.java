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
