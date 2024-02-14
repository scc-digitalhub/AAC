package it.smartcommunitylab.aac.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import org.jsoup.safety.Safelist;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
    validatedBy = { SafeStringValidator.class, SafeStringCollectionValidator.class, SafeStringMapValidator.class }
)
public @interface SafeString {
    SafeList safelist() default SafeList.NONE;

    String message() default "unsafe according to safelist:{safelist}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public enum SafeList {
        NONE(SafeLists.NONE),
        SIMPLE_TEXT(SafeLists.SIMPLE_TEXT),
        BASIC(SafeLists.BASIC),
        BASIC_WITH_IMAGES(SafeLists.BASIC_WITH_IMAGES),
        RELAXED(SafeLists.RELAXED),
        RELAXED_WITH_IMAGES(SafeLists.RELAXED_WITH_IMAGES);

        private final Safelist value;

        private SafeList(Safelist value) {
            this.value = value;
        }

        public Safelist getValue() {
            return value;
        }
    }

    public static class SafeLists {

        public static final Safelist NONE = Safelist.none();

        public static final Safelist SIMPLE_TEXT = Safelist.simpleText();

        public static final Safelist BASIC = Safelist
            .basic()
            .addTags("nav", "button", "hr")
            .addProtocols("a", "href", "#")
            .addAttributes(":all", "class")
            .addAttributes(":all", "style")
            .addAttributes(":all", "role");

        public static final Safelist BASIC_WITH_IMAGES = BASIC
            .addTags("img")
            .addAttributes("img", "align", "alt", "height", "src", "title", "width")
            .addProtocols("img", "src", "http", "https");

        public static final Safelist RELAXED = Safelist
            .relaxed()
            .removeTags("img")
            .addTags("nav", "button", "hr")
            .addProtocols("a", "href", "#")
            .addAttributes(":all", "class")
            .addAttributes(":all", "style")
            .addAttributes(":all", "role");

        public static final Safelist RELAXED_WITH_IMAGES = RELAXED
            .addTags("img")
            .addAttributes("img", "align", "alt", "height", "src", "title", "width")
            .addProtocols("img", "src", "http", "https");

        private SafeLists() {}
    }
}
