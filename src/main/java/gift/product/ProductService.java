package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    public Product create(ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + request.categoryId()));
        return productRepository.save(request.toEntity(category));
    }

    @Transactional
    public Product update(Long id, ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Product product = findById(id);
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + request.categoryId()));
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return product;
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
