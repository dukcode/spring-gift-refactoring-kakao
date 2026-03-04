package gift.cucumber;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionStepDefinitions {

    @Autowired
    private ScenarioContext scenarioContext;

    @만일("상품 {string}에 옵션 {string}을 수량 {int}로 추가하면")
    public void 상품에_옵션을_추가하면(String productName, String optionName, int quantity) {
        Long productId = scenarioContext.getId(productName);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "quantity": %d
                        }
                        """.formatted(optionName, quantity))
                .when()
                .post("/api/products/{productId}/options", productId);

        scenarioContext.setResponse(response);
    }

    @만일("존재하지 않는 상품에 옵션 {string}를 수량 {int}로 추가하면")
    public void 존재하지_않는_상품에_옵션을_추가하면(String optionName, int quantity) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "quantity": %d
                        }
                        """.formatted(optionName, quantity))
                .when()
                .post("/api/products/{productId}/options", 999L);

        scenarioContext.setResponse(response);
    }

    @만일("상품 {string}의 옵션 {string}을 삭제하면")
    public void 상품의_옵션을_삭제하면(String productName, String optionName) {
        Long productId = scenarioContext.getId(productName);
        Long optionId = scenarioContext.getId(optionName);

        Response response = RestAssured.given()
                .when()
                .delete("/api/products/{productId}/options/{optionId}", productId, optionId);

        scenarioContext.setResponse(response);
    }

    @그러면("상품 {string}의 옵션이 {int}개이다")
    public void 상품의_옵션_개수를_확인한다(String productName, int expectedCount) {
        Long productId = scenarioContext.getId(productName);

        Response response = RestAssured.given()
                .when()
                .get("/api/products/{productId}/options", productId);

        int actualCount = response.jsonPath().getList("$").size();
        assertThat(actualCount).isEqualTo(expectedCount);
    }
}
