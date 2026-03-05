package gift.wish;

import gift.product.ProductRepository;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public AddWishResult addWishIdempotent(Long memberId, Long productId) {
        var existing = wishRepository.findByMemberIdAndProductId(memberId, productId);
        if (existing.isPresent()) {
            return new AddWishResult(existing.get(), false);
        }
        var product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
        var saved = wishRepository.save(new Wish(memberId, product));
        return new AddWishResult(saved, true);
    }

    public record AddWishResult(Wish wish, boolean created) {
    }

    @Transactional
    public void removeWish(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다. id=" + wishId));
        if (!wish.belongsTo(memberId)) {
            throw new IllegalStateException("다른 사용자의 위시를 삭제할 수 없습니다.");
        }
        wishRepository.delete(wish);
    }
}
