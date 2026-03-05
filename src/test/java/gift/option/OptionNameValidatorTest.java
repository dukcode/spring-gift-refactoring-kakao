package gift.option;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OptionNameValidatorTest {

    @Test
    void 정상_옵션명_성공() {
        List<String> errors = OptionNameValidator.validate("기본옵션");
        assertThat(errors).isEmpty();
    }

    @Test
    void 최대_길이_초과_실패() {
        List<String> errors = OptionNameValidator.validate("a".repeat(51));
        assertThat(errors).anyMatch(e -> e.contains("최대 50자"));
    }

    @Test
    void 빈_이름_실패() {
        List<String> errors = OptionNameValidator.validate("");
        assertThat(errors).isNotEmpty();
    }

    @Test
    void 허용되지_않는_특수문자_실패() {
        List<String> errors = OptionNameValidator.validate("옵션#1");
        assertThat(errors).anyMatch(e -> e.contains("허용되지 않는 특수 문자"));
    }
}
