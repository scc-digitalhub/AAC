package it.smartcommunitylab.aac.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { SafeStringValidator.class })
public @interface SafeString {
    SafeList safelist() default SafeList.NONE;

    String message() default "invalid string: value might be unsafe";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    enum SafeList {
        NONE,
        SIMPLE_TEXT,
        BASIC,
        BASIC_WITH_IMAGES,
        RELAXED,
    }
}
