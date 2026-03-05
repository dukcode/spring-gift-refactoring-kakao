package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
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

    @Transactional
    public Product create(ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Category category = categoryService.findById(request.categoryId());
        return productRepository.save(request.toEntity(category));
    }

    @Transactional
    public Product update(Long id, ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Product product = findById(id);
        Category category = categoryService.findById(request.categoryId());
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return product;
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
