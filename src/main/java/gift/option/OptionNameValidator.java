package gift.option;

import gift.common.NameValidator;
import java.util.List;

/*
 * Validates option names against the following rules:
 * - Must not be null or blank
 * - Maximum length of 50 characters (including spaces)
 * - Only Korean, English, digits, spaces, and selected special characters are allowed: ( ) [ ] + - & / _
 */
public class OptionNameValidator {
    private static final int MAX_LENGTH = 50;

    private OptionNameValidator() {
    }

    public static List<String> validate(String name) {
        return NameValidator.validate(name, MAX_LENGTH, "옵션");
    }
}
