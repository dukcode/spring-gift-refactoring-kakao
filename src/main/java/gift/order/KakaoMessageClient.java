package gift.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.product.Product;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KakaoMessageClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public void sendToMe(String accessToken, Order order, Product product) {
        var templateObject = buildTemplate(order, product);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("template_object", templateObject);

        restClient.post()
            .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(params)
            .retrieve()
            .toBodilessEntity();
    }

    private String buildTemplate(Order order, Product product) {
        var totalPrice = String.format("%,d", order.getOption().calculateTotalPrice(order.getQuantity()));
        var messageText = buildMessageText(order, product, totalPrice);

        try {
            return objectMapper.writeValueAsString(Map.of(
                "object_type", "text",
                "text", messageText,
                "link", Map.of(),
                "button_title", "선물 확인하기"
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카카오 메시지 템플릿 생성 실패", e);
        }
    }

    private String buildMessageText(Order order, Product product, String totalPrice) {
        String messageBody = "🎁 선물이 도착했어요!\n\n"
            + product.getName() + " (" + order.getOption().getName() + ")\n"
            + "수량: " + order.getQuantity() + "개\n"
            + "금액: " + totalPrice + "원";

        if (order.getMessage() != null && !order.getMessage().isBlank()) {
            messageBody += "\n\n💌 " + order.getMessage();
        }
        return messageBody;
    }
}
