package gift.order;

import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.wish.WishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final KakaoMessageClient kakaoMessageClient;
    private final WishService wishService;

    public OrderService(
        OrderRepository orderRepository,
        OptionService optionService,
        MemberService memberService,
        KakaoMessageClient kakaoMessageClient,
        WishService wishService
    ) {
        this.orderRepository = orderRepository;
        this.optionService = optionService;
        this.memberService = memberService;
        this.kakaoMessageClient = kakaoMessageClient;
        this.wishService = wishService;
    }

    public Page<Order> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order createOrder(Member member, Long optionId, int quantity, String message) {
        Option option = optionService.findById(optionId);

        option.subtractQuantity(quantity);

        int price = option.calculateTotalPrice(quantity);
        memberService.deductPoint(member, price);

        Order saved = orderRepository.save(new Order(option, member.getId(), quantity, message));

        wishService.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

        sendKakaoMessageIfPossible(member, saved, option);
        return saved;
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            var product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: {}", e.getMessage(), e);
        }
    }
}
