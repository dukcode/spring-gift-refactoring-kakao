package gift.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProductNameValidatorTest {

    @Test
    void 카카오_포함_API_호출시_실패() {
        List<String> errors = ProductNameValidator.validate("카카오상품", false);
        assertThat(errors).anyMatch(e -> e.contains("카카오"));
    }

    @Test
    void 카카오_포함_어드민_허용() {
        List<String> errors = ProductNameValidator.validate("카카오상품", true);
        assertThat(errors).isEmpty();
    }

    @Test
    void 길이_초과_AND_카카오_둘다_에러() {
        List<String> errors = ProductNameValidator.validate("카카오긴이름상품인데열여섯자이상", false);
        assertThat(errors).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void 정상_상품명_성공() {
        List<String> errors = ProductNameValidator.validate("정상상품명");
        assertThat(errors).isEmpty();
    }
}
