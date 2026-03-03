package gift.wish;

import gift.product.ProductRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Page<Wish> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    public Optional<Wish> findByMemberAndProduct(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId);
    }

    public Wish addWish(Long memberId, Long productId) {
        var product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
        return wishRepository.save(new Wish(memberId, product));
    }

    public void removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다. id=" + wishId));
        if (!wish.getMemberId().equals(memberId)) {
            throw new IllegalStateException("다른 사용자의 위시를 삭제할 수 없습니다.");
        }
        wishRepository.delete(wish);
    }
}
