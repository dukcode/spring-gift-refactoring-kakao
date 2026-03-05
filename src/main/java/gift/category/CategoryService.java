package gift.category;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Category create(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        category.update(request.name(), request.color(), request.imageUrl(), request.description());
        return category;
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
