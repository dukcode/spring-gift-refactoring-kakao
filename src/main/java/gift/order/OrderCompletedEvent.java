package gift.order;

import gift.member.Member;
import gift.option.Option;

public record OrderCompletedEvent(Order order, Member member, Option option) {
}
