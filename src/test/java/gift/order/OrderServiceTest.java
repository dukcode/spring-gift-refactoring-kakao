package gift.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import gift.category.Category;
import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.product.Product;
import gift.wish.WishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OptionService optionService;

    @Mock
    MemberService memberService;

    @Mock
    WishService wishService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    OrderService orderService;

    private Product product;
    private Option option;
    private Member member;

    @BeforeEach
    void setUp() {
        Category category = new Category("식품", "#ff0000", "http://img.com", "");
        product = new Product("테스트상품", 10000, "http://img.com", category);
        option = new Option(product, "기본옵션", 10);
        member = new Member("test@test.com", "pass");
        member.chargePoint(50000);
    }

    @Test
    void 주문_성공_이벤트_발행() {
        given(optionService.findById(1L)).willReturn(option);
        given(orderRepository.save(any())).willReturn(new Order(option, member.getId(), 2, ""));

        orderService.createOrder(member, 1L, 2, "");

        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
        verify(wishService).deleteByMemberIdAndProductId(any(), any());
    }

    @Test
    void 재고_부족_주문_실패() {
        given(optionService.findById(1L)).willReturn(option);

        assertThatThrownBy(() -> orderService.createOrder(member, 1L, 11, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("차감할 수량이 현재 재고보다 많습니다.");
    }
}
