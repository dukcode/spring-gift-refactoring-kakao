package gift.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NameValidatorTest {

    @Test
    void null_이름_실패() {
        List<String> errors = NameValidator.validate(null, 15, "상품");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("필수");
    }

    @Test
    void 빈_이름_실패() {
        List<String> errors = NameValidator.validate("  ", 15, "상품");
        assertThat(errors).hasSize(1);
    }

    @Test
    void 길이_초과_실패() {
        List<String> errors = NameValidator.validate("열여섯자이상인이름이다아아아아!", 15, "상품");
        assertThat(errors).anyMatch(e -> e.contains("최대 15자"));
    }

    @Test
    void 허용되지_않는_특수문자_실패() {
        List<String> errors = NameValidator.validate("상품@이름", 15, "상품");
        assertThat(errors).anyMatch(e -> e.contains("허용되지 않는 특수 문자"));
    }

    @Test
    void 정상_이름_성공() {
        List<String> errors = NameValidator.validate("정상상품", 15, "상품");
        assertThat(errors).isEmpty();
    }

    @Test
    void 허용_특수문자_포함_성공() {
        List<String> errors = NameValidator.validate("상품 (A+B)", 15, "상품");
        assertThat(errors).isEmpty();
    }
}
