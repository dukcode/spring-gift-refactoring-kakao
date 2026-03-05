package gift.wish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import gift.category.Category;
import gift.common.ForbiddenAccessException;
import gift.product.Product;
import gift.product.ProductService;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    WishRepository wishRepository;

    @Mock
    ProductService productService;

    @InjectMocks
    WishService wishService;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = new Category("식품", "#ff0000", "http://img.com", "");
        product = new Product("테스트상품", 10000, "http://img.com", category);
    }

    @Test
    void 위시_신규_추가_성공() {
        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.empty());
        given(productService.findById(1L)).willReturn(product);
        Wish wish = new Wish(1L, product);
        given(wishRepository.save(any())).willReturn(wish);

        var result = wishService.addWishIdempotent(1L, 1L);

        assertThat(result.created()).isTrue();
    }

    @Test
    void 위시_중복_추가_기존_반환() {
        Wish existing = new Wish(1L, product);
        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.of(existing));

        var result = wishService.addWishIdempotent(1L, 1L);

        assertThat(result.created()).isFalse();
    }

    @Test
    void 다른_사용자_위시_삭제_실패() {
        Wish wish = new Wish(2L, product);
        given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

        assertThatThrownBy(() -> wishService.removeWish(1L, 1L))
            .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void 존재하지_않는_위시_삭제_실패() {
        given(wishRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishService.removeWish(1L, 999L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 위시_삭제_성공() {
        Wish wish = new Wish(1L, product);
        given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

        wishService.removeWish(1L, 1L);

        verify(wishRepository).delete(wish);
    }
}
