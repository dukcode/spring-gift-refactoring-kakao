package gift.product;

import gift.common.NameValidator;
import java.util.List;

public class ProductNameValidator {
    private static final int MAX_LENGTH = 15;

    private ProductNameValidator() {
    }

    public static List<String> validate(String name) {
        return validate(name, false);
    }

    public static List<String> validate(String name, boolean allowKakao) {
        List<String> errors = NameValidator.validate(name, MAX_LENGTH, "상품");

        if (name != null && !name.isBlank() && !allowKakao && name.contains("카카오")) {
            errors.add("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
        }

        return errors;
    }
}
