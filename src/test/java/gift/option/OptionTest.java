package gift.option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OptionTest {

    private Option option;

    @BeforeEach
    void setUp() {
        Category category = new Category("식품", "#ff0000", "http://img.com", "");
        Product product = new Product("테스트상품", 10000, "http://img.com", category);
        option = new Option(product, "기본옵션", 10);
    }

    @Test
    void 재고_차감_성공() {
        option.subtractQuantity(3);
        assertThat(option.getQuantity()).isEqualTo(7);
    }

    @Test
    void 재고_전량_차감_성공() {
        option.subtractQuantity(10);
        assertThat(option.getQuantity()).isEqualTo(0);
    }

    @Test
    void 재고_초과_차감_실패() {
        assertThatThrownBy(() -> option.subtractQuantity(11))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("차감할 수량이 현재 재고보다 많습니다.");
    }

    @Test
    void 총_가격_계산() {
        assertThat(option.calculateTotalPrice(3)).isEqualTo(30000);
    }
}
