package gift.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    @Test
    void 카테고리_생성_성공() {
        var request = new CategoryRequest("식품", "#ff0000", "http://img.com", "설명");
        var category = request.toEntity();
        given(categoryRepository.save(any())).willReturn(category);

        Category result = categoryService.create(request);

        assertThat(result.getName()).isEqualTo("식품");
        verify(categoryRepository).save(any());
    }

    @Test
    void 카테고리_조회_존재하지_않으면_예외() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(999L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("카테고리가 존재하지 않습니다");
    }

    @Test
    void 카테고리_삭제_호출() {
        categoryService.delete(1L);
        verify(categoryRepository).deleteById(1L);
    }
}
