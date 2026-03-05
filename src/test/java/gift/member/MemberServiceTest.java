package gift.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    MemberService memberService;

    @Test
    void 회원가입_성공() {
        given(memberRepository.existsByEmail("new@test.com")).willReturn(false);
        given(memberRepository.save(any())).willReturn(new Member("new@test.com", "pass"));

        Member result = memberService.register("new@test.com", "pass");

        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void 중복_이메일_회원가입_실패() {
        given(memberRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.register("dup@test.com", "pass"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 사용 중인 이메일입니다.");
    }

    @Test
    void 로그인_성공() {
        Member member = new Member("test@test.com", "secret");
        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));

        Member result = memberService.login("test@test.com", "secret");

        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void 잘못된_비밀번호_로그인_실패() {
        Member member = new Member("test@test.com", "secret");
        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.login("test@test.com", "wrong"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void 포인트_차감() {
        Member member = new Member("test@test.com", "pass");
        member.chargePoint(1000);
        given(memberRepository.save(any())).willReturn(member);

        memberService.deductPoint(member, 300);

        assertThat(member.getPoint()).isEqualTo(700);
        verify(memberRepository).save(member);
    }
}
