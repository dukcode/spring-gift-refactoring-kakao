package gift.wish;

import gift.auth.LoginMember;
import gift.member.Member;
import gift.wish.WishService.AddWishResult;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;

    public WishController(WishService wishService) {
        this.wishService = wishService;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @LoginMember Member member,
        Pageable pageable
    ) {
        var wishes = wishService.getWishes(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @LoginMember Member member,
        @Valid @RequestBody WishRequest request
    ) {
        AddWishResult result = wishService.addWishIdempotent(member.getId(), request.productId());
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/wishes/" + result.wish().getId()))
                .body(WishResponse.from(result.wish()));
        }
        return ResponseEntity.ok(WishResponse.from(result.wish()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @LoginMember Member member,
        @PathVariable Long id
    ) {
        wishService.removeWish(member.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
