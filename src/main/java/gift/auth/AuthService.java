package gift.auth;

import gift.member.Member;
import gift.member.MemberService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public AuthService(MemberService memberService, JwtProvider jwtProvider) {
        this.memberService = memberService;
        this.jwtProvider = jwtProvider;
    }

    public String registerAndCreateToken(String email, String password) {
        Member member = memberService.register(email, password);
        return jwtProvider.createToken(member.getEmail());
    }

    public String loginAndCreateToken(String email, String password) {
        Member member = memberService.login(email, password);
        return jwtProvider.createToken(member.getEmail());
    }
}
