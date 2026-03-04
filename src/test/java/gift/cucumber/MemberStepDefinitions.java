package gift.cucumber;

import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class MemberStepDefinitions {

    @Autowired
    private ScenarioContext scenarioContext;

    @만일("이메일 {string}, 비밀번호 {string}로 회원가입하면")
    public void 회원가입하면(String email, String password) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                        """.formatted(email, password))
                .when()
                .post("/api/members/register");

        scenarioContext.setResponse(response);
    }

    @만일("이메일 {string}, 비밀번호 {string}로 로그인하면")
    public void 로그인하면(String email, String password) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                        """.formatted(email, password))
                .when()
                .post("/api/members/login");

        scenarioContext.setResponse(response);
    }

    @그러면("응답에 토큰이 포함되어 있다")
    public void 응답에_토큰이_포함되어_있다() {
        String token = scenarioContext.getResponse().jsonPath().getString("token");
        assertThat(token).isNotNull().isNotBlank();
    }
}
