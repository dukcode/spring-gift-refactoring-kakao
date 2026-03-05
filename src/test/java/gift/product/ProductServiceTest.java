package gift.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import gift.category.Category;
import gift.category.CategoryService;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryService categoryService;

    @InjectMocks
    ProductService productService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("식품", "#ff0000", "http://img.com", "");
    }

    @Test
    void 상품_생성_성공() {
        var request = new ProductRequest("테스트상품", 10000, "http://img.com", 1L);
        given(categoryService.findById(1L)).willReturn(category);
        given(productRepository.save(any())).willReturn(request.toEntity(category));

        Product result = productService.create(request);

        assertThat(result.getName()).isEqualTo("테스트상품");
    }

    @Test
    void 카카오_포함_상품명_생성_실패() {
        var request = new ProductRequest("카카오상품", 10000, "http://img.com", 1L);

        assertThatThrownBy(() -> productService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("카카오");
    }

    @Test
    void 상품_조회_존재하지_않으면_예외() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(999L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("상품이 존재하지 않습니다");
    }

    @Test
    void 어드민_카카오_포함_상품명_허용() {
        var request = new ProductRequest("카카오상품", 10000, "http://img.com", 1L);
        given(categoryService.findById(1L)).willReturn(category);
        given(productRepository.save(any())).willReturn(request.toEntity(category));

        Product result = productService.createForAdmin(request);

        assertThat(result.getName()).isEqualTo("카카오상품");
    }
}
