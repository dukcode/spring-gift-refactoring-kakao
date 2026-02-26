package gift.cucumber;

import gift.auth.JwtProvider;
import io.cucumber.java.Before;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.util.List;

public class CommonStepDefinitions {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ScenarioContext scenarioContext;

    @Before
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of("orders", "options", "wish", "product", "category", "member")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @조건("회원 {string}이 {int} 포인트를 가지고 있다")
    public void 회원이_포인트를_가지고_있다(String name, int point) {
        String email = name + "@test.com";
        String password = "password1234";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO member (email, password, point) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, email);
            ps.setString(2, password);
            ps.setInt(3, point);
            return ps;
        }, keyHolder);

        scenarioContext.storeId(name, keyHolder.getKey().longValue());
        scenarioContext.storeToken(name, jwtProvider.createToken(email));
    }

    @조건("카테고리 {string}이 존재한다")
    public void 카테고리가_존재한다(String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO category (name, color, image_url, description) VALUES (?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, name);
            ps.setString(2, "#000000");
            ps.setString(3, "https://example.com/image.jpg");
            ps.setString(4, name + " 카테고리");
            return ps;
        }, keyHolder);

        scenarioContext.storeId(name, keyHolder.getKey().longValue());
    }

    @조건("{string} 카테고리에 상품 {string}이 가격 {int}으로 존재한다")
    public void 카테고리에_상품이_존재한다(String categoryName, String productName, int price) {
        Long categoryId = scenarioContext.getId(categoryName);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO product (name, price, image_url, category_id) VALUES (?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, productName);
            ps.setInt(2, price);
            ps.setString(3, "https://example.com/" + productName + ".jpg");
            ps.setLong(4, categoryId);
            return ps;
        }, keyHolder);

        scenarioContext.storeId(productName, keyHolder.getKey().longValue());
    }

    @그러면("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드를_확인한다(int statusCode) {
        scenarioContext.getResponse().then().statusCode(statusCode);
    }
}
