package gift.cucumber;

import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.조건;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;

public class GiftStepDefinitions {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScenarioContext scenarioContext;

    @조건("상품 {string}에 옵션 {string}의 재고가 {int}개이다")
    public void 상품에_옵션이_존재한다(String productName, String optionName, int quantity) {
        Long productId = scenarioContext.getId(productName);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO options (product_id, name, quantity) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setLong(1, productId);
            ps.setString(2, optionName);
            ps.setInt(3, quantity);
            return ps;
        }, keyHolder);

        scenarioContext.storeId(optionName, keyHolder.getKey().longValue());
    }

    @만일("{string}이 옵션 {string}을 {int}개 {string} 메시지와 함께 주문하면")
    public void 옵션을_주문하면(String memberName, String optionName, int quantity, String message) {
        Long optionId = scenarioContext.getId(optionName);
        String token = scenarioContext.getToken(memberName);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                            "optionId": %d,
                            "quantity": %d,
                            "message": "%s"
                        }
                        """.formatted(optionId, quantity, message))
                .when()
                .post("/api/orders");

        scenarioContext.setResponse(response);
    }
}
