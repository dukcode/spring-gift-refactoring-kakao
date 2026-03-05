package gift.option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import gift.category.Category;
import gift.product.Product;
import gift.product.ProductService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {

    @Mock
    OptionRepository optionRepository;

    @Mock
    ProductService productService;

    @InjectMocks
    OptionService optionService;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = new Category("식품", "#ff0000", "http://img.com", "");
        product = new Product("테스트상품", 10000, "http://img.com", category);
    }

    @Test
    void 옵션_생성_성공() {
        var request = new OptionRequest("기본옵션", 10);
        given(productService.findById(1L)).willReturn(product);
        given(optionRepository.existsByProductIdAndName(1L, "기본옵션")).willReturn(false);
        given(optionRepository.save(any())).willReturn(new Option(product, "기본옵션", 10));

        Option result = optionService.create(1L, request);

        assertThat(result.getName()).isEqualTo("기본옵션");
    }

    @Test
    void 중복_옵션명_실패() {
        var request = new OptionRequest("기본옵션", 10);
        given(productService.findById(1L)).willReturn(product);
        given(optionRepository.existsByProductIdAndName(1L, "기본옵션")).willReturn(true);

        assertThatThrownBy(() -> optionService.create(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 옵션명입니다.");
    }

    @Test
    void 옵션_조회_없으면_예외() {
        given(optionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> optionService.findById(999L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("옵션이 존재하지 않습니다");
    }

    @Test
    void 옵션_1개_삭제_불가() {
        Option option = new Option(product, "기본옵션", 10);
        given(productService.findById(1L)).willReturn(product);
        given(optionRepository.findByProductId(1L)).willReturn(List.of(option));

        assertThatThrownBy(() -> optionService.delete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
    }
}
