package it.smartcommunitylab.aac.repository;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.util.StringUtils;

public class SafeStringValidator implements ConstraintValidator<SafeString, String> {

    private SafeString safestring;

    @Override
    public void initialize(SafeString safestring) {
        this.safestring = safestring;
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (!StringUtils.hasText(s)) {
            return true;
        }
        return switch (safestring.safelist()) {
            case NONE -> Jsoup.isValid(s, Safelist.none());
            case BASIC -> Jsoup.isValid(s, Safelist.basic());
            case RELAXED -> Jsoup.isValid(s, Safelist.relaxed());
            case SIMPLE_TEXT -> Jsoup.isValid(s, Safelist.simpleText());
            case BASIC_WITH_IMAGES -> Jsoup.isValid(s, Safelist.basicWithImages());
            default -> false;
        };
    }
}
