package gift.order;

import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.wish.WishService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final WishService wishService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
        OrderRepository orderRepository,
        OptionService optionService,
        MemberService memberService,
        WishService wishService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.optionService = optionService;
        this.memberService = memberService;
        this.wishService = wishService;
        this.eventPublisher = eventPublisher;
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

        eventPublisher.publishEvent(new OrderCompletedEvent(saved, member, option));
        return saved;
    }
}
