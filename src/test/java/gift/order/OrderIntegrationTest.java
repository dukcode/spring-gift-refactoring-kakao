package gift.order;

import gift.auth.JwtProvider;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(
    scripts = {"/sql/common-cleanup.sql", "/sql/order/setup.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = "/sql/common-cleanup.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class OrderIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void 정상_주문_성공() {
        Option option = optionRepository.findAll().get(0);
        Member member = memberRepository.findByEmail("rich@test.com").orElseThrow();
        String token = jwtProvider.createToken("rich@test.com");
        int initialPoint = member.getPoint();
        int initialQuantity = option.getQuantity();
        int orderQuantity = 3;
        int price = option.calculateTotalPrice(orderQuantity);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {"optionId": %d, "quantity": %d, "message": "맛있게 드세요"}
                """.formatted(option.getId(), orderQuantity))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);

        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(initialQuantity - orderQuantity);

        Member updatedMember = memberRepository.findByEmail("rich@test.com").orElseThrow();
        assertThat(updatedMember.getPoint()).isEqualTo(initialPoint - price);
    }

    @Test
    void 재고_부족_시_주문_실패하고_재고와_포인트는_유지된다() {
        Option option = optionRepository.findAll().get(0);
        Member member = memberRepository.findByEmail("rich@test.com").orElseThrow();
        String token = jwtProvider.createToken("rich@test.com");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {"optionId": %d, "quantity": 11, "message": "많이 주세요"}
                """.formatted(option.getId()))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(anyOf(is(400), is(500)));

        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(option.getQuantity());

        Member updatedMember = memberRepository.findByEmail("rich@test.com").orElseThrow();
        assertThat(updatedMember.getPoint()).isEqualTo(member.getPoint());
    }

    @Test
    void 포인트_부족_시_주문_실패하고_재고와_포인트는_유지된다() {
        Option option = optionRepository.findAll().get(0);
        Member member = memberRepository.findByEmail("poor@test.com").orElseThrow();
        String token = jwtProvider.createToken("poor@test.com");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {"optionId": %d, "quantity": 1, "message": "선물"}
                """.formatted(option.getId()))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(anyOf(is(400), is(500)));

        // @Transactional 추가 후 재고도 롤백되어 유지되어야 한다 (현재는 버그: 재고가 차감됨)
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(option.getQuantity());

        Member updatedMember = memberRepository.findByEmail("poor@test.com").orElseThrow();
        assertThat(updatedMember.getPoint()).isEqualTo(member.getPoint());
    }

    @Test
    void 위시있는_상품_주문_후_위시가_삭제된다() {
        Option option = optionRepository.findAll().get(0);
        Member member = memberRepository.findByEmail("rich@test.com").orElseThrow();
        String token = jwtProvider.createToken("rich@test.com");
        Long productId = option.getProduct().getId();

        jdbcTemplate.update(
            "INSERT INTO wish (member_id, product_id) VALUES (?, ?)",
            member.getId(), productId
        );
        assertThat(wishRepository.findByMemberIdAndProductId(member.getId(), productId)).isPresent();

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {"optionId": %d, "quantity": 1, "message": "주문"}
                """.formatted(option.getId()))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);

        assertThat(wishRepository.findByMemberIdAndProductId(member.getId(), productId)).isEmpty();
    }

    @Test
    void 위시없는_상품_주문은_정상_처리된다() {
        Option option = optionRepository.findAll().get(0);
        Member member = memberRepository.findByEmail("rich@test.com").orElseThrow();
        String token = jwtProvider.createToken("rich@test.com");
        Long productId = option.getProduct().getId();

        assertThat(wishRepository.findByMemberIdAndProductId(member.getId(), productId)).isEmpty();

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {"optionId": %d, "quantity": 1, "message": "주문"}
                """.formatted(option.getId()))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);
    }
}
