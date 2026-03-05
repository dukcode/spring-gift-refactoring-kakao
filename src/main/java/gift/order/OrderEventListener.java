package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final KakaoMessageClient kakaoMessageClient;

    public OrderEventListener(KakaoMessageClient kakaoMessageClient) {
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        String accessToken = event.member().getKakaoAccessToken();
        if (accessToken == null) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(accessToken, event.order(), event.option().getProduct());
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: {}", e.getMessage(), e);
        }
    }
}
