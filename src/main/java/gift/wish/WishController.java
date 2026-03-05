package gift.wish;

import gift.auth.AuthenticationResolver;
import gift.wish.WishService.AddWishResult;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;
    private final AuthenticationResolver authenticationResolver;

    public WishController(WishService wishService, AuthenticationResolver authenticationResolver) {
        this.wishService = wishService;
        this.authenticationResolver = authenticationResolver;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var wishes = wishService.getWishes(member.getId(), pageable).map(WishResponse::from);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AddWishResult result = wishService.addWishIdempotent(member.getId(), request.productId());
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/wishes/" + result.wish().getId()))
                .body(WishResponse.from(result.wish()));
        }
        return ResponseEntity.ok(WishResponse.from(result.wish()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id
    ) {
        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            wishService.removeWish(member.getId(), id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
