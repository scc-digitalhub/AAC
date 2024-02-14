package it.smartcommunitylab.aac.repository;

import java.util.Collection;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.jsoup.safety.Cleaner;

public class SafeStringCollectionValidator implements ConstraintValidator<SafeString, Collection<String>> {

    private SafeString constraint;

    @Override
    public void initialize(SafeString constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isValid(Collection<String> values, ConstraintValidatorContext constraintValidatorContext) {
        if (values == null || values.isEmpty()) {
            return true;
        }

        Cleaner cleaner = new Cleaner(constraint.safelist().getValue());
        return values.stream().allMatch(cleaner::isValidBodyHtml);
    }
}
