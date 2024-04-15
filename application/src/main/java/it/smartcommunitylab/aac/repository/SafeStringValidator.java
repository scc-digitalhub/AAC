package it.smartcommunitylab.aac.repository;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.jsoup.Jsoup;
import org.springframework.util.StringUtils;

public class SafeStringValidator implements ConstraintValidator<SafeString, String> {

    private SafeString constraint;

    @Override
    public void initialize(SafeString constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (!StringUtils.hasText(s)) {
            return true;
        }

        return Jsoup.isValid(s, constraint.safelist().getValue());
    }
}
