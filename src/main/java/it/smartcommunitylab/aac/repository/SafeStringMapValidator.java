package it.smartcommunitylab.aac.repository;

import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.jsoup.safety.Cleaner;

public class SafeStringMapValidator implements ConstraintValidator<SafeString, Map<?, String>> {

    private SafeString constraint;

    @Override
    public void initialize(SafeString constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isValid(Map<?, String> map, ConstraintValidatorContext constraintValidatorContext) {
        if (map == null || map.isEmpty()) {
            return true;
        }

        Cleaner cleaner = new Cleaner(constraint.safelist().getValue());
        boolean validValues = map.values().stream().allMatch(cleaner::isValidBodyHtml);
        boolean validKeys = map
            .keySet()
            .stream()
            .filter(String.class::isInstance)
            .map(o -> (String) o)
            .allMatch(cleaner::isValidBodyHtml);

        return validValues && validKeys;
    }
}
