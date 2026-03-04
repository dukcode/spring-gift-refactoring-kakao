package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {

    private Response response;
    private final Map<String, Long> ids = new HashMap<>();
    private final Map<String, String> tokens = new HashMap<>();

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public void storeId(String name, Long id) {
        ids.put(name, id);
    }

    public Long getId(String name) {
        return ids.get(name);
    }

    public void storeToken(String name, String token) {
        tokens.put(name, token);
    }

    public String getToken(String name) {
        return tokens.get(name);
    }
}
