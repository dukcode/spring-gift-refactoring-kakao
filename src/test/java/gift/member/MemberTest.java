package gift.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void 포인트_충전_성공() {
        Member member = new Member("test@test.com", "pass");
        member.chargePoint(1000);
        assertThat(member.getPoint()).isEqualTo(1000);
    }

    @Test
    void 포인트_충전_0이하_실패() {
        Member member = new Member("test@test.com", "pass");
        assertThatThrownBy(() -> member.chargePoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("충전 금액은 1 이상이어야 합니다.");
    }

    @Test
    void 포인트_차감_성공() {
        Member member = new Member("test@test.com", "pass");
        member.chargePoint(1000);
        member.deductPoint(300);
        assertThat(member.getPoint()).isEqualTo(700);
    }

    @Test
    void 포인트_차감_잔액_부족_실패() {
        Member member = new Member("test@test.com", "pass");
        member.chargePoint(100);
        assertThatThrownBy(() -> member.deductPoint(200))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("포인트가 부족합니다.");
    }

    @Test
    void 비밀번호_일치_확인() {
        Member member = new Member("test@test.com", "secret");
        assertThat(member.matchesPassword("secret")).isTrue();
        assertThat(member.matchesPassword("wrong")).isFalse();
    }
}
