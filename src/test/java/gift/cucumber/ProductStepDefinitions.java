package gift.cucumber;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductStepDefinitions {

    @Autowired
    private ScenarioContext scenarioContext;

    @만일("{string} 상품을 가격 {int}, 이미지 {string}, 카테고리 {string}으로 등록하면")
    public void 상품을_카테고리로_등록하면(String name, int price, String imageUrl, String categoryName) {
        Long categoryId = scenarioContext.getId(categoryName);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %d,
                            "imageUrl": "%s",
                            "categoryId": %d
                        }
                        """.formatted(name, price, imageUrl, categoryId))
                .when()
                .post("/api/products");

        scenarioContext.setResponse(response);
    }

    @만일("{string} 상품을 가격 {int}, 이미지 {string}, 존재하지 않는 카테고리로 등록하면")
    public void 상품을_존재하지_않는_카테고리로_등록하면(String name, int price, String imageUrl) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %d,
                            "imageUrl": "%s",
                            "categoryId": 999
                        }
                        """.formatted(name, price, imageUrl))
                .when()
                .post("/api/products");

        scenarioContext.setResponse(response);
    }

    @그러면("상품이 {int}개 등록되어 있다")
    public void 상품_개수를_확인한다(int expectedCount) {
        Response response = RestAssured.given()
                .when()
                .get("/api/products");

        int actualCount = response.jsonPath().getList("content").size();
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @그러면("등록된 상품 {string}의 카테고리는 {string}이다")
    public void 상품의_카테고리를_확인한다(String productName, String categoryName) {
        Long expectedCategoryId = scenarioContext.getId(categoryName);

        Response response = RestAssured.given()
                .when()
                .get("/api/products");

        Long actualCategoryId = response.jsonPath()
                .getLong("content.find { it.name == '%s' }.categoryId".formatted(productName));
        assertThat(actualCategoryId).isEqualTo(expectedCategoryId);
    }
}
