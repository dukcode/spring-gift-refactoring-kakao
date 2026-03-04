package gift.cucumber;

import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

public class WishStepDefinitions {

    @Autowired
    private ScenarioContext scenarioContext;

    @만일("{string}이 상품 {string}을 위시리스트에 추가하면")
    public void 상품을_위시리스트에_추가하면(String memberName, String productName) {
        Long productId = scenarioContext.getId(productName);
        String token = scenarioContext.getToken(memberName);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                            "productId": %d
                        }
                        """.formatted(productId))
                .when()
                .post("/api/wishes");

        scenarioContext.setResponse(response);

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            Long wishId = response.jsonPath().getLong("id");
            scenarioContext.storeId("마지막위시", wishId);
        }
    }

    @만일("{string}이 마지막 위시를 삭제하면")
    public void 마지막_위시를_삭제하면(String memberName) {
        Long wishId = scenarioContext.getId("마지막위시");
        String token = scenarioContext.getToken(memberName);

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/wishes/{id}", wishId);

        scenarioContext.setResponse(response);
    }

    @만일("{string}이 존재하지 않는 상품을 위시리스트에 추가하면")
    public void 존재하지_않는_상품을_위시리스트에_추가하면(String memberName) {
        String token = scenarioContext.getToken(memberName);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                            "productId": 999
                        }
                        """)
                .when()
                .post("/api/wishes");

        scenarioContext.setResponse(response);
    }

    @만일("잘못된 인증으로 위시리스트를 조회하면")
    public void 잘못된_인증으로_위시리스트를_조회하면() {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer invalid-token")
                .when()
                .get("/api/wishes");

        scenarioContext.setResponse(response);
    }
}
